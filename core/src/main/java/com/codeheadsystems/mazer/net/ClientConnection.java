package com.codeheadsystems.mazer.net;

import com.badlogic.gdx.Gdx;
import com.codeheadsystems.mazer.input.InputState;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;

/**
 * Client-side networking. Connects to a host server via KryoNet.
 */
public class ClientConnection {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final long MIN_INPUT_INTERVAL_NS = 1_000_000_000L / 30; // 30Hz throttle

    private final NetworkManager networkManager;
    private Client client;
    private long lastInputSendTimeNs;

    public ClientConnection(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /**
     * Connects to the host and sends a join request.
     */
    public void connect(String hostIp, String playerName, int shapeIndex) {
        client = new Client(16384, 8192);
        Protocol.register(client.getKryo());

        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                handleMessage(object);
            }

            @Override
            public void disconnected(Connection connection) {
                Gdx.app.postRunnable(() -> networkManager.fireDisconnected());
            }
        });

        client.start();

        // Connect in a background thread to avoid blocking the render thread
        new Thread(() -> {
            try {
                client.connect(CONNECT_TIMEOUT_MS, hostIp, Protocol.TCP_PORT, Protocol.UDP_PORT);

                // Send join request
                Protocol.JoinRequest req = new Protocol.JoinRequest();
                req.playerName = playerName;
                req.shapeIndex = shapeIndex;
                client.sendTCP(req);
            } catch (IOException e) {
                Gdx.app.error("ClientConnection", "Failed to connect to " + hostIp, e);
                Gdx.app.postRunnable(() -> networkManager.fireDisconnected());
            }
        }, "client-connect").start();
    }

    public void sendTCP(Object message) {
        if (client != null && client.isConnected()) {
            client.sendTCP(message);
        }
    }

    public void sendUDP(Object message) {
        if (client != null && client.isConnected()) {
            client.sendUDP(message);
        }
    }

    public void disconnect() {
        if (client != null) {
            client.stop();
            client.close();
        }
    }

    /**
     * Sends player input via UDP, throttled to 30Hz max.
     */
    public void sendPlayerInput(InputState input) {
        long now = System.nanoTime();
        if (now - lastInputSendTimeNs < MIN_INPUT_INTERVAL_NS) {
            return;
        }
        lastInputSendTimeNs = now;

        Protocol.PlayerInput msg = new Protocol.PlayerInput();
        msg.forward = input.moveForward;
        msg.turnAmount = input.turnAmount;
        msg.fire = input.fire;
        sendUDP(msg);
    }

    private void handleMessage(Object object) {
        if (object instanceof Protocol.JoinResponse msg) {
            Gdx.app.postRunnable(() -> {
                if (msg.accepted) {
                    networkManager.setLocalPlayerId(msg.playerId);
                }
                networkManager.fireJoinResponse(msg);
            });
        } else if (object instanceof Protocol.LobbyUpdate msg) {
            Gdx.app.postRunnable(() -> networkManager.fireLobbyUpdate(msg));
        } else if (object instanceof Protocol.StartGame msg) {
            Gdx.app.postRunnable(() -> networkManager.fireGameStart(msg));
        } else if (object instanceof Protocol.GameSnapshot msg) {
            Gdx.app.postRunnable(() -> networkManager.fireGameSnapshot(msg));
        } else if (object instanceof Protocol.PlayerHit msg) {
            Gdx.app.postRunnable(() -> networkManager.firePlayerHit(msg));
        } else if (object instanceof Protocol.PlayerEliminated msg) {
            Gdx.app.postRunnable(() -> networkManager.firePlayerEliminated(msg));
        } else if (object instanceof Protocol.GameOver msg) {
            Gdx.app.postRunnable(() -> networkManager.fireGameOver(msg));
        }
    }
}
