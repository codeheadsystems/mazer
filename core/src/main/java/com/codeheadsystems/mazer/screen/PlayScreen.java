package com.codeheadsystems.mazer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.codeheadsystems.mazer.MazerGame;
import com.codeheadsystems.mazer.input.InputHelper;
import com.codeheadsystems.mazer.input.InputState;
import com.codeheadsystems.mazer.maze.MazeGenerator;
import com.codeheadsystems.mazer.maze.MazeGrid;
import com.codeheadsystems.mazer.render.BulletRenderer;
import com.codeheadsystems.mazer.render.HudRenderer;
import com.codeheadsystems.mazer.render.MazeRenderer;
import com.codeheadsystems.mazer.render.PlayerModelFactory;
import com.codeheadsystems.mazer.render.PlayerRenderer;
import com.codeheadsystems.mazer.world.GameWorld;
import com.codeheadsystems.mazer.world.Player;

/**
 * Main gameplay screen. Handles player movement, bullets, rendering, and HUD.
 */
public class PlayScreen extends ScreenAdapter {

    private static final float CELL_SIZE = 4.0f;
    private static final int DUMMY_TARGET_COUNT = 3;

    private final MazerGame game;
    private final int mazeWidth;
    private final int mazeHeight;
    private final long mazeSeed;
    private final boolean soloMode;
    private final PlayerModelFactory.Shape playerShape;

    private MazeGrid maze;
    private GameWorld gameWorld;
    private MazeRenderer mazeRenderer;
    private PlayerRenderer playerRenderer;
    private BulletRenderer bulletRenderer;
    private HudRenderer hudRenderer;
    private Player localPlayer;
    private InputState inputState;
    private Runnable inputUpdater;

    public PlayScreen(MazerGame game, int mazeWidth, int mazeHeight, long mazeSeed,
                      boolean soloMode, PlayerModelFactory.Shape playerShape) {
        this.game = game;
        this.mazeWidth = mazeWidth;
        this.mazeHeight = mazeHeight;
        this.mazeSeed = mazeSeed;
        this.soloMode = soloMode;
        this.playerShape = playerShape;
    }

    public PlayScreen(int mazeWidth, int mazeHeight, long mazeSeed) {
        this(null, mazeWidth, mazeHeight, mazeSeed, true, PlayerModelFactory.Shape.CUBE);
    }

    @Override
    public void show() {
        maze = MazeGenerator.generate(mazeWidth, mazeHeight, CELL_SIZE, mazeSeed);
        gameWorld = new GameWorld(maze);
        mazeRenderer = new MazeRenderer(maze);
        playerRenderer = new PlayerRenderer();
        bulletRenderer = new BulletRenderer();
        hudRenderer = new HudRenderer(maze);

        // Spawn local player in cell (1,1), facing east
        Vector2 spawn = maze.cellToWorld(1, 1);
        localPlayer = new Player(0, spawn.x, spawn.y, 0f, playerShape);
        gameWorld.addPlayer(localPlayer);

        // In solo mode, spawn dummy targets to shoot at
        if (soloMode) {
            gameWorld.spawnDummyTargets(DUMMY_TARGET_COUNT, 1, 1, mazeSeed + 1);
        }

        inputState = new InputState();
        inputUpdater = InputHelper.setupInput(inputState);
    }

    @Override
    public void render(float delta) {
        // Clamp delta to prevent physics explosions on lag spikes
        delta = Math.min(delta, 1f / 15f);

        // Update input
        inputUpdater.run();

        // Update world simulation (player movement, bullets, collisions)
        gameWorld.update(delta, localPlayer, inputState);

        // Update camera to follow player
        mazeRenderer.setMoving(inputState.moveForward);
        mazeRenderer.updateCamera(localPlayer.getX(), localPlayer.getZ(), localPlayer.getAngle());

        // Render 3D maze
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        mazeRenderer.render();

        // Render other players
        playerRenderer.render(mazeRenderer.getCamera(),
                gameWorld.getPlayers(), localPlayer.getId());

        // Render bullets
        bulletRenderer.render(mazeRenderer.getCamera(), gameWorld.getBullets());

        // Render HUD (minimap, rear-view mirror, score)
        hudRenderer.render(mazeRenderer, localPlayer);

        // Check for leave request
        if (hudRenderer.isLeaveRequested() && game != null) {
            game.setScreen(new MenuScreen(game));
        }
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
