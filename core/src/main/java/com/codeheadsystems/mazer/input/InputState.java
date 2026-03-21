package com.codeheadsystems.mazer.input;

/**
 * Unified input abstraction consumed by the game loop.
 * Platform-specific input processors produce this each frame.
 */
public class InputState {

    /** True while the player is holding forward (mouse/touch held). */
    public boolean moveForward;

    /** Turn amount from -1.0 (full left) to +1.0 (full right). 0 = no turn. */
    public float turnAmount;

    /** True on the frame the player presses fire. */
    public boolean fire;

    public void reset() {
        moveForward = false;
        turnAmount = 0f;
        fire = false;
    }
}
