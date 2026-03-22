package com.codeheadsystems.mazer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.codeheadsystems.mazer.MazerGame;
import com.codeheadsystems.mazer.input.InputHelper;
import com.codeheadsystems.mazer.input.InputState;
import com.codeheadsystems.mazer.maze.MazeGenerator;
import com.codeheadsystems.mazer.maze.MazeGrid;
import com.codeheadsystems.mazer.net.NetworkManager;
import com.codeheadsystems.mazer.net.Protocol;
import com.codeheadsystems.mazer.render.BulletRenderer;
import com.codeheadsystems.mazer.render.HudRenderer;
import com.codeheadsystems.mazer.render.MazeRenderer;
import com.codeheadsystems.mazer.render.PlayerModelFactory;
import com.codeheadsystems.mazer.render.PlayerRenderer;
import com.codeheadsystems.mazer.world.Bullet;
import com.codeheadsystems.mazer.world.GameWorld;
import com.codeheadsystems.mazer.world.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Networked gameplay screen that works for both host and client.
 * Host: uses local GameWorld for rendering (simulated by HostServer tick).
 * Client: receives GameSnapshots and interpolates remote state.
 */
public class NetworkedPlayScreen extends ScreenAdapter {

    private static final float CELL_SIZE = 4.0f;
    private static final float LERP_SPEED = 15f;

    private final MazerGame game;
    private final NetworkManager networkManager;
    private final Protocol.StartGame startMsg;

    private MazeGrid maze;
    private GameWorld gameWorld;
    private MazeRenderer mazeRenderer;
    private PlayerRenderer playerRenderer;
    private BulletRenderer bulletRenderer;
    private HudRenderer hudRenderer;
    private Player localPlayer;
    private InputState inputState;
    private Runnable inputUpdater;

    // Client-side snapshot state
    private final List<Bullet> clientBullets = new ArrayList<>();

    public NetworkedPlayScreen(MazerGame game, NetworkManager networkManager,
                               Protocol.StartGame startMsg) {
        this.game = game;
        this.networkManager = networkManager;
        this.startMsg = startMsg;
    }

    @Override
    public void show() {
        maze = MazeGenerator.generate(startMsg.mazeWidth, startMsg.mazeHeight,
                CELL_SIZE, startMsg.mazeSeed);
        gameWorld = new GameWorld(maze);
        mazeRenderer = new MazeRenderer(maze);
        playerRenderer = new PlayerRenderer();
        bulletRenderer = new BulletRenderer();
        hudRenderer = new HudRenderer(maze);

        // Create all players from the start message
        int localId = networkManager.getLocalPlayerId();
        for (int i = 0; i < startMsg.spawnX.length; i++) {
            // Use index as player id (matches how HostServer assigns them)
            Player p = new Player(i, startMsg.spawnX[i], startMsg.spawnZ[i],
                    startMsg.spawnAngle[i], PlayerModelFactory.Shape.CUBE);
            gameWorld.addPlayer(p);
            if (i == localId) {
                localPlayer = p;
            }
        }

        if (localPlayer == null) {
            // Fallback: shouldn't happen but be safe
            localPlayer = gameWorld.getPlayers().get(0);
        }

        inputState = new InputState();
        inputUpdater = InputHelper.setupInput(inputState);

        // Register network callbacks
        networkManager.setOnGameSnapshot(this::onGameSnapshot);
        networkManager.setOnPlayerHit(this::onPlayerHit);
        networkManager.setOnPlayerEliminated(this::onPlayerEliminated);
        networkManager.setOnGameOver(this::onGameOver);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 15f);

        // Update input
        inputUpdater.run();

        // Send input to server
        networkManager.sendPlayerInput(inputState);

        if (networkManager.isHost()) {
            // Host: apply input locally for client-side prediction
            // The authoritative simulation runs in HostServer.tick()
            // but we predict locally for responsive feel
            localPlayer.update(delta, inputState, maze);
        } else {
            // Client: apply local input for client-side prediction
            localPlayer.update(delta, inputState, maze);

            // Update client bullet positions (simple forward projection)
            var iter = clientBullets.iterator();
            while (iter.hasNext()) {
                Bullet b = iter.next();
                if (!b.update(delta, maze)) {
                    iter.remove();
                }
            }
        }

        // Update camera to follow local player
        mazeRenderer.updateCamera(localPlayer.getX(), localPlayer.getZ(),
                localPlayer.getAngle());

        // Render
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        mazeRenderer.render();

        // Render other players
        playerRenderer.render(mazeRenderer.getCamera(),
                gameWorld.getPlayers(), localPlayer.getId());

        // Render bullets
        List<Bullet> bulletsToRender = networkManager.isHost()
                ? gameWorld.getBullets() : clientBullets;
        bulletRenderer.render(mazeRenderer.getCamera(), bulletsToRender);

        // Render HUD
        hudRenderer.render(mazeRenderer, localPlayer);
    }

    private void onGameSnapshot(Protocol.GameSnapshot snapshot) {
        int localId = networkManager.getLocalPlayerId();

        // Update player states from snapshot
        for (Protocol.PlayerSnapshot ps : snapshot.players) {
            Player player = findPlayer(ps.id);
            if (player == null) continue;

            if (ps.id == localId) {
                // Reconcile local player: snap score/alive from server,
                // lerp position to reduce jitter while keeping prediction
                player.setScore(ps.score);
                player.setAlive(ps.alive);
                if (!networkManager.isHost()) {
                    // Gentle reconciliation toward server position
                    float dx = ps.x - player.getX();
                    float dz = ps.z - player.getZ();
                    float distSq = dx * dx + dz * dz;
                    if (distSq > 4f) {
                        // Snap if too far off
                        player.setPosition(ps.x, ps.z);
                        player.setAngle(ps.angle);
                    } else if (distSq > 0.01f) {
                        float t = Math.min(1f, LERP_SPEED * Gdx.graphics.getDeltaTime());
                        player.setPosition(
                                MathUtils.lerp(player.getX(), ps.x, t),
                                MathUtils.lerp(player.getZ(), ps.z, t));
                    }
                }
            } else {
                // Remote players: interpolate smoothly
                float t = Math.min(1f, LERP_SPEED * Gdx.graphics.getDeltaTime());
                player.setPosition(
                        MathUtils.lerp(player.getX(), ps.x, t),
                        MathUtils.lerp(player.getZ(), ps.z, t));
                player.setAngle(ps.angle);
                player.setScore(ps.score);
                player.setAlive(ps.alive);
            }
        }

        // Update client-side bullets from snapshot (clients only)
        if (!networkManager.isHost()) {
            clientBullets.clear();
            for (Protocol.BulletSnapshot bs : snapshot.bullets) {
                float angle = MathUtils.atan2(bs.dirZ, bs.dirX);
                clientBullets.add(new Bullet(bs.x, bs.z, angle, bs.ownerId));
            }
        }
    }

    private void onPlayerHit(Protocol.PlayerHit msg) {
        Player hit = findPlayer(msg.hitPlayerId);
        if (hit != null) {
            hit.setScore(msg.newScore);
        }
    }

    private void onPlayerEliminated(Protocol.PlayerEliminated msg) {
        Player eliminated = findPlayer(msg.playerId);
        if (eliminated != null) {
            eliminated.setAlive(false);
        }
    }

    private void onGameOver(Protocol.GameOver msg) {
        game.setScreen(new GameOverScreen(game, networkManager, msg.winnerPlayerId));
    }

    private Player findPlayer(int id) {
        for (Player p : gameWorld.getPlayers()) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    @Override
    public void resize(int width, int height) {
        mazeRenderer.resize(width, height);
    }

    @Override
    public void dispose() {
        if (mazeRenderer != null) mazeRenderer.dispose();
        if (playerRenderer != null) playerRenderer.dispose();
        if (bulletRenderer != null) bulletRenderer.dispose();
        if (hudRenderer != null) hudRenderer.dispose();
    }
}
