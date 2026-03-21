package com.codeheadsystems.mazer.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Disposable;
import com.codeheadsystems.mazer.world.Bullet;

import java.util.List;

/**
 * Renders bullets as small bright green wireframe diamonds at eye height.
 */
public class BulletRenderer implements Disposable {

    private static final Color BULLET_COLOR = new Color(0.5f, 1f, 0.5f, 1f);
    private static final float SIZE = 0.2f;

    private final ModelBatch modelBatch;
    private final Model bulletModel;
    private final ModelInstance bulletInstance;

    public BulletRenderer() {
        this.modelBatch = new ModelBatch();
        this.bulletModel = buildBulletModel();
        this.bulletInstance = new ModelInstance(bulletModel);
    }

    public void render(Camera camera, List<Bullet> bullets) {
        if (bullets.isEmpty()) return;

        modelBatch.begin(camera);
        for (Bullet b : bullets) {
            if (!b.isAlive()) continue;
            bulletInstance.transform.setToTranslation(b.getX(), MazeRenderer.EYE_HEIGHT, b.getZ());
            modelBatch.render(bulletInstance);
        }
        modelBatch.end();
    }

    private Model buildBulletModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();

        MeshPartBuilder builder = mb.part("bullet",
                GL20.GL_LINES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked,
                new Material());

        builder.setColor(BULLET_COLOR);

        // Small diamond/octahedron shape
        float s = SIZE;
        // Horizontal diamond
        builder.line(-s, 0, 0, 0, 0, s);
        builder.line(0, 0, s, s, 0, 0);
        builder.line(s, 0, 0, 0, 0, -s);
        builder.line(0, 0, -s, -s, 0, 0);
        // Top and bottom points
        builder.line(-s, 0, 0, 0, s, 0);
        builder.line(0, 0, s, 0, s, 0);
        builder.line(s, 0, 0, 0, s, 0);
        builder.line(0, 0, -s, 0, s, 0);
        builder.line(-s, 0, 0, 0, -s, 0);
        builder.line(0, 0, s, 0, -s, 0);
        builder.line(s, 0, 0, 0, -s, 0);
        builder.line(0, 0, -s, 0, -s, 0);

        return mb.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        bulletModel.dispose();
    }
}
