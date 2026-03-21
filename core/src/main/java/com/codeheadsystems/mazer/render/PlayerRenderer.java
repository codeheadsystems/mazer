package com.codeheadsystems.mazer.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Disposable;
import com.codeheadsystems.mazer.world.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders other players as wireframe shapes at their world positions.
 * Each player's shape model is created on first encounter and cached.
 */
public class PlayerRenderer implements Disposable {

    // Distinct colors for up to 8 players
    private static final Color[] PLAYER_COLORS = {
            new Color(1f, 0.2f, 0.2f, 1f),    // red
            new Color(0.2f, 0.6f, 1f, 1f),     // blue
            new Color(1f, 1f, 0.2f, 1f),        // yellow
            new Color(1f, 0.5f, 0f, 1f),        // orange
            new Color(0.8f, 0.2f, 1f, 1f),      // purple
            new Color(0f, 1f, 1f, 1f),           // cyan
            new Color(1f, 0.5f, 0.7f, 1f),      // pink
            new Color(0.6f, 1f, 0.2f, 1f),      // lime
    };

    private final ModelBatch modelBatch;
    private final Map<Integer, ModelInstance> playerInstances;
    private final Map<Integer, Model> playerModels;

    public PlayerRenderer() {
        this.modelBatch = new ModelBatch();
        this.playerInstances = new HashMap<>();
        this.playerModels = new HashMap<>();
    }

    /**
     * Renders all players in the list except the local player.
     */
    public void render(Camera camera, List<Player> players, int localPlayerId) {
        modelBatch.begin(camera);
        for (Player player : players) {
            if (player.getId() == localPlayerId) continue;
            if (!player.isAlive()) continue;

            ModelInstance instance = getOrCreateInstance(player);
            instance.transform.setToTranslation(
                    player.getX(), MazeRenderer.EYE_HEIGHT, player.getZ());
            // Rotate the model to face the player's direction
            instance.transform.rotate(0, 1, 0,
                    -(float) Math.toDegrees(player.getAngle()));
            modelBatch.render(instance);
        }
        modelBatch.end();
    }

    private ModelInstance getOrCreateInstance(Player player) {
        ModelInstance instance = playerInstances.get(player.getId());
        if (instance == null) {
            Color color = PLAYER_COLORS[player.getId() % PLAYER_COLORS.length];
            Model model = PlayerModelFactory.create(player.getShape(), color);
            instance = new ModelInstance(model);
            playerModels.put(player.getId(), model);
            playerInstances.put(player.getId(), instance);
        }
        return instance;
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        for (Model model : playerModels.values()) {
            model.dispose();
        }
    }
}
