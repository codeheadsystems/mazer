package com.codeheadsystems.mazer;

import com.badlogic.gdx.Game;
import com.codeheadsystems.mazer.screen.MenuScreen;

/**
 * Main game entry point. Manages screen transitions.
 */
public class MazerGame extends Game {

    @Override
    public void create() {
        setScreen(new MenuScreen(this));
    }
}
