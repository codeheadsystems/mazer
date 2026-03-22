package com.codeheadsystems.mazer.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;

/**
 * Mobile/touch input: touch anywhere to move forward, X position = turn,
 * touch in the fire button region (lower-right) = fire.
 *
 * The fire button occupies the lower-right corner of the screen.
 * Touches inside that region trigger fire but do NOT contribute to movement/steering.
 */
public class MobileInputProcessor extends InputAdapter {

    private static final float FIRE_BUTTON_SIZE_FRACTION = 0.15f; // 15% of screen width/height
    private static final float DEAD_ZONE = 0.05f;

    private final InputState inputState;
    private boolean fireButtonTouched;
    private int moveTouchPointer = -1; // which pointer is driving movement

    public MobileInputProcessor(InputState inputState) {
        this.inputState = inputState;
    }

    /**
     * Call each frame to update the input state from current touch state.
     */
    public void update() {
        inputState.reset();

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        // Check all active touch pointers
        for (int i = 0; i < 5; i++) {
            if (!Gdx.input.isTouched(i)) continue;

            int touchX = Gdx.input.getX(i);
            int touchY = Gdx.input.getY(i);

            if (isInFireButtonRegion(touchX, touchY, screenW, screenH)) {
                inputState.fire = true;
            } else {
                // This touch drives movement
                inputState.moveForward = true;

                float screenCenterX = screenW / 2f;
                float normalized = (touchX - screenCenterX) / screenCenterX;

                if (Math.abs(normalized) < DEAD_ZONE) {
                    inputState.turnAmount = 0f;
                } else {
                    float sign = Math.signum(normalized);
                    inputState.turnAmount = sign * (Math.abs(normalized) - DEAD_ZONE) / (1f - DEAD_ZONE);
                    inputState.turnAmount = Math.max(-1f, Math.min(1f, inputState.turnAmount));
                }
            }
        }
    }

    /**
     * Returns true if the touch coordinates are inside the fire button region
     * (lower-right corner of the screen).
     */
    public static boolean isInFireButtonRegion(int touchX, int touchY, int screenW, int screenH) {
        float btnW = screenW * FIRE_BUTTON_SIZE_FRACTION;
        float btnH = screenH * FIRE_BUTTON_SIZE_FRACTION;
        // touchY is in screen coords (0 = top), fire button is in lower-right
        return touchX >= screenW - btnW * 1.5f && touchY >= screenH - btnH * 1.5f;
    }

    /**
     * Returns the fire button bounds for rendering by HudRenderer.
     * Returns [x, y, width, height] in screen coordinates (y-up, for SpriteBatch).
     */
    public static float[] getFireButtonBounds(int screenW, int screenH) {
        float btnSize = Math.min(screenW, screenH) * FIRE_BUTTON_SIZE_FRACTION;
        float padding = btnSize * 0.3f;
        float x = screenW - btnSize - padding;
        float y = padding; // y-up: bottom of screen
        return new float[]{x, y, btnSize, btnSize};
    }
}
