package com.codeheadsystems.mazer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.codeheadsystems.mazer.MazerGame;
import com.codeheadsystems.mazer.render.ViewportHelper;
import com.codeheadsystems.mazer.net.NetworkManager;
import com.codeheadsystems.mazer.net.Protocol;
import com.codeheadsystems.mazer.net.QrCodeUtil;
import com.badlogic.gdx.graphics.Pixmap;

/**
 * Lobby screen showing connected players, ready status, and QR code for host.
 */
public class LobbyScreen extends ScreenAdapter {

    private final MazerGame game;
    private final NetworkManager networkManager;
    private final int mazeWidth;
    private final int mazeHeight;

    private Stage stage;
    private Skin skin;
    private Table playerListTable;
    private Label statusLabel;
    private TextButton startButton;
    private Texture qrTexture;

    public LobbyScreen(MazerGame game, NetworkManager networkManager,
                       int mazeWidth, int mazeHeight) {
        this.game = game;
        this.networkManager = networkManager;
        this.mazeWidth = mazeWidth;
        this.mazeHeight = mazeHeight;
    }

    @Override
    public void show() {
        stage = new Stage(ViewportHelper.createScaledViewport());
        skin = createSkin();
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Title
        Label title = new Label("LOBBY", skin, "title");
        root.add(title).padBottom(20).colspan(2).row();

        // Status
        statusLabel = new Label("Waiting for players...", skin);
        root.add(statusLabel).padBottom(20).colspan(2).row();

        // Single-column layout (works for portrait and landscape)
        Table contentTable = new Table();

        // QR code / IP (host only, show first so joiners see it)
        if (networkManager.isHost()) {
            String connectString = networkManager.getConnectString();
            if (connectString != null) {
                String ip = connectString.replace("mazer://", "").split(":")[0];

                Label ipHeaderLabel = new Label("Join IP:", skin);
                contentTable.add(ipHeaderLabel).padBottom(3).row();

                Label ipLabel = new Label(ip, skin, "title");
                contentTable.add(ipLabel).padBottom(10).row();

                qrTexture = QrCodeUtil.generateQrTexture(connectString, 200);
                if (qrTexture != null) {
                    Image qrImage = new Image(new TextureRegionDrawable(
                            new TextureRegion(qrTexture)));
                    contentTable.add(qrImage).size(120).padBottom(10).row();
                }
            }
        }

        // Player list
        playerListTable = new Table();
        playerListTable.top();
        contentTable.add(playerListTable).fillX().padBottom(10).row();

        root.add(contentTable).padBottom(30).colspan(2).row();

        // Buttons (stacked vertically for portrait compatibility)
        TextButton readyButton = new TextButton("READY", skin);
        readyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                networkManager.toggleReady();
            }
        });
        root.add(readyButton).width(250).height(45).padBottom(10).row();

        if (networkManager.isHost()) {
            startButton = new TextButton("START GAME", skin);
            startButton.setDisabled(true);
            startButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!startButton.isDisabled()) {
                        networkManager.requestStartGame(mazeWidth, mazeHeight);
                    }
                }
            });
            root.add(startButton).width(250).height(45).padBottom(10).row();
        }

        TextButton backButton = new TextButton("LEAVE", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                networkManager.shutdown();
                game.setScreen(new MenuScreen(game));
            }
        });
        root.add(backButton).width(250).height(45).padBottom(10).row();

        // Set up network callbacks
        networkManager.setOnLobbyUpdate(this::onLobbyUpdate);
        networkManager.setOnGameStart(this::onGameStart);
        networkManager.setOnDisconnected(this::onDisconnected);
    }

    private void onLobbyUpdate(Protocol.LobbyUpdate update) {
        playerListTable.clear();
        playerListTable.add(new Label("PLAYERS:", skin)).left().padBottom(10).row();

        boolean allReady = update.players.length > 1; // need at least 2 players
        for (Protocol.PlayerInfo info : update.players) {
            String readyStr = info.ready ? " [READY]" : "";
            String shapeStr = switch (info.shapeIndex) {
                case 0 -> "Cube";
                case 1 -> "Sphere";
                case 2 -> "Eyeball";
                default -> "?";
            };
            Label playerLabel = new Label(
                    info.name + " (" + shapeStr + ")" + readyStr, skin);
            if (info.id == networkManager.getLocalPlayerId()) {
                playerLabel.setColor(Color.WHITE);
            }
            playerListTable.add(playerLabel).left().padBottom(5).row();

            if (!info.ready) allReady = false;
        }

        // Enable start button if host and all ready
        if (startButton != null) {
            startButton.setDisabled(!allReady);
        }

        statusLabel.setText(update.players.length + " player(s) connected");
    }

    private void onGameStart(Protocol.StartGame msg) {
        game.setScreen(new CountdownScreen(game, networkManager, msg));
    }

    private void onDisconnected() {
        statusLabel.setText("Disconnected from host!");
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
        if (qrTexture != null) qrTexture.dispose();
    }

    private Skin createSkin() {
        Skin s = new Skin();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        s.add("default-font", font);

        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(3f);
        s.add("title-font", titleFont);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(0, 0.3f, 0, 1));
        pixmap.fill();
        s.add("button-up", new Texture(pixmap));

        pixmap.setColor(new Color(0, 0.5f, 0, 1));
        pixmap.fill();
        s.add("button-down", new Texture(pixmap));

        pixmap.setColor(new Color(0.1f, 0.1f, 0.1f, 1));
        pixmap.fill();
        s.add("button-disabled", new Texture(pixmap));

        pixmap.dispose();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.GREEN;
        buttonStyle.up = new TextureRegionDrawable(
                new TextureRegion(s.get("button-up", Texture.class)));
        buttonStyle.down = new TextureRegionDrawable(
                new TextureRegion(s.get("button-down", Texture.class)));
        buttonStyle.disabled = new TextureRegionDrawable(
                new TextureRegion(s.get("button-disabled", Texture.class)));
        buttonStyle.disabledFontColor = Color.DARK_GRAY;
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
