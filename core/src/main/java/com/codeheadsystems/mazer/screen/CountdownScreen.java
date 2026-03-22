package com.codeheadsystems.mazer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.codeheadsystems.mazer.MazerGame;
import com.codeheadsystems.mazer.maze.MazeGenerator;
import com.codeheadsystems.mazer.maze.MazeGrid;
import com.codeheadsystems.mazer.net.NetworkManager;
import com.codeheadsystems.mazer.net.Protocol;
import com.codeheadsystems.mazer.render.MazeRenderer;

/**
 * Shows a 5-second countdown before gameplay begins.
 * Players see the maze from their spawn position during the countdown.
 */
public class CountdownScreen extends ScreenAdapter {

    private static final float COUNTDOWN_SECONDS = 5f;
    private static final float CELL_SIZE = 4.0f;

    private final MazerGame game;
    private final NetworkManager networkManager;
    private final Protocol.StartGame startMsg;

    private MazeGrid maze;
    private MazeRenderer mazeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont countdownFont;
    private GlyphLayout glyphLayout;
    private float elapsed;
    private boolean transitioned;

    public CountdownScreen(MazerGame game, NetworkManager networkManager,
                           Protocol.StartGame startMsg) {
        this.game = game;
        this.networkManager = networkManager;
        this.startMsg = startMsg;
    }

    @Override
    public void show() {
        maze = MazeGenerator.generate(startMsg.mazeWidth, startMsg.mazeHeight,
                CELL_SIZE, startMsg.mazeSeed);
        mazeRenderer = new MazeRenderer(maze);
        spriteBatch = new SpriteBatch();
        countdownFont = new BitmapFont();
        countdownFont.getData().setScale(6f);
        countdownFont.setColor(Color.GREEN);
        glyphLayout = new GlyphLayout();
        elapsed = 0f;
        transitioned = false;

        // Position camera at local player's spawn
        int localId = networkManager.getLocalPlayerId();
        if (localId >= 0 && localId < startMsg.spawnX.length) {
            mazeRenderer.updateCamera(
                    startMsg.spawnX[localId],
                    startMsg.spawnZ[localId],
                    startMsg.spawnAngle[localId]);
        }
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        // Render maze from spawn viewpoint
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        mazeRenderer.render();

        // Draw countdown number
        int remaining = Math.max(1, (int) Math.ceil(COUNTDOWN_SECONDS - elapsed));
        String text;
        if (elapsed >= COUNTDOWN_SECONDS) {
            text = "GO!";
        } else {
            text = String.valueOf(remaining);
        }

        spriteBatch.begin();
        glyphLayout.setText(countdownFont, text);
        float textX = (Gdx.graphics.getWidth() - glyphLayout.width) / 2f;
        float textY = (Gdx.graphics.getHeight() + glyphLayout.height) / 2f;
        countdownFont.draw(spriteBatch, text, textX, textY);
        spriteBatch.end();

        // Transition after countdown + small delay for "GO!"
        if (elapsed >= COUNTDOWN_SECONDS + 0.5f && !transitioned) {
            transitioned = true;
            game.setScreen(new NetworkedPlayScreen(game, networkManager, startMsg));
        }
    }

    @Override
    public void resize(int width, int height) {
        mazeRenderer.resize(width, height);
    }

    @Override
    public void dispose() {
        if (mazeRenderer != null) mazeRenderer.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (countdownFont != null) countdownFont.dispose();
    }
}
