package com.codeheadsystems.mazer.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.codeheadsystems.mazer.maze.MazeCell;
import com.codeheadsystems.mazer.maze.MazeGrid;

/**
 * Renders the maze as a first-person 3D wireframe using GL_LINES.
 * Green-on-black retro aesthetic.
 */
public class MazeRenderer implements Disposable {

    public static final float WALL_HEIGHT = 3.0f;
    public static final float EYE_HEIGHT = 1.5f;

    private static final Color WALL_COLOR = new Color(0.0f, 1.0f, 0.0f, 1.0f);       // bright green
    private static final Color FLOOR_COLOR = new Color(0.0f, 0.3f, 0.0f, 1.0f);      // dark green
    private static final Color CEILING_COLOR = new Color(0.0f, 0.2f, 0.0f, 1.0f);    // darker green
    private static final Color WALL_FILL_COLOR = new Color(0.08f, 0.18f, 0.08f, 1.0f);    // dark greenish wall fill
    private static final Color FLOOR_FILL_COLOR = new Color(0.0f, 0.12f, 0.0f, 1.0f);   // dark green floor fill
    private static final Color CEILING_FILL_COLOR = new Color(0.0f, 0.08f, 0.0f, 1.0f); // dark green ceiling fill

    private final ModelBatch modelBatch;
    private final Model mazeModel;
    private final ModelInstance mazeInstance;
    private final PerspectiveCamera camera;

    public MazeRenderer(MazeGrid maze) {
        this.modelBatch = new ModelBatch();
        this.camera = new PerspectiveCamera(75,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 80f;

        this.mazeModel = buildMazeModel(maze);
        this.mazeInstance = new ModelInstance(mazeModel);
    }

    public PerspectiveCamera getCamera() {
        return camera;
    }

    /**
     * Updates camera to match a player's position and facing angle.
     *
     * @param x     world X position
     * @param z     world Z position
     * @param angle facing angle in radians (0 = +X axis)
     */
    public void updateCamera(float x, float z, float angle) {
        camera.position.set(x, EYE_HEIGHT, z);
        camera.direction.set(MathUtils.cos(angle), 0, MathUtils.sin(angle));
        camera.up.set(0, 1, 0);
        camera.update();
    }

    /**
     * Renders the maze using the given camera. Useful for rear-view mirror.
     * Uses polygon offset so wireframe lines render cleanly on top of fill quads.
     */
    public void render(Camera cam) {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
        Gdx.gl.glPolygonOffset(1f, 1f);

        modelBatch.begin(cam);
        modelBatch.render(mazeInstance);
        modelBatch.end();

        Gdx.gl.glDisable(GL20.GL_POLYGON_OFFSET_FILL);
    }

    /**
     * Renders the maze using the internal camera.
     */
    public void render() {
        render(camera);
    }

    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    private Model buildMazeModel(MazeGrid maze) {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        int width = maze.getWidth();
        int height = maze.getHeight();
        float cs = maze.getCellSize();
        float h = WALL_HEIGHT;

        // --- Solid fill quads (rendered first, behind wireframe lines) ---

        // Wall fill quads - render both sides of each wall
        MeshPartBuilder wallFill = modelBuilder.part("wall_fill",
                GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked,
                new Material());

        wallFill.setColor(WALL_FILL_COLOR);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                MazeCell cell = maze.getCell(x, y);
                float cx = x * cs;
                float cz = y * cs;

                if (cell.wallSouth) {
                    fillWallQuad(wallFill, cx, cz, cx + cs, cz, h);
                }
                if (cell.wallWest) {
                    fillWallQuad(wallFill, cx, cz, cx, cz + cs, h);
                }
                if (y == height - 1 && cell.wallNorth) {
                    fillWallQuad(wallFill, cx, cz + cs, cx + cs, cz + cs, h);
                }
                if (x == width - 1 && cell.wallEast) {
                    fillWallQuad(wallFill, cx + cs, cz, cx + cs, cz + cs, h);
                }
            }
        }

        // Floor fill - one quad per cell
        MeshPartBuilder floorFill = modelBuilder.part("floor_fill",
                GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked,
                new Material());

        floorFill.setColor(FLOOR_FILL_COLOR);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float cx = x * cs;
                float cz = y * cs;
                fillHorizontalQuad(floorFill, cx, cz, cx + cs, cz + cs, 0f, FLOOR_FILL_COLOR);
            }
        }

        // Ceiling fill - one quad per cell
        MeshPartBuilder ceilingFill = modelBuilder.part("ceiling_fill",
                GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked,
                new Material());

        ceilingFill.setColor(CEILING_FILL_COLOR);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float cx = x * cs;
                float cz = y * cs;
                fillHorizontalQuad(ceilingFill, cx, cz, cx + cs, cz + cs, h, CEILING_FILL_COLOR);
            }
        }

        // --- Wireframe lines (rendered on top via polygon offset) ---

        // Wall wireframe lines
        MeshPartBuilder wallBuilder = modelBuilder.part("walls",
                GL20.GL_LINES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked,
                new Material());

        wallBuilder.setColor(WALL_COLOR);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                MazeCell cell = maze.getCell(x, y);
                float cx = x * cs;
                float cz = y * cs;

                if (cell.wallSouth) {
                    drawWallRect(wallBuilder, cx, cz, cx + cs, cz, h);
                }
                if (cell.wallWest) {
                    drawWallRect(wallBuilder, cx, cz, cx, cz + cs, h);
                }
                if (y == height - 1 && cell.wallNorth) {
                    drawWallRect(wallBuilder, cx, cz + cs, cx + cs, cz + cs, h);
                }
                if (x == width - 1 && cell.wallEast) {
                    drawWallRect(wallBuilder, cx + cs, cz, cx + cs, cz + cs, h);
                }
            }
        }

        // Floor grid lines
        MeshPartBuilder floorBuilder = modelBuilder.part("floor",
                GL20.GL_LINES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked,
                new Material());

        floorBuilder.setColor(FLOOR_COLOR);

        for (int x = 0; x <= width; x++) {
            floorBuilder.line(x * cs, 0, 0, x * cs, 0, height * cs);
        }
        for (int y = 0; y <= height; y++) {
            floorBuilder.line(0, 0, y * cs, width * cs, 0, y * cs);
        }

        // Ceiling grid lines
        MeshPartBuilder ceilingBuilder = modelBuilder.part("ceiling",
                GL20.GL_LINES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked,
                new Material());

        ceilingBuilder.setColor(CEILING_COLOR);

        for (int x = 0; x <= width; x++) {
            ceilingBuilder.line(x * cs, h, 0, x * cs, h, height * cs);
        }
        for (int y = 0; y <= height; y++) {
            ceilingBuilder.line(0, h, y * cs, width * cs, h, y * cs);
        }

        return modelBuilder.end();
    }

    /**
     * Fills a wall as two triangles (a quad) between two base points up to height h.
     * Emits both front and back faces so the wall is visible from either side.
     */
    private void fillWallQuad(MeshPartBuilder builder,
                               float x1, float z1, float x2, float z2, float h) {
        // Front face (CCW winding)
        builder.triangle(
                new MeshPartBuilder.VertexInfo().setPos(x1, 0, z1).setCol(WALL_FILL_COLOR),
                new MeshPartBuilder.VertexInfo().setPos(x2, 0, z2).setCol(WALL_FILL_COLOR),
                new MeshPartBuilder.VertexInfo().setPos(x2, h, z2).setCol(WALL_FILL_COLOR));
        builder.triangle(
                new MeshPartBuilder.VertexInfo().setPos(x1, 0, z1).setCol(WALL_FILL_COLOR),
                new MeshPartBuilder.VertexInfo().setPos(x2, h, z2).setCol(WALL_FILL_COLOR),
                new MeshPartBuilder.VertexInfo().setPos(x1, h, z1).setCol(WALL_FILL_COLOR));
        // Back face (reverse winding)
        builder.triangle(
                new MeshPartBuilder.VertexInfo().setPos(x2, 0, z2).setCol(WALL_FILL_COLOR),
                new MeshPartBuilder.VertexInfo().setPos(x1, 0, z1).setCol(WALL_FILL_COLOR),
                new MeshPartBuilder.VertexInfo().setPos(x1, h, z1).setCol(WALL_FILL_COLOR));
        builder.triangle(
                new MeshPartBuilder.VertexInfo().setPos(x2, 0, z2).setCol(WALL_FILL_COLOR),
                new MeshPartBuilder.VertexInfo().setPos(x1, h, z1).setCol(WALL_FILL_COLOR),
                new MeshPartBuilder.VertexInfo().setPos(x2, h, z2).setCol(WALL_FILL_COLOR));
    }

    /**
     * Fills a horizontal quad (floor or ceiling) defined by two corners on the XZ plane at height y.
     */
    private void fillHorizontalQuad(MeshPartBuilder builder,
                                     float x1, float z1, float x2, float z2, float y, Color color) {
        builder.triangle(
                new MeshPartBuilder.VertexInfo().setPos(x1, y, z1).setCol(color),
                new MeshPartBuilder.VertexInfo().setPos(x2, y, z1).setCol(color),
                new MeshPartBuilder.VertexInfo().setPos(x2, y, z2).setCol(color));
        builder.triangle(
                new MeshPartBuilder.VertexInfo().setPos(x1, y, z1).setCol(color),
                new MeshPartBuilder.VertexInfo().setPos(x2, y, z2).setCol(color),
                new MeshPartBuilder.VertexInfo().setPos(x1, y, z2).setCol(color));
    }

    /**
     * Draws a wall rectangle as 4 line segments between two base points up to height h.
     * The two base points define the bottom edge of the wall on the XZ plane (Y=0).
     */
    private void drawWallRect(MeshPartBuilder builder,
                               float x1, float z1, float x2, float z2, float h) {
        // Bottom edge
        builder.line(x1, 0, z1, x2, 0, z2);
        // Top edge
        builder.line(x1, h, z1, x2, h, z2);
        // Left vertical
        builder.line(x1, 0, z1, x1, h, z1);
        // Right vertical
        builder.line(x2, 0, z2, x2, h, z2);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        mazeModel.dispose();
    }
}
