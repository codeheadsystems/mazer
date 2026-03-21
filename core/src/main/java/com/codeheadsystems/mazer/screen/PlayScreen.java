package com.codeheadsystems.mazer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.codeheadsystems.mazer.input.DesktopInputProcessor;
import com.codeheadsystems.mazer.input.InputState;
import com.codeheadsystems.mazer.maze.MazeGenerator;
import com.codeheadsystems.mazer.maze.MazeGrid;
import com.codeheadsystems.mazer.render.HudRenderer;
import com.codeheadsystems.mazer.render.MazeRenderer;
import com.codeheadsystems.mazer.world.Player;

/**
 * Main gameplay screen. Handles player movement, rendering, and HUD.
 */
public class PlayScreen extends ScreenAdapter {

    private static final float CELL_SIZE = 4.0f;

    private final int mazeWidth;
    private final int mazeHeight;
    private final long mazeSeed;

    private MazeGrid maze;
    private MazeRenderer mazeRenderer;
    private HudRenderer hudRenderer;
    private Player localPlayer;
    private InputState inputState;
    private DesktopInputProcessor inputProcessor;

    public PlayScreen(int mazeWidth, int mazeHeight, long mazeSeed) {
        this.mazeWidth = mazeWidth;
        this.mazeHeight = mazeHeight;
        this.mazeSeed = mazeSeed;
    }

    @Override
    public void show() {
        maze = MazeGenerator.generate(mazeWidth, mazeHeight, CELL_SIZE, mazeSeed);
        mazeRenderer = new MazeRenderer(maze);
        hudRenderer = new HudRenderer(maze);

        // Spawn player in cell (1,1), facing east
        Vector2 spawn = maze.cellToWorld(1, 1);
        localPlayer = new Player(0, spawn.x, spawn.y, 0f);

        inputState = new InputState();
        inputProcessor = new DesktopInputProcessor(inputState);
        Gdx.input.setInputProcessor(inputProcessor);
    }

    @Override
    public void render(float delta) {
        // Clamp delta to prevent physics explosions on lag spikes
        delta = Math.min(delta, 1f / 15f);

        // Update input
        inputProcessor.update();

        // Update player
        localPlayer.update(delta, inputState, maze);

        // Update camera to follow player
        mazeRenderer.updateCamera(localPlayer.getX(), localPlayer.getZ(), localPlayer.getAngle());

        // Render 3D maze
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        mazeRenderer.render();

        // Render HUD (minimap, rear-view mirror, score)
        hudRenderer.render(mazeRenderer, localPlayer);
    }

    @Override
    public void resize(int width, int height) {
        mazeRenderer.resize(width, height);
    }

    @Override
    public void dispose() {
        if (mazeRenderer != null) {
            mazeRenderer.dispose();
        }
        if (hudRenderer != null) {
            hudRenderer.dispose();
        }
    }
}
