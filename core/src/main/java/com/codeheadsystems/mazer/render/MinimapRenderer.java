package com.codeheadsystems.mazer.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.codeheadsystems.mazer.maze.MazeCell;
import com.codeheadsystems.mazer.maze.MazeGrid;

/**
 * Renders a 2D minimap overlay in the upper-right corner of the screen.
 * Shows the maze layout and the local player's position/direction.
 * Does NOT show other players.
 */
public class MinimapRenderer implements Disposable {

    private static final int MINIMAP_SIZE = 200;  // pixels
    private static final int MINIMAP_PADDING = 10;
    private static final Color BG_COLOR = new Color(0, 0, 0, 0.7f);
    private static final Color WALL_COLOR = new Color(0, 0.6f, 0, 1f);
    private static final Color PLAYER_COLOR = new Color(0, 1f, 0, 1f);

    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera minimapCamera;
    private final MazeGrid maze;

    public MinimapRenderer(MazeGrid maze) {
        this.maze = maze;
        this.shapeRenderer = new ShapeRenderer();
        this.minimapCamera = new OrthographicCamera();

        // Camera shows the entire maze
        float mazeWorldWidth = maze.getWidth() * maze.getCellSize();
        float mazeWorldHeight = maze.getHeight() * maze.getCellSize();
        minimapCamera.setToOrtho(false, mazeWorldWidth, mazeWorldHeight);
        minimapCamera.position.set(mazeWorldWidth / 2f, mazeWorldHeight / 2f, 0);
        minimapCamera.update();
    }

    /**
     * Renders the minimap in the upper-right corner.
     *
     * @param playerX     player world X position
     * @param playerZ     player world Z position (mapped to minimap Y)
     * @param playerAngle player facing angle in radians
     * @param screenWidth current screen width
     * @param screenHeight current screen height
     */
    public void render(float playerX, float playerZ, float playerAngle,
                       int screenWidth, int screenHeight) {
        int mapX = screenWidth - MINIMAP_SIZE - MINIMAP_PADDING;
        int mapY = screenHeight - MINIMAP_SIZE - MINIMAP_PADDING;

        // Set viewport to minimap region
        Gdx.gl.glViewport(mapX, mapY, MINIMAP_SIZE, MINIMAP_SIZE);

        // Enable blending for semi-transparent background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(minimapCamera.combined);

        float mazeW = maze.getWidth() * maze.getCellSize();
        float mazeH = maze.getHeight() * maze.getCellSize();

        // Draw background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BG_COLOR);
        shapeRenderer.rect(0, 0, mazeW, mazeH);
        shapeRenderer.end();

        // Draw walls
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(WALL_COLOR);

        float cs = maze.getCellSize();
        for (int x = 0; x < maze.getWidth(); x++) {
            for (int y = 0; y < maze.getHeight(); y++) {
                MazeCell cell = maze.getCell(x, y);
                float cx = x * cs;
                float cy = y * cs;

                if (cell.wallSouth) {
                    shapeRenderer.line(cx, cy, cx + cs, cy);
                }
                if (cell.wallWest) {
                    shapeRenderer.line(cx, cy, cx, cy + cs);
                }
                if (y == maze.getHeight() - 1 && cell.wallNorth) {
                    shapeRenderer.line(cx, cy + cs, cx + cs, cy + cs);
                }
                if (x == maze.getWidth() - 1 && cell.wallEast) {
                    shapeRenderer.line(cx + cs, cy, cx + cs, cy + cs);
                }
            }
        }
        shapeRenderer.end();

        // Draw player as a small triangle pointing in their direction
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(PLAYER_COLOR);

        float triSize = cs * 0.4f;
        float tipX = playerX + MathUtils.cos(playerAngle) * triSize;
        float tipZ = playerZ + MathUtils.sin(playerAngle) * triSize;
        float leftX = playerX + MathUtils.cos(playerAngle + 2.5f) * triSize * 0.6f;
        float leftZ = playerZ + MathUtils.sin(playerAngle + 2.5f) * triSize * 0.6f;
        float rightX = playerX + MathUtils.cos(playerAngle - 2.5f) * triSize * 0.6f;
        float rightZ = playerZ + MathUtils.sin(playerAngle - 2.5f) * triSize * 0.6f;

        shapeRenderer.triangle(tipX, tipZ, leftX, leftZ, rightX, rightZ);
        shapeRenderer.end();

        // Restore full viewport
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
