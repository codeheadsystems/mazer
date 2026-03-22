package com.codeheadsystems.mazer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.codeheadsystems.mazer.platform.DefaultPlatformServices;
import com.codeheadsystems.mazer.platform.PlatformServices;
import com.codeheadsystems.mazer.screen.MenuScreen;

/**
 * Main game entry point. Manages screen transitions, Android lifecycle,
 * and persistent user preferences.
 */
public class MazerGame extends Game {

    private static final String PREFS_NAME = "mazer-prefs";
    private static final String PREF_PLAYER_NAME = "playerName";
    private static final String PREF_HOST_IP = "hostIp";

    private final PlatformServices platformServices;
    private Preferences prefs;

    public MazerGame() {
        this(new DefaultPlatformServices());
    }

    public MazerGame(PlatformServices platformServices) {
        this.platformServices = platformServices;
    }

    public PlatformServices getPlatformServices() {
        return platformServices;
    }

    public String getLastPlayerName() {
        return prefs.getString(PREF_PLAYER_NAME, "Player");
    }

    public void setLastPlayerName(String name) {
        prefs.putString(PREF_PLAYER_NAME, name);
        prefs.flush();
    }

    public String getLastHostIp() {
        return prefs.getString(PREF_HOST_IP, "192.168.1.");
    }

    public void setLastHostIp(String ip) {
        prefs.putString(PREF_HOST_IP, ip);
        prefs.flush();
    }

    @Override
    public void create() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        setScreen(new MenuScreen(this));
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void resume() {
        super.resume();
    }
}
