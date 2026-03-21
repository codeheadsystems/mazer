package com.codeheadsystems.mazer.world;

import com.badlogic.gdx.math.MathUtils;
import com.codeheadsystems.mazer.maze.MazeGrid;

/**
 * A bullet projectile that travels in a straight line until hitting a wall or player.
 */
public class Bullet {

    public static final float SPEED = Player.SPEED;  // same speed as player
    public static final float RADIUS = 0.1f;
    public static final float MAX_LIFETIME = 10f;    // seconds
    public static final float FIRE_COOLDOWN = 1.0f;  // seconds between shots

    private float x;
    private float z;
    private final float dirX;
    private final float dirZ;
    private final int ownerPlayerId;
    private boolean alive = true;
    private float lifetime = 0f;

    public Bullet(float x, float z, float angle, int ownerPlayerId) {
        this.x = x;
        this.z = z;
        this.dirX = MathUtils.cos(angle);
        this.dirZ = MathUtils.sin(angle);
        this.ownerPlayerId = ownerPlayerId;
    }

    /**
     * Moves the bullet forward. Returns false if the bullet should be removed.
     */
    public boolean update(float delta, MazeGrid maze) {
        if (!alive) return false;

        lifetime += delta;
        if (lifetime > MAX_LIFETIME) {
            alive = false;
            return false;
        }

        x += dirX * SPEED * delta;
        z += dirZ * SPEED * delta;

        // Wall collision
        if (maze.collidesWithWall(x, z, RADIUS)) {
            alive = false;
            return false;
        }

        return true;
    }

    public float getX() { return x; }
    public float getZ() { return z; }
    public int getOwnerPlayerId() { return ownerPlayerId; }
    public boolean isAlive() { return alive; }

    public void kill() {
        alive = false;
    }
}
