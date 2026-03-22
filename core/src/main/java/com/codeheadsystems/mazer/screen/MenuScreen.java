package com.codeheadsystems.mazer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.codeheadsystems.mazer.MazerGame;
import com.codeheadsystems.mazer.render.ViewportHelper;
import com.codeheadsystems.mazer.net.NetworkManager;
import com.codeheadsystems.mazer.render.PlayerModelFactory;

/**
 * Main menu screen. Solo starts a local game. Host/Join are placeholders for networking.
 */
public class MenuScreen extends ScreenAdapter {

    private final MazerGame game;
    private Stage stage;
    private Skin skin;

    public MenuScreen(MazerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(ViewportHelper.createScaledViewport());
        skin = createMinimalSkin();
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Title
        Label title = new Label("MAZER", skin, "title");
        root.add(title).padBottom(40).row();

        // Maze size selector
        Label sizeLabel = new Label("Maze Size:", skin);
        root.add(sizeLabel).padBottom(5).row();

        SelectBox<String> sizeSelect = new SelectBox<>(skin);
        sizeSelect.setItems("8x8", "10x10", "16x16", "20x20", "32x32");
        sizeSelect.setSelectedIndex(1); // default 10x10
        root.add(sizeSelect).width(200).padBottom(20).row();

        // Shape selector
        Label shapeLabel = new Label("Shape:", skin);
        root.add(shapeLabel).padBottom(5).row();

        SelectBox<String> shapeSelect = new SelectBox<>(skin);
        shapeSelect.setItems("Cube", "Sphere", "Eyeball");
        root.add(shapeSelect).width(200).padBottom(30).row();

        // Solo button
        TextButton soloButton = new TextButton("SOLO PLAY", skin);
        soloButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int[] size = parseMazeSize(sizeSelect.getSelected());
                PlayerModelFactory.Shape shape = PlayerModelFactory.Shape.fromIndex(
                        shapeSelect.getSelectedIndex());
                game.setScreen(new PlayScreen(game, size[0], size[1],
                        System.currentTimeMillis(), true, shape));
            }
        });
        root.add(soloButton).width(250).height(50).padBottom(15).row();

        // Player name field
        Label nameLabel = new Label("Name:", skin);
        root.add(nameLabel).padBottom(5).row();

        TextField nameField = new TextField("Player", skin);
        root.add(nameField).width(200).padBottom(20).row();

        // Host button
        TextButton hostButton = new TextButton("HOST GAME", skin);
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int[] size = parseMazeSize(sizeSelect.getSelected());
                NetworkManager net = new NetworkManager();
                String ip = net.startHost(nameField.getText(), shapeSelect.getSelectedIndex());
                if (ip != null) {
                    game.setScreen(new LobbyScreen(game, net, size[0], size[1]));
                }
            }
        });
        root.add(hostButton).width(250).height(50).padBottom(15).row();

        // Join section: IP field + button
        Table joinTable = new Table();
        TextField ipField = new TextField("192.168.1.", skin);
        joinTable.add(ipField).width(180).padRight(10);

        TextButton joinButton = new TextButton("JOIN", skin);
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int[] size = parseMazeSize(sizeSelect.getSelected());
                NetworkManager net = new NetworkManager();
                net.setOnJoinResponse(resp -> {
                    if (resp.accepted) {
                        game.setScreen(new LobbyScreen(game, net, size[0], size[1]));
                    }
                });
                net.connectToHost(ipField.getText(),
                        nameField.getText(), shapeSelect.getSelectedIndex());
            }
        });
        joinTable.add(joinButton).width(80).height(50);

        // QR scan button (only on platforms that support it)
        if (game.getPlatformServices().isQrScanAvailable()) {
            TextButton scanButton = new TextButton("SCAN QR", skin);
            scanButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.getPlatformServices().scanQrCode(scannedText -> {
                        if (scannedText != null) {
                            // Parse mazer://IP:PORT format
                            String ip = scannedText
                                    .replace("mazer://", "")
                                    .split(":")[0];
                            ipField.setText(ip);

                            // Auto-connect
                            int[] size = parseMazeSize(sizeSelect.getSelected());
                            NetworkManager net = new NetworkManager();
                            net.setOnJoinResponse(resp -> {
                                if (resp.accepted) {
                                    game.setScreen(new LobbyScreen(game, net, size[0], size[1]));
                                }
                            });
                            net.connectToHost(ip,
                                    nameField.getText(), shapeSelect.getSelectedIndex());
                        }
                    });
                }
            });
            joinTable.add(scanButton).width(100).height(50).padLeft(10);
        }

        root.add(joinTable).padBottom(15).row();
    }

    private int[] parseMazeSize(String sizeStr) {
        String[] parts = sizeStr.split("x");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
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

    /**
     * Creates a minimal programmatic skin since we have no asset files.
     */
    private Skin createMinimalSkin() {
        Skin s = new Skin();

        // Font
        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        s.add("default-font", font);

        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(3f);
        s.add("title-font", titleFont);

        // Pixmap-based drawables for buttons and select boxes
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(0, 0.3f, 0, 1));
        pixmap.fill();
        s.add("button-up", new com.badlogic.gdx.graphics.Texture(pixmap));

        pixmap.setColor(new Color(0, 0.5f, 0, 1));
        pixmap.fill();
        s.add("button-down", new com.badlogic.gdx.graphics.Texture(pixmap));

        pixmap.setColor(new Color(0.1f, 0.1f, 0.1f, 1));
        pixmap.fill();
        s.add("button-disabled", new com.badlogic.gdx.graphics.Texture(pixmap));

        pixmap.setColor(new Color(0, 0.2f, 0, 1));
        pixmap.fill();
        s.add("select-bg", new com.badlogic.gdx.graphics.Texture(pixmap));

        pixmap.setColor(new Color(0, 0.4f, 0, 1));
        pixmap.fill();
        s.add("select-over", new com.badlogic.gdx.graphics.Texture(pixmap));

        pixmap.dispose();

        // Create styles using texture drawables
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.GREEN;
        buttonStyle.up = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(s.get("button-up", com.badlogic.gdx.graphics.Texture.class)));
        buttonStyle.down = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(s.get("button-down", com.badlogic.gdx.graphics.Texture.class)));
        buttonStyle.disabled = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(s.get("button-disabled", com.badlogic.gdx.graphics.Texture.class)));
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

        com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable selectBg =
                new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                        new com.badlogic.gdx.graphics.g2d.TextureRegion(
                                s.get("select-bg", com.badlogic.gdx.graphics.Texture.class)));
        com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable selectOver =
                new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                        new com.badlogic.gdx.graphics.g2d.TextureRegion(
                                s.get("select-over", com.badlogic.gdx.graphics.Texture.class)));

        com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle listStyle =
                new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = Color.GREEN;
        listStyle.selection = selectOver;
        listStyle.background = selectBg;
        s.add("default", listStyle);

        com.badlogic.gdx.graphics.g2d.NinePatch scrollPatch =
                new com.badlogic.gdx.graphics.g2d.NinePatch(
                        s.get("select-bg", com.badlogic.gdx.graphics.Texture.class), 0, 0, 0, 0);
        com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle scrollStyle =
                new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle();
        scrollStyle.background = selectBg;
        s.add("default", scrollStyle);

        SelectBox.SelectBoxStyle selectStyle = new SelectBox.SelectBoxStyle();
        selectStyle.font = font;
        selectStyle.fontColor = Color.GREEN;
        selectStyle.background = selectBg;
        selectStyle.backgroundOver = selectOver;
        selectStyle.scrollStyle = scrollStyle;
        selectStyle.listStyle = listStyle;
        s.add("default", selectStyle);

        // TextField style
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.GREEN;
        textFieldStyle.background = selectBg;
        textFieldStyle.cursor = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(
                        s.get("button-down", com.badlogic.gdx.graphics.Texture.class)));
        textFieldStyle.selection = selectOver;
        s.add("default", textFieldStyle);

        return s;
    }
}
