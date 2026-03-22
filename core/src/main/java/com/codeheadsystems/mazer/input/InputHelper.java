package com.codeheadsystems.mazer.input;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

/**
 * Helper for creating the appropriate input processor based on platform.
 */
public class InputHelper {

    /**
     * Returns true if running on a mobile platform (Android/iOS).
     */
    public static boolean isMobile() {
        return Gdx.app.getType() == Application.ApplicationType.Android
                || Gdx.app.getType() == Application.ApplicationType.iOS;
    }

    /**
     * Creates the appropriate input processor for the current platform
     * and sets it as the active input processor.
     * Returns a Runnable that should be called each frame to update the input state.
     */
    public static Runnable setupInput(InputState inputState) {
        if (isMobile()) {
            MobileInputProcessor processor = new MobileInputProcessor(inputState);
            Gdx.input.setInputProcessor(processor);
            return processor::update;
        } else {
            DesktopInputProcessor processor = new DesktopInputProcessor(inputState);
            Gdx.input.setInputProcessor(processor);
            return processor::update;
        }
    }
}
