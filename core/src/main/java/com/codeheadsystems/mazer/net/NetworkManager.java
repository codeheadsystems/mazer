package com.codeheadsystems.mazer.net;

import com.codeheadsystems.mazer.net.Protocol.LobbyUpdate;
import com.codeheadsystems.mazer.net.Protocol.PlayerInfo;

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

    // Callbacks set by screens
    private Consumer<LobbyUpdate> onLobbyUpdate;
    private Consumer<Protocol.StartGame> onGameStart;
    private Consumer<Protocol.JoinResponse> onJoinResponse;
    private Runnable onDisconnected;

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
        if (onLobbyUpdate != null) {
            onLobbyUpdate.accept(update);
        }
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
