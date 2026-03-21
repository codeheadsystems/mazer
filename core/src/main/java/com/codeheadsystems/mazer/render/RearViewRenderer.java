package com.codeheadsystems.mazer.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/**
 * Renders a rear-view mirror by drawing the 3D scene with a backward-facing
 * camera into a FrameBuffer, then displaying it as a 2D texture on the HUD.
 */
public class RearViewRenderer implements Disposable {

    private static final int FBO_WIDTH = 200;
    private static final int FBO_HEIGHT = 112;
    private static final int MIRROR_PADDING = 10;
    private static final int FRAME_THICKNESS = 3;
    private static final Color FRAME_COLOR = new Color(0f, 0.8f, 0f, 1f);
    private static final Color LABEL_COLOR = new Color(0f, 0.6f, 0f, 1f);

    private final FrameBuffer fbo;
    private final PerspectiveCamera rearCamera;
    private final SpriteBatch spriteBatch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout;
    private final TextureRegion fboRegion;

    public RearViewRenderer() {
        this.fbo = new FrameBuffer(Pixmap.Format.RGB888, FBO_WIDTH, FBO_HEIGHT, true);
        this.rearCamera = new PerspectiveCamera(75, FBO_WIDTH, FBO_HEIGHT);
        rearCamera.near = 0.1f;
        rearCamera.far = 80f;
        this.spriteBatch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.font = new BitmapFont();
        font.setColor(LABEL_COLOR);
        this.glyphLayout = new GlyphLayout();

        // FBO textures are Y-flipped, so use a TextureRegion to flip it back
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);
    }

    /**
     * Renders the rear-view mirror.
     *
     * @param mazeRenderer the maze renderer to use for drawing the scene
     * @param playerX      player world X
     * @param playerZ      player world Z
     * @param playerAngle  player facing angle (radians). Rear = angle + PI
     * @param screenWidth  current screen width
     * @param screenHeight current screen height
     */
    public void render(MazeRenderer mazeRenderer, float playerX, float playerZ,
                       float playerAngle, int screenWidth, int screenHeight) {
        // Set up rear camera: same position, opposite direction
        rearCamera.position.set(playerX, MazeRenderer.EYE_HEIGHT, playerZ);
        float rearAngle = playerAngle + MathUtils.PI;
        rearCamera.direction.set(MathUtils.cos(rearAngle), 0, MathUtils.sin(rearAngle));
        rearCamera.up.set(0, 1, 0);
        rearCamera.update();

        // Render scene into FBO
        fbo.begin();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        mazeRenderer.render(rearCamera);
        fbo.end();

        // Restore the main viewport
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);

        // Draw the FBO texture as a mirror at the top-center of the screen
        float mirrorWidth = FBO_WIDTH;
        float mirrorHeight = FBO_HEIGHT;
        float mirrorX = (screenWidth - mirrorWidth) / 2f;
        float mirrorY = screenHeight - mirrorHeight - MIRROR_PADDING;

        // Draw frame border
        float ft = FRAME_THICKNESS;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(FRAME_COLOR);
        // Top
        shapeRenderer.rect(mirrorX - ft, mirrorY + mirrorHeight, mirrorWidth + ft * 2, ft);
        // Bottom
        shapeRenderer.rect(mirrorX - ft, mirrorY - ft, mirrorWidth + ft * 2, ft);
        // Left
        shapeRenderer.rect(mirrorX - ft, mirrorY - ft, ft, mirrorHeight + ft * 2);
        // Right
        shapeRenderer.rect(mirrorX + mirrorWidth, mirrorY - ft, ft, mirrorHeight + ft * 2);
        shapeRenderer.end();

        // Draw mirror image
        fboRegion.setTexture(fbo.getColorBufferTexture());

        spriteBatch.begin();
        spriteBatch.draw(fboRegion, mirrorX, mirrorY, mirrorWidth, mirrorHeight);

        // Draw "REAR VIEW" label centered below the frame
        glyphLayout.setText(font, "REAR VIEW");
        float labelX = mirrorX + (mirrorWidth - glyphLayout.width) / 2f;
        float labelY = mirrorY - ft - 2f;
        font.draw(spriteBatch, "REAR VIEW", labelX, labelY);
        spriteBatch.end();
    }

    @Override
    public void dispose() {
        fbo.dispose();
        spriteBatch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
