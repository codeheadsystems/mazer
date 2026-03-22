package com.codeheadsystems.mazer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.codeheadsystems.mazer.platform.DefaultPlatformServices;
import com.codeheadsystems.mazer.platform.PlatformServices;
import com.codeheadsystems.mazer.screen.MenuScreen;

/**
 * Main game entry point. Manages screen transitions and Android lifecycle.
 */
public class MazerGame extends Game {

    private final PlatformServices platformServices;

    public MazerGame() {
        this(new DefaultPlatformServices());
    }

    public MazerGame(PlatformServices platformServices) {
        this.platformServices = platformServices;
    }

    public PlatformServices getPlatformServices() {
        return platformServices;
    }

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
