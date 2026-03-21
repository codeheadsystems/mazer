package com.codeheadsystems.mazer.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.codeheadsystems.mazer.input.InputState;
import com.codeheadsystems.mazer.maze.MazeGrid;

/**
 * Player entity with position, facing direction, and movement logic.
 */
public class Player {

    public static final float SPEED = 4.0f;           // units/sec (= 1 cell/sec at cellSize=4)
    public static final float TURN_SPEED = 3.0f;      // radians/sec at full turn
    public static final float COLLISION_RADIUS = 0.3f;

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
     * Uses axis-separated collision for wall sliding.
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
