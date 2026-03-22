package com.codeheadsystems.mazer.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

/**
 * Desktop input: mouse button held = move forward, mouse X position = turn,
 * spacebar = fire.
 */
public class DesktopInputProcessor extends InputAdapter {

    private final InputState inputState;

    public DesktopInputProcessor(InputState inputState) {
        this.inputState = inputState;
    }

    /**
     * Call each frame to update the input state from current mouse/keyboard state.
     */
    public void update() {
        // Forward movement: any mouse button held
        inputState.moveForward = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
                || Gdx.input.isButtonPressed(Input.Buttons.RIGHT);

        // Turn: mouse X position relative to screen center
        if (inputState.moveForward) {
            float screenCenterX = Gdx.graphics.getWidth() / 2f;
            float mouseX = Gdx.input.getX();
            // Map to -1..+1, with dead zone in the center 10%
            float normalized = (mouseX - screenCenterX) / screenCenterX;
            float deadZone = 0.05f;
            if (Math.abs(normalized) < deadZone) {
                inputState.turnAmount = 0f;
            } else {
                // Remove dead zone from the range and re-normalize
                float sign = Math.signum(normalized);
                inputState.turnAmount = sign * (Math.abs(normalized) - deadZone) / (1f - deadZone);
                inputState.turnAmount = Math.max(-1f, Math.min(1f, inputState.turnAmount));
            }
        } else {
            inputState.turnAmount = 0f;
        }

        // Fire: spacebar (level-triggered, held = fire).
        // Server-side cooldown (1sec) prevents rapid fire.
        // Level-triggered ensures the fire signal isn't missed by throttled UDP sends.
        inputState.fire = Gdx.input.isKeyPressed(Input.Keys.SPACE);
    }
}
