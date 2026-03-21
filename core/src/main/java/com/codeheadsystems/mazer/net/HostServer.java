package com.codeheadsystems.mazer.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.codeheadsystems.mazer.maze.MazeGenerator;
import com.codeheadsystems.mazer.maze.MazeGrid;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Host-side networking. Runs a KryoNet Server, manages lobby state,
 * and coordinates game start.
 */
public class HostServer {

    private static final int MAX_PLAYERS = 8;
    private static final float CELL_SIZE = 4.0f;

    private final NetworkManager networkManager;
    private Server server;
    private String hostIp;
    private int nextPlayerId = 1; // 0 is reserved for host

    // Lobby state
    private final Map<Integer, Protocol.PlayerInfo> playerInfoMap = new HashMap<>();
    private final Map<Integer, Integer> connectionToPlayerId = new HashMap<>();

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
        MazeGrid maze = MazeGenerator.generate(mazeWidth, mazeHeight, CELL_SIZE, seed);

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

        server.sendToAllTCP(msg);

        // Also fire locally for the host
        networkManager.fireGameStart(msg);
    }

    public void stop() {
        if (server != null) {
            server.stop();
            server.close();
        }
    }

    public List<Protocol.PlayerInfo> getPlayerInfos() {
        return new ArrayList<>(playerInfoMap.values());
    }

    private void handleMessage(Connection connection, Object object) {
        if (object instanceof Protocol.JoinRequest req) {
            handleJoinRequest(connection, req);
        } else if (object instanceof Protocol.ReadyToggle) {
            handleReadyToggle(connection);
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
