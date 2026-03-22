package com.codeheadsystems.mazer.render;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.codeheadsystems.mazer.input.MobileInputProcessor;
import com.codeheadsystems.mazer.maze.MazeGrid;
import com.codeheadsystems.mazer.world.Player;

/**
 * Composes all HUD elements: minimap, rear-view mirror, score display,
 * hit flash overlay, leave button, and on-screen fire button (mobile only).
 */
public class HudRenderer implements Disposable {

    private static final int SCORE_PADDING = 20;
    private static final float HIT_FLASH_DURATION = 0.3f;
    private static final Color FIRE_BUTTON_COLOR = new Color(0.8f, 0.1f, 0.1f, 0.6f);
    private static final Color FIRE_BUTTON_BORDER_COLOR = new Color(1f, 0.2f, 0.2f, 0.8f);
    private static final Color LEAVE_BUTTON_COLOR = new Color(0.3f, 0.3f, 0.3f, 0.6f);
    private static final Color LEAVE_BUTTON_BORDER_COLOR = new Color(0.6f, 0.6f, 0.6f, 0.8f);

    private static final float LEAVE_BTN_WIDTH = 80f;
    private static final float LEAVE_BTN_HEIGHT = 30f;
    private static final float LEAVE_BTN_PADDING = 10f;

    private final MinimapRenderer minimapRenderer;
    private final RearViewRenderer rearViewRenderer;
    private final SpriteBatch spriteBatch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final GlyphLayout glyphLayout;
    private final boolean isMobile;

    private float hitFlashTimer = 0f;
    private int lastKnownScore = -1;
    private boolean leaveRequested = false;

    public HudRenderer(MazeGrid maze) {
        this.minimapRenderer = new MinimapRenderer(maze);
        this.rearViewRenderer = new RearViewRenderer();
        this.spriteBatch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.font = new BitmapFont();
        font.setColor(Color.GREEN);
        font.getData().setScale(2f);
        this.smallFont = new BitmapFont();
        smallFont.setColor(Color.WHITE);
        this.glyphLayout = new GlyphLayout();
        this.isMobile = Gdx.app.getType() == Application.ApplicationType.Android
                || Gdx.app.getType() == Application.ApplicationType.iOS;
    }

    /**
     * Triggers the hit flash effect.
     */
    public void triggerHitFlash() {
        hitFlashTimer = HIT_FLASH_DURATION;
    }

    /**
     * Returns true if the player requested to leave (ESC key or tapped leave button).
     * Resets after being read.
     */
    public boolean isLeaveRequested() {
        boolean result = leaveRequested;
        leaveRequested = false;
        return result;
    }

    /**
     * Renders all HUD elements.
     */
    public void render(MazeRenderer mazeRenderer, Player localPlayer) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Check ESC key
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            leaveRequested = true;
        }

        // Detect score decrease to auto-trigger hit flash
        if (lastKnownScore >= 0 && localPlayer.getScore() < lastKnownScore) {
            triggerHitFlash();
        }
        lastKnownScore = localPlayer.getScore();

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

        // Leave button (lower left)
        renderLeaveButton(screenWidth, screenHeight);

        // Fire button (mobile only)
        if (isMobile) {
            renderFireButton(screenWidth, screenHeight);
        }

        // Hit flash overlay
        if (hitFlashTimer > 0) {
            hitFlashTimer -= Gdx.graphics.getDeltaTime();
            float alpha = Math.max(0, hitFlashTimer / HIT_FLASH_DURATION) * 0.4f;
            renderHitFlash(screenWidth, screenHeight, alpha);
        }
    }

    private void renderLeaveButton(int screenWidth, int screenHeight) {
        float x = LEAVE_BTN_PADDING;
        float y = LEAVE_BTN_PADDING;

        // Check for touch/click on the leave button
        if (Gdx.input.justTouched()) {
            int touchX = Gdx.input.getX();
            int touchY = Gdx.input.getY();
            // Convert screen coords (y-down) to GL coords (y-up)
            float glY = screenHeight - touchY;
            if (touchX >= x && touchX <= x + LEAVE_BTN_WIDTH
                    && glY >= y && glY <= y + LEAVE_BTN_HEIGHT) {
                leaveRequested = true;
            }
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(LEAVE_BUTTON_COLOR);
        shapeRenderer.rect(x, y, LEAVE_BTN_WIDTH, LEAVE_BTN_HEIGHT);
        shapeRenderer.end();

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(LEAVE_BUTTON_BORDER_COLOR);
        shapeRenderer.rect(x, y, LEAVE_BTN_WIDTH, LEAVE_BTN_HEIGHT);
        shapeRenderer.end();

        // Label
        spriteBatch.begin();
        glyphLayout.setText(smallFont, "LEAVE");
        float textX = x + (LEAVE_BTN_WIDTH - glyphLayout.width) / 2f;
        float textY = y + (LEAVE_BTN_HEIGHT + glyphLayout.height) / 2f;
        smallFont.draw(spriteBatch, "LEAVE", textX, textY);
        spriteBatch.end();
    }

    private void renderHitFlash(int screenWidth, int screenHeight, float alpha) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 0f, 0f, alpha);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();
    }

    private void renderFireButton(int screenWidth, int screenHeight) {
        float[] bounds = MobileInputProcessor.getFireButtonBounds(screenWidth, screenHeight);
        float x = bounds[0];
        float y = bounds[1];
        float w = bounds[2];
        float h = bounds[3];

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Filled background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(FIRE_BUTTON_COLOR);
        shapeRenderer.circle(x + w / 2f, y + h / 2f, w / 2f, 32);
        shapeRenderer.end();

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(FIRE_BUTTON_BORDER_COLOR);
        shapeRenderer.circle(x + w / 2f, y + h / 2f, w / 2f, 32);
        shapeRenderer.end();

        // "FIRE" label
        spriteBatch.begin();
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "FIRE", x + w * 0.15f, y + h * 0.6f);
        font.setColor(Color.GREEN);
        spriteBatch.end();
    }

    @Override
    public void dispose() {
        minimapRenderer.dispose();
        rearViewRenderer.dispose();
        spriteBatch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        smallFont.dispose();
    }
}
