package com.codeheadsystems.mazer.net;

import com.codeheadsystems.mazer.input.InputState;
import com.codeheadsystems.mazer.net.Protocol.LobbyUpdate;
import com.codeheadsystems.mazer.net.Protocol.PlayerInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Abstraction over host and client networking roles.
 * The game code interacts with this instead of directly with KryoNet.
 */
public class NetworkManager {

    private HostServer hostServer;
    private ClientConnection clientConnection;
    private boolean isHost;
    private int localPlayerId = -1;
    private final Map<Integer, String> playerNames = new HashMap<>();

    // Callbacks set by screens
    private Consumer<LobbyUpdate> onLobbyUpdate;
    private Consumer<Protocol.StartGame> onGameStart;
    private Consumer<Protocol.JoinResponse> onJoinResponse;
    private Runnable onDisconnected;
    private Consumer<Protocol.GameSnapshot> onGameSnapshot;
    private Consumer<Protocol.PlayerHit> onPlayerHit;
    private Consumer<Protocol.PlayerEliminated> onPlayerEliminated;
    private Consumer<Protocol.GameOver> onGameOver;

    public boolean isHost() {
        return isHost;
    }

    public int getLocalPlayerId() {
        return localPlayerId;
    }

    public void setLocalPlayerId(int id) {
        this.localPlayerId = id;
    }

    public void setOnLobbyUpdate(Consumer<LobbyUpdate> callback) {
        this.onLobbyUpdate = callback;
    }

    public void setOnGameStart(Consumer<Protocol.StartGame> callback) {
        this.onGameStart = callback;
    }

    public void setOnJoinResponse(Consumer<Protocol.JoinResponse> callback) {
        this.onJoinResponse = callback;
    }

    public void setOnDisconnected(Runnable callback) {
        this.onDisconnected = callback;
    }

    public void setOnGameSnapshot(Consumer<Protocol.GameSnapshot> callback) {
        this.onGameSnapshot = callback;
    }

    public void setOnPlayerHit(Consumer<Protocol.PlayerHit> callback) {
        this.onPlayerHit = callback;
    }

    public void setOnPlayerEliminated(Consumer<Protocol.PlayerEliminated> callback) {
        this.onPlayerEliminated = callback;
    }

    public void setOnGameOver(Consumer<Protocol.GameOver> callback) {
        this.onGameOver = callback;
    }

    /**
     * Starts hosting a game. Returns the host's IP address for others to connect.
     */
    public String startHost(String playerName, int shapeIndex) {
        isHost = true;
        localPlayerId = 0;
        hostServer = new HostServer(this);
        return hostServer.start(playerName, shapeIndex);
    }

    /**
     * Connects to a host at the given IP address.
     */
    public void connectToHost(String hostIp, String playerName, int shapeIndex) {
        isHost = false;
        clientConnection = new ClientConnection(this);
        clientConnection.connect(hostIp, playerName, shapeIndex);
    }

    /**
     * Toggles the local player's ready state.
     */
    public void toggleReady() {
        if (isHost) {
            hostServer.toggleHostReady();
        } else {
            clientConnection.sendTCP(new Protocol.ReadyToggle());
        }
    }

    /**
     * Host: starts the game if all players are ready.
     */
    public void requestStartGame(int mazeWidth, int mazeHeight) {
        if (isHost && hostServer != null) {
            hostServer.startGame(mazeWidth, mazeHeight);
        }
    }

    /**
     * Returns the connect string for clients to join.
     */
    public String getConnectString() {
        if (hostServer != null) {
            return hostServer.getConnectString();
        }
        return null;
    }

    // --- Internal callbacks from HostServer/ClientConnection ---

    void fireLobbyUpdate(LobbyUpdate update) {
        // Cache player names for later use (e.g., GameOverScreen)
        if (update.players != null) {
            for (PlayerInfo info : update.players) {
                playerNames.put(info.id, info.name);
            }
        }
        if (onLobbyUpdate != null) {
            onLobbyUpdate.accept(update);
        }
    }

    /**
     * Returns the player name for the given ID, or "Player N" as fallback.
     */
    public String getPlayerName(int playerId) {
        return playerNames.getOrDefault(playerId, "Player " + playerId);
    }

    void fireGameStart(Protocol.StartGame msg) {
        if (onGameStart != null) {
            onGameStart.accept(msg);
        }
    }

    void fireJoinResponse(Protocol.JoinResponse msg) {
        if (onJoinResponse != null) {
            onJoinResponse.accept(msg);
        }
    }

    void fireDisconnected() {
        if (onDisconnected != null) {
            onDisconnected.run();
        }
    }

    void fireGameSnapshot(Protocol.GameSnapshot msg) {
        if (onGameSnapshot != null) {
            onGameSnapshot.accept(msg);
        }
    }

    void firePlayerHit(Protocol.PlayerHit msg) {
        if (onPlayerHit != null) {
            onPlayerHit.accept(msg);
        }
    }

    void firePlayerEliminated(Protocol.PlayerEliminated msg) {
        if (onPlayerEliminated != null) {
            onPlayerEliminated.accept(msg);
        }
    }

    void fireGameOver(Protocol.GameOver msg) {
        if (onGameOver != null) {
            onGameOver.accept(msg);
        }
    }

    /**
     * Sends player input to the server. In host mode, forwards directly
     * to the HostServer; in client mode, sends via the ClientConnection.
     */
    public void sendPlayerInput(InputState input) {
        if (isHost && hostServer != null) {
            Protocol.PlayerInput msg = new Protocol.PlayerInput();
            msg.forward = input.moveForward;
            msg.turnAmount = input.turnAmount;
            msg.fire = input.fire;
            hostServer.handleLocalPlayerInput(msg);
        } else if (clientConnection != null) {
            clientConnection.sendPlayerInput(input);
        }
    }

    public HostServer getHostServer() {
        return hostServer;
    }

    public ClientConnection getClientConnection() {
        return clientConnection;
    }

    /**
     * Shuts down all networking.
     */
    public void shutdown() {
        if (hostServer != null) {
            hostServer.stop();
            hostServer = null;
        }
        if (clientConnection != null) {
            clientConnection.disconnect();
            clientConnection = null;
        }
    }
}
