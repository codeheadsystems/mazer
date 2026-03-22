package com.codeheadsystems.mazer.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.codeheadsystems.mazer.input.InputState;
import com.codeheadsystems.mazer.maze.MazeGenerator;
import com.codeheadsystems.mazer.maze.MazeGrid;
import com.codeheadsystems.mazer.render.PlayerModelFactory;
import com.codeheadsystems.mazer.world.Bullet;
import com.codeheadsystems.mazer.world.Player;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Host-side networking. Runs a KryoNet Server, manages lobby state,
 * coordinates game start, and runs the authoritative gameplay simulation.
 */
public class HostServer {

    private static final int MAX_PLAYERS = 8;
    private static final float CELL_SIZE = 4.0f;
    private static final float TICK_RATE = 20f;
    private static final float TICK_DELTA = 1f / TICK_RATE;
    private static final long TICK_INTERVAL_MS = (long) (1000f / TICK_RATE);

    private final NetworkManager networkManager;
    private Server server;
    private String hostIp;
    private int nextPlayerId = 1; // 0 is reserved for host

    // Lobby state
    private final Map<Integer, Protocol.PlayerInfo> playerInfoMap = new HashMap<>();
    private final Map<Integer, Integer> connectionToPlayerId = new HashMap<>();

    // Gameplay state
    private MazeGrid maze;
    private final Map<Integer, Player> gamePlayers = new ConcurrentHashMap<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final Map<Integer, Protocol.PlayerInput> bufferedInputs = new ConcurrentHashMap<>();
    private final Map<Integer, Float> fireCooldowns = new HashMap<>();
    private final Map<Integer, Integer> previousScores = new HashMap<>();
    private ScheduledExecutorService tickExecutor;
    private long tickCount = 0;
    private boolean gameplayActive = false;

    public HostServer(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /**
     * Starts the server. Returns the host's LAN IP.
     */
    public String start(String hostName, int hostShapeIndex) {
        server = new Server(16384, 8192);
        Protocol.register(server.getKryo());

        // Add host player to lobby
        Protocol.PlayerInfo hostInfo = new Protocol.PlayerInfo();
        hostInfo.id = 0;
        hostInfo.name = hostName;
        hostInfo.shapeIndex = hostShapeIndex;
        hostInfo.ready = false;
        playerInfoMap.put(0, hostInfo);

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                handleMessage(connection, object);
            }

            @Override
            public void disconnected(Connection connection) {
                handleDisconnect(connection);
            }
        });

        try {
            server.bind(Protocol.TCP_PORT, Protocol.UDP_PORT);
            server.start();
        } catch (IOException e) {
            Gdx.app.error("HostServer", "Failed to start server", e);
            return null;
        }

        hostIp = detectLanIp();
        return hostIp;
    }

    public String getConnectString() {
        return "mazer://" + hostIp + ":" + Protocol.TCP_PORT;
    }

    public void toggleHostReady() {
        Protocol.PlayerInfo hostInfo = playerInfoMap.get(0);
        if (hostInfo != null) {
            hostInfo.ready = !hostInfo.ready;
            broadcastLobbyUpdate();
        }
    }

    /**
     * Starts the game if all players are ready. Generates spawn positions
     * and sends StartGame to all clients.
     */
    public void startGame(int mazeWidth, int mazeHeight) {
        long seed = System.currentTimeMillis();
        maze = MazeGenerator.generate(mazeWidth, mazeHeight, CELL_SIZE, seed);

        List<Protocol.PlayerInfo> players = new ArrayList<>(playerInfoMap.values());
        Random random = new Random(seed + 42);

        float[] spawnX = new float[players.size()];
        float[] spawnZ = new float[players.size()];
        float[] spawnAngle = new float[players.size()];

        // Pick random distinct cells for spawns
        List<int[]> usedCells = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            int cx, cy;
            boolean unique;
            do {
                cx = random.nextInt(mazeWidth);
                cy = random.nextInt(mazeHeight);
                unique = true;
                for (int[] used : usedCells) {
                    if (used[0] == cx && used[1] == cy) {
                        unique = false;
                        break;
                    }
                }
            } while (!unique);
            usedCells.add(new int[]{cx, cy});

            Vector2 pos = maze.cellToWorld(cx, cy);
            spawnX[i] = pos.x;
            spawnZ[i] = pos.y;
            spawnAngle[i] = random.nextFloat() * MathUtils.PI2;
        }

        Protocol.StartGame msg = new Protocol.StartGame();
        msg.mazeSeed = seed;
        msg.mazeWidth = mazeWidth;
        msg.mazeHeight = mazeHeight;
        msg.spawnX = spawnX;
        msg.spawnZ = spawnZ;
        msg.spawnAngle = spawnAngle;

        // Initialize gameplay state before sending StartGame
        initializeGameplay(players, spawnX, spawnZ, spawnAngle);

        server.sendToAllTCP(msg);

        // Also fire locally for the host
        networkManager.fireGameStart(msg);

        // Start the simulation loop
        startGameplayLoop();
    }

    /**
     * Accepts input from the local host player (player ID 0) without going through the network.
     */
    public void handleLocalPlayerInput(Protocol.PlayerInput input) {
        bufferedInputs.put(0, input);
    }

    public void stop() {
        stopGameplayLoop();
        if (server != null) {
            server.stop();
            server.close();
        }
    }

    public List<Protocol.PlayerInfo> getPlayerInfos() {
        return new ArrayList<>(playerInfoMap.values());
    }

    // --- Gameplay simulation ---

    private void initializeGameplay(List<Protocol.PlayerInfo> players, float[] spawnX, float[] spawnZ, float[] spawnAngle) {
        gamePlayers.clear();
        bullets.clear();
        bufferedInputs.clear();
        fireCooldowns.clear();
        previousScores.clear();
        tickCount = 0;

        for (int i = 0; i < players.size(); i++) {
            Protocol.PlayerInfo info = players.get(i);
            PlayerModelFactory.Shape shape = PlayerModelFactory.Shape.fromIndex(info.shapeIndex);
            Player player = new Player(info.id, spawnX[i], spawnZ[i], spawnAngle[i], shape);
            gamePlayers.put(info.id, player);
            fireCooldowns.put(info.id, 0f);
            previousScores.put(info.id, player.getScore());
        }
    }

    private void startGameplayLoop() {
        gameplayActive = true;
        tickExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "host-tick");
            t.setDaemon(true);
            return t;
        });
        tickExecutor.scheduleAtFixedRate(this::tick, TICK_INTERVAL_MS, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopGameplayLoop() {
        gameplayActive = false;
        if (tickExecutor != null) {
            tickExecutor.shutdownNow();
            tickExecutor = null;
        }
    }

    private void tick() {
        if (!gameplayActive) return;

        try {
            tickCount++;

            // Apply buffered inputs and update each player
            for (Player player : gamePlayers.values()) {
                if (!player.isAlive()) continue;

                InputState input = toInputState(bufferedInputs.get(player.getId()));
                player.update(TICK_DELTA, input, maze);

                // Handle firing
                float cooldown = fireCooldowns.getOrDefault(player.getId(), 0f);
                cooldown = Math.max(0, cooldown - TICK_DELTA);
                if (input.fire && cooldown <= 0 && player.isAlive()) {
                    bullets.add(new Bullet(
                            player.getX(), player.getZ(),
                            player.getAngle(), player.getId()));
                    cooldown = Bullet.FIRE_COOLDOWN;
                }
                fireCooldowns.put(player.getId(), cooldown);
            }

            // Update bullets and check collisions
            updateBullets();

            // Detect score changes and send hit/eliminated messages
            detectHits();

            // Check for game over
            checkGameOver();

            // Build and broadcast snapshot
            broadcastSnapshot();
        } catch (Exception e) {
            Gdx.app.error("HostServer", "Error in tick", e);
        }
    }

    private InputState toInputState(Protocol.PlayerInput input) {
        InputState state = new InputState();
        if (input != null) {
            state.moveForward = input.forward;
            state.turnAmount = input.turnAmount;
            state.fire = input.fire;
        }
        return state;
    }

    private void updateBullets() {
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            if (!bullet.update(TICK_DELTA, maze)) {
                bulletIter.remove();
                continue;
            }

            // Check bullet-player collision
            for (Player player : gamePlayers.values()) {
                if (!player.isAlive()) continue;
                if (player.getId() == bullet.getOwnerPlayerId()) continue;

                float dx = bullet.getX() - player.getX();
                float dz = bullet.getZ() - player.getZ();
                float dist = dx * dx + dz * dz;
                float hitRadius = Bullet.RADIUS + Player.COLLISION_RADIUS * 0.5f;

                if (dist < hitRadius * hitRadius) {
                    player.hit();
                    bullet.kill();
                    break;
                }
            }

            if (!bullet.isAlive()) {
                bulletIter.remove();
            }
        }
    }

    private void detectHits() {
        for (Player player : gamePlayers.values()) {
            int prevScore = previousScores.getOrDefault(player.getId(), player.getScore());
            int currentScore = player.getScore();

            if (currentScore < prevScore) {
                // Player was hit — find the shooter (bullet owner from most recent hit)
                // Since we process bullets above, the shooter is the owner of the bullet that hit
                Protocol.PlayerHit hitMsg = new Protocol.PlayerHit();
                hitMsg.hitPlayerId = player.getId();
                hitMsg.shooterPlayerId = -1; // We don't track shooter per-hit; use -1
                hitMsg.newScore = currentScore;

                server.sendToAllTCP(hitMsg);
                Gdx.app.postRunnable(() -> networkManager.firePlayerHit(hitMsg));

                if (!player.isAlive()) {
                    Protocol.PlayerEliminated elimMsg = new Protocol.PlayerEliminated();
                    elimMsg.playerId = player.getId();
                    server.sendToAllTCP(elimMsg);
                    Gdx.app.postRunnable(() -> networkManager.firePlayerEliminated(elimMsg));
                }
            }

            previousScores.put(player.getId(), currentScore);
        }
    }

    private void checkGameOver() {
        List<Player> alive = new ArrayList<>();
        for (Player player : gamePlayers.values()) {
            if (player.isAlive()) {
                alive.add(player);
            }
        }

        // Game over when only one (or zero) players remain alive, and we started with more than 1
        if (alive.size() <= 1 && gamePlayers.size() > 1) {
            gameplayActive = false;

            Protocol.GameOver msg = new Protocol.GameOver();
            msg.winnerPlayerId = alive.isEmpty() ? -1 : alive.get(0).getId();
            server.sendToAllTCP(msg);
            Gdx.app.postRunnable(() -> networkManager.fireGameOver(msg));

            stopGameplayLoop();
        }
    }

    private void broadcastSnapshot() {
        Protocol.GameSnapshot snapshot = new Protocol.GameSnapshot();
        snapshot.tick = tickCount;

        // Build player snapshots
        List<Protocol.PlayerSnapshot> playerSnapshots = new ArrayList<>();
        for (Player player : gamePlayers.values()) {
            Protocol.PlayerSnapshot ps = new Protocol.PlayerSnapshot();
            ps.id = player.getId();
            ps.x = player.getX();
            ps.z = player.getZ();
            ps.angle = player.getAngle();
            ps.score = player.getScore();
            ps.alive = player.isAlive();
            playerSnapshots.add(ps);
        }
        snapshot.players = playerSnapshots.toArray(new Protocol.PlayerSnapshot[0]);

        // Build bullet snapshots
        List<Protocol.BulletSnapshot> bulletSnapshots = new ArrayList<>();
        for (Bullet bullet : bullets) {
            if (!bullet.isAlive()) continue;
            Protocol.BulletSnapshot bs = new Protocol.BulletSnapshot();
            bs.x = bullet.getX();
            bs.z = bullet.getZ();
            bs.dirX = bullet.getDirX();
            bs.dirZ = bullet.getDirZ();
            bs.ownerId = bullet.getOwnerPlayerId();
            bulletSnapshots.add(bs);
        }
        snapshot.bullets = bulletSnapshots.toArray(new Protocol.BulletSnapshot[0]);

        server.sendToAllUDP(snapshot);
        Gdx.app.postRunnable(() -> networkManager.fireGameSnapshot(snapshot));
    }

    // --- Lobby message handling ---

    private void handleMessage(Connection connection, Object object) {
        if (object instanceof Protocol.JoinRequest req) {
            handleJoinRequest(connection, req);
        } else if (object instanceof Protocol.ReadyToggle) {
            handleReadyToggle(connection);
        } else if (object instanceof Protocol.PlayerInput input) {
            handlePlayerInput(connection, input);
        }
    }

    private void handlePlayerInput(Connection connection, Protocol.PlayerInput input) {
        Integer playerId = connectionToPlayerId.get(connection.getID());
        if (playerId != null) {
            bufferedInputs.put(playerId, input);
        }
    }

    private void handleJoinRequest(Connection connection, Protocol.JoinRequest req) {
        if (playerInfoMap.size() >= MAX_PLAYERS) {
            Protocol.JoinResponse resp = new Protocol.JoinResponse();
            resp.accepted = false;
            resp.reason = "Server is full";
            connection.sendTCP(resp);
            return;
        }

        int playerId = nextPlayerId++;
        connectionToPlayerId.put(connection.getID(), playerId);

        Protocol.PlayerInfo info = new Protocol.PlayerInfo();
        info.id = playerId;
        info.name = req.playerName;
        info.shapeIndex = req.shapeIndex;
        info.ready = false;
        playerInfoMap.put(playerId, info);

        Protocol.JoinResponse resp = new Protocol.JoinResponse();
        resp.playerId = playerId;
        resp.accepted = true;
        connection.sendTCP(resp);

        broadcastLobbyUpdate();
    }

    private void handleReadyToggle(Connection connection) {
        Integer playerId = connectionToPlayerId.get(connection.getID());
        if (playerId != null) {
            Protocol.PlayerInfo info = playerInfoMap.get(playerId);
            if (info != null) {
                info.ready = !info.ready;
                broadcastLobbyUpdate();
            }
        }
    }

    private void handleDisconnect(Connection connection) {
        Integer playerId = connectionToPlayerId.remove(connection.getID());
        if (playerId != null) {
            playerInfoMap.remove(playerId);
            broadcastLobbyUpdate();
        }
    }

    private void broadcastLobbyUpdate() {
        Protocol.LobbyUpdate update = new Protocol.LobbyUpdate();
        update.players = playerInfoMap.values().toArray(new Protocol.PlayerInfo[0]);
        server.sendToAllTCP(update);

        // Also fire locally for the host
        networkManager.fireLobbyUpdate(update);
    }

    private String detectLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                var addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Gdx.app.error("HostServer", "Failed to detect LAN IP", e);
        }
        return "127.0.0.1";
    }
}
