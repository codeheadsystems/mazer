package com.codeheadsystems.mazer.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.codeheadsystems.mazer.input.InputState;
import com.codeheadsystems.mazer.maze.MazeCell;
import com.codeheadsystems.mazer.maze.MazeGrid;

/**
 * Player entity with position, facing direction, and movement logic.
 */
public class Player {

    public static final float SPEED = 4.0f;           // units/sec (= 1 cell/sec at cellSize=4)
    public static final float TURN_SPEED = 3.0f;      // radians/sec at full turn
    public static final float COLLISION_RADIUS = 1.8f;
    private static final float CENTERING_STRENGTH = 8.0f; // how fast centering pulls (units/sec)

    private float x;
    private float z;
    private float angle; // radians, 0 = +X axis
    private int score;
    private boolean alive;
    private int id;

    public Player(int id, float x, float z, float angle) {
        this.id = id;
        this.x = x;
        this.z = z;
        this.angle = angle;
        this.score = 5;
        this.alive = true;
    }

    /**
     * Updates player position and facing based on input.
     * Uses axis-separated collision for wall sliding, plus corridor centering
     * to guide the player through doorways smoothly.
     */
    public void update(float delta, InputState input, MazeGrid maze) {
        if (!alive) return;

        // Steering
        angle += input.turnAmount * TURN_SPEED * delta;

        // Normalize angle to [0, 2*PI)
        angle = angle % (MathUtils.PI2);
        if (angle < 0) angle += MathUtils.PI2;

        // Movement with wall sliding
        if (input.moveForward) {
            float dx = MathUtils.cos(angle) * SPEED * delta;
            float dz = MathUtils.sin(angle) * SPEED * delta;

            // Try X movement independently
            if (!maze.collidesWithWall(x + dx, z, COLLISION_RADIUS)) {
                x += dx;
            }
            // Try Z movement independently
            if (!maze.collidesWithWall(x, z + dz, COLLISION_RADIUS)) {
                z += dz;
            }

            // Apply corridor centering to guide player through openings
            applyCorridorCentering(delta, maze);
        }
    }

    /**
     * Gently pulls the player toward the center axis of the corridor they're in.
     * This ensures the player lines up with doorways between cells for smooth transitions.
     *
     * For each axis, if the player is in a corridor (openings on opposite sides or
     * approaching an opening), pull toward the cell center on the perpendicular axis.
     */
    private void applyCorridorCentering(float delta, MazeGrid maze) {
        float cs = maze.getCellSize();
        int cx = maze.worldToCellX(x);
        int cy = maze.worldToCellY(z);
        MazeCell cell = maze.getCell(cx, cy);
        if (cell == null) return;

        float cellCenterX = cx * cs + cs / 2f;
        float cellCenterZ = cy * cs + cs / 2f;

        // Determine primary movement direction from facing angle
        boolean movingHorizontal = Math.abs(MathUtils.cos(angle)) > Math.abs(MathUtils.sin(angle));

        if (movingHorizontal) {
            // Moving mostly east/west: center on Z axis (pull toward cell center Z)
            // This aligns the player with east/west openings
            float dz = cellCenterZ - z;
            float pull = Math.min(Math.abs(dz), CENTERING_STRENGTH * delta);
            float newZ = z + Math.signum(dz) * pull;
            if (!maze.collidesWithWall(x, newZ, COLLISION_RADIUS)) {
                z = newZ;
            }
        } else {
            // Moving mostly north/south: center on X axis (pull toward cell center X)
            // This aligns the player with north/south openings
            float dx = cellCenterX - x;
            float pull = Math.min(Math.abs(dx), CENTERING_STRENGTH * delta);
            float newX = x + Math.signum(dx) * pull;
            if (!maze.collidesWithWall(newX, z, COLLISION_RADIUS)) {
                x = newX;
            }
        }
    }

    public float getX() { return x; }
    public float getZ() { return z; }
    public float getAngle() { return angle; }
    public int getScore() { return score; }
    public boolean isAlive() { return alive; }
    public int getId() { return id; }

    public void setPosition(float x, float z) {
        this.x = x;
        this.z = z;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public void hit() {
        score--;
        if (score <= 0) {
            score = 0;
            alive = false;
        }
    }
}
