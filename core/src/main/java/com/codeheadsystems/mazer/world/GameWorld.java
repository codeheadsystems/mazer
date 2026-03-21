package com.codeheadsystems.mazer.world;

import com.badlogic.gdx.math.Vector2;
import com.codeheadsystems.mazer.input.InputState;
import com.codeheadsystems.mazer.maze.MazeGrid;
import com.codeheadsystems.mazer.render.PlayerModelFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Owns all game entities and ticks the simulation each frame.
 * Handles bullet movement, wall collision, and bullet-player collision.
 */
public class GameWorld {

    private final MazeGrid maze;
    private final List<Player> players;
    private final List<Bullet> bullets;
    private float fireCooldown = 0f;

    public GameWorld(MazeGrid maze) {
        this.maze = maze;
        this.players = new ArrayList<>();
        this.bullets = new ArrayList<>();
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public MazeGrid getMaze() {
        return maze;
    }

    /**
     * Spawns dummy target players at random positions in the maze for solo play.
     * Each gets a different shape. Avoids placing them in the same cell as the local player.
     */
    public void spawnDummyTargets(int count, int avoidCellX, int avoidCellY, long seed) {
        Random random = new Random(seed);
        PlayerModelFactory.Shape[] shapes = PlayerModelFactory.Shape.values();
        int nextId = players.size();

        for (int i = 0; i < count; i++) {
            int cx, cy;
            do {
                cx = random.nextInt(maze.getWidth());
                cy = random.nextInt(maze.getHeight());
            } while (cx == avoidCellX && cy == avoidCellY);

            Vector2 pos = maze.cellToWorld(cx, cy);
            PlayerModelFactory.Shape shape = shapes[(i + 1) % shapes.length]; // +1 to skip local player's shape
            Player dummy = new Player(nextId++, pos.x, pos.y, random.nextFloat() * 6.28f, shape);
            players.add(dummy);
        }
    }

    /**
     * Updates the world simulation for one frame.
     *
     * @param delta       frame delta time
     * @param localPlayer the local player (for firing)
     * @param input       local player's input state
     */
    public void update(float delta, Player localPlayer, InputState input) {
        // Update local player movement
        localPlayer.update(delta, input, maze);

        // Fire cooldown
        fireCooldown = Math.max(0, fireCooldown - delta);

        // Handle firing
        if (input.fire && fireCooldown <= 0 && localPlayer.isAlive()) {
            bullets.add(new Bullet(
                    localPlayer.getX(), localPlayer.getZ(),
                    localPlayer.getAngle(), localPlayer.getId()));
            fireCooldown = Bullet.FIRE_COOLDOWN;
        }

        // Update bullets
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            if (!bullet.update(delta, maze)) {
                bulletIter.remove();
                continue;
            }

            // Check bullet-player collision
            for (Player player : players) {
                if (!player.isAlive()) continue;
                if (player.getId() == bullet.getOwnerPlayerId()) continue;

                float dx = bullet.getX() - player.getX();
                float dz = bullet.getZ() - player.getZ();
                float dist = dx * dx + dz * dz;
                float hitRadius = Bullet.RADIUS + Player.COLLISION_RADIUS * 0.5f;

                if (dist < hitRadius * hitRadius) {
                    player.hit();
                    bullet.kill();
                    break;
                }
            }

            // Remove if killed by player collision
            if (!bullet.isAlive()) {
                bulletIter.remove();
            }
        }
    }
}
