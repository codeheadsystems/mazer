package com.codeheadsystems.mazer;

import com.badlogic.gdx.Game;
import com.codeheadsystems.mazer.screen.PlayScreen;

/**
 * Main game entry point. Manages screen transitions.
 */
public class MazerGame extends Game {

    @Override
    public void create() {
        // For now, go straight into a 10x10 maze game
        setScreen(new PlayScreen(10, 10, System.currentTimeMillis()));
    }
}
