package com.codeheadsystems.mazer.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.codeheadsystems.mazer.maze.MazeGrid;
import com.codeheadsystems.mazer.world.Player;

/**
 * Composes all HUD elements: minimap, rear-view mirror, and score display.
 */
public class HudRenderer implements Disposable {

    private static final int SCORE_PADDING = 20;

    private final MinimapRenderer minimapRenderer;
    private final RearViewRenderer rearViewRenderer;
    private final SpriteBatch spriteBatch;
    private final BitmapFont font;

    public HudRenderer(MazeGrid maze) {
        this.minimapRenderer = new MinimapRenderer(maze);
        this.rearViewRenderer = new RearViewRenderer();
        this.spriteBatch = new SpriteBatch();
        this.font = new BitmapFont();
        font.setColor(Color.GREEN);
        font.getData().setScale(2f);
    }

    /**
     * Renders all HUD elements.
     */
    public void render(MazeRenderer mazeRenderer, Player localPlayer) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Rear-view mirror (top center)
        rearViewRenderer.render(mazeRenderer,
                localPlayer.getX(), localPlayer.getZ(), localPlayer.getAngle(),
                screenWidth, screenHeight);

        // Minimap (upper right)
        minimapRenderer.render(
                localPlayer.getX(), localPlayer.getZ(), localPlayer.getAngle(),
                screenWidth, screenHeight);

        // Score (upper left)
        spriteBatch.begin();
        font.draw(spriteBatch, "HP: " + localPlayer.getScore(),
                SCORE_PADDING, screenHeight - SCORE_PADDING);
        spriteBatch.end();
    }

    @Override
    public void dispose() {
        minimapRenderer.dispose();
        rearViewRenderer.dispose();
        spriteBatch.dispose();
        font.dispose();
    }
}
