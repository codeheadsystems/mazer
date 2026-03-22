package com.codeheadsystems.mazer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.codeheadsystems.mazer.screen.MenuScreen;

/**
 * Main game entry point. Manages screen transitions and Android lifecycle.
 */
public class MazerGame extends Game {

    @Override
    public void create() {
        setScreen(new MenuScreen(this));
    }

    @Override
    public void pause() {
        super.pause();
        Gdx.app.log("MazerGame", "Game paused");
    }

    @Override
    public void resume() {
        super.resume();
        Gdx.app.log("MazerGame", "Game resumed");
    }
}
