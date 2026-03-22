package com.codeheadsystems.mazer.net;

import com.esotericsoftware.kryo.Kryo;

/**
 * Network protocol message classes for KryoNet serialization.
 * All message classes must be simple POJOs with a no-arg constructor.
 */
public class Protocol {

    public static final int TCP_PORT = 54555;
    public static final int UDP_PORT = 54556;

    /**
     * Registers all protocol classes with Kryo. Must be called identically
     * on both server and client for serialization to work.
     */
    public static void register(Kryo kryo) {
        kryo.register(JoinRequest.class);
        kryo.register(JoinResponse.class);
        kryo.register(LobbyUpdate.class);
        kryo.register(PlayerInfo.class);
        kryo.register(PlayerInfo[].class);
        kryo.register(ReadyToggle.class);
        kryo.register(StartGame.class);
        kryo.register(float[].class);
        kryo.register(int[].class);
        kryo.register(PlayerInput.class);
        kryo.register(GameSnapshot.class);
        kryo.register(PlayerSnapshot.class);
        kryo.register(PlayerSnapshot[].class);
        kryo.register(BulletSnapshot.class);
        kryo.register(BulletSnapshot[].class);
        kryo.register(PlayerHit.class);
        kryo.register(PlayerEliminated.class);
        kryo.register(GameOver.class);
    }

    // --- Lobby phase messages ---

    /** Client -> Server: request to join the game. */
    public static class JoinRequest {
        public String playerName;
        public int shapeIndex;
    }

    /** Server -> Client: response to join request. */
    public static class JoinResponse {
        public int playerId;
        public boolean accepted;
        public String reason;
    }

    /** Server -> All clients: current lobby state. */
    public static class LobbyUpdate {
        public PlayerInfo[] players;
    }

    /** Info about a single player in the lobby. */
    public static class PlayerInfo {
        public int id;
        public String name;
        public int shapeIndex;
        public boolean ready;
    }

    /** Client -> Server: toggle ready state. */
    public static class ReadyToggle {
    }

    /** Server -> All clients: game is starting. */
    public static class StartGame {
        public long mazeSeed;
        public int mazeWidth;
        public int mazeHeight;
        public int[] playerIds;
        public int[] shapeIndices;
        public float[] spawnX;
        public float[] spawnZ;
        public float[] spawnAngle;
    }

    // --- Gameplay phase messages ---

    /** Client -> Server: player input for this frame (UDP). */
    public static class PlayerInput {
        public boolean forward;
        public float turnAmount;
        public boolean fire;
    }

    /** Server -> All clients: authoritative game state snapshot (UDP). */
    public static class GameSnapshot {
        public long tick;
        public PlayerSnapshot[] players;
        public BulletSnapshot[] bullets;
    }

    /** Snapshot of a single player's state. */
    public static class PlayerSnapshot {
        public int id;
        public float x;
        public float z;
        public float angle;
        public int score;
        public boolean alive;
    }

    /** Snapshot of a single bullet. */
    public static class BulletSnapshot {
        public float x;
        public float z;
        public float dirX;
        public float dirZ;
        public int ownerId;
    }

    /** Server -> All clients: a player was hit (TCP reliable). */
    public static class PlayerHit {
        public int hitPlayerId;
        public int shooterPlayerId;
        public int newScore;
    }

    /** Server -> All clients: a player was eliminated (TCP reliable). */
    public static class PlayerEliminated {
        public int playerId;
    }

    /** Server -> All clients: game over (TCP reliable). */
    public static class GameOver {
        public int winnerPlayerId;
    }
}
