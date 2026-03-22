package com.codeheadsystems.mazer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.codeheadsystems.mazer.MazerGame;
import com.codeheadsystems.mazer.render.ViewportHelper;
import com.codeheadsystems.mazer.net.NetworkManager;

/**
 * Game over screen showing the winner and a button to return to menu.
 */
public class GameOverScreen extends ScreenAdapter {

    private final MazerGame game;
    private final NetworkManager networkManager;
    private final int winnerPlayerId;

    private Stage stage;
    private Skin skin;

    public GameOverScreen(MazerGame game, NetworkManager networkManager,
                          int winnerPlayerId) {
        this.game = game;
        this.networkManager = networkManager;
        this.winnerPlayerId = winnerPlayerId;
    }

    @Override
    public void show() {
        stage = new Stage(ViewportHelper.createScaledViewport());
        skin = createSkin();
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // GAME OVER title
        Label title = new Label("GAME OVER", skin, "title");
        root.add(title).padBottom(40).row();

        // Winner info
        String winnerText;
        if (winnerPlayerId < 0) {
            winnerText = "No winner - everyone was eliminated!";
        } else if (winnerPlayerId == networkManager.getLocalPlayerId()) {
            winnerText = "YOU WIN!";
        } else {
            winnerText = networkManager.getPlayerName(winnerPlayerId) + " wins!";
        }
        Label winnerLabel = new Label(winnerText, skin);
        root.add(winnerLabel).padBottom(40).row();

        // Return to menu button
        TextButton menuButton = new TextButton("RETURN TO MENU", skin);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                networkManager.shutdown();
                game.setScreen(new MenuScreen(game));
            }
        });
        root.add(menuButton).width(300).height(50).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }

    private Skin createSkin() {
        Skin s = new Skin();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        s.add("default-font", font);

        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(4f);
        s.add("title-font", titleFont);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(0, 0.3f, 0, 1));
        pixmap.fill();
        s.add("button-up", new Texture(pixmap));

        pixmap.setColor(new Color(0, 0.5f, 0, 1));
        pixmap.fill();
        s.add("button-down", new Texture(pixmap));

        pixmap.dispose();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.GREEN;
        buttonStyle.up = new TextureRegionDrawable(
                new TextureRegion(s.get("button-up", Texture.class)));
        buttonStyle.down = new TextureRegionDrawable(
                new TextureRegion(s.get("button-down", Texture.class)));
        s.add("default", buttonStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.GREEN;
        s.add("default", labelStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = titleFont;
        titleStyle.fontColor = Color.GREEN;
        s.add("title", titleStyle);

        return s;
    }
}
