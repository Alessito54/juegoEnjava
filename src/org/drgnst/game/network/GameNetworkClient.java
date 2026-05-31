package org.drgnst.game.network;

import java.net.*;
import java.io.*;

/**
 * Game client that connects to a server.
 */
public class GameNetworkClient
{
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientId;
    private String playerName;
    private boolean isConnected;
    private Thread listenerThread;
    private ClientStateListener stateListener;

    public interface ClientStateListener
    {
        void onConnected(String clientId);
        void onPlayerListUpdate(String[] playerNames, String[] clientIds);
        void onGameStarting();
        void onGameStateUpdate(NetworkProtocol.GameStateMessage state);
        void onError(String error);
        void onDisconnected();
    }

    public GameNetworkClient(ClientStateListener listener)
    {
        this.stateListener = listener;
        this.isConnected = false;
    }

    public GameNetworkClient(
        java.util.function.Consumer<String> onConnected,
        java.util.function.BiConsumer<String[], String[]> onPlayerListUpdate,
        Runnable onGameStarting,
        java.util.function.Consumer<NetworkProtocol.GameStateMessage> onGameStateUpdate,
        java.util.function.Consumer<String> onError,
        Runnable onDisconnected)
    {
        this.isConnected = false;
        this.stateListener = new ClientStateListener()
        {
            @Override
            public void onConnected(String clientId)
            {
                onConnected.accept(clientId);
            }

            @Override
            public void onPlayerListUpdate(String[] playerNames, String[] clientIds)
            {
                onPlayerListUpdate.accept(playerNames, clientIds);
            }

            @Override
            public void onGameStarting()
            {
                onGameStarting.run();
            }

            @Override
            public void onGameStateUpdate(NetworkProtocol.GameStateMessage state)
            {
                onGameStateUpdate.accept(state);
            }

            @Override
            public void onError(String error)
            {
                onError.accept(error);
            }

            @Override
            public void onDisconnected()
            {
                onDisconnected.run();
            }
        };
    }    /**
     * Connect to a game server.
     */
    public boolean connect(String serverIp, int serverPort, String playerName) throws IOException
    {
        try
        {
            this.playerName = playerName;
            socket = new Socket(serverIp, serverPort);
            System.out.println("Connected to server " + serverIp + ":" + serverPort);

            // Initialize streams (output first)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            isConnected = true;

            // Start listening for server messages
            listenerThread = new Thread(() -> listenForMessages());
            listenerThread.setName("ClientListenerThread");
            listenerThread.start();

            // Send connect message with player name
            NetworkProtocol.ClientConnectMessage connectMsg = new NetworkProtocol.ClientConnectMessage(playerName);
            send(connectMsg);

            return true;
        }
        catch (IOException e)
        {
            System.err.println("Failed to connect to server: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Listen for messages from server.
     */
    private void listenForMessages()
    {
        try
        {
            while (isConnected)
            {
                try
                {
                    Object obj = in.readObject();

                    if (obj instanceof NetworkProtocol.Message)
                    {
                        NetworkProtocol.Message msg = (NetworkProtocol.Message) obj;

                        switch (msg.type)
                        {
                            case CLIENT_CONNECTED:
                                clientId = msg.clientId;
                                System.out.println("Connected with ID: " + clientId);
                                if (stateListener != null)
                                    stateListener.onConnected(clientId);
                                break;

                            case PLAYER_LIST_UPDATE:
                                if (obj instanceof NetworkProtocol.PlayerListMessage)
                                {
                                    NetworkProtocol.PlayerListMessage playerList = (NetworkProtocol.PlayerListMessage) obj;
                                    System.out.println("Players in lobby: " + playerList.playerNames.length);
                                    if (stateListener != null)
                                        stateListener.onPlayerListUpdate(playerList.playerNames, playerList.clientIds);
                                }
                                break;

                            case START_GAME:
                                System.out.println("Server is starting the game!");
                                if (stateListener != null)
                                    stateListener.onGameStarting();
                                break;

                            case GAME_STATE_UPDATE:
                                if (obj instanceof NetworkProtocol.GameStateMessage)
                                {
                                    NetworkProtocol.GameStateMessage gameState = (NetworkProtocol.GameStateMessage) obj;
                                    if (stateListener != null)
                                        stateListener.onGameStateUpdate(gameState);
                                }
                                break;

                            case ERROR:
                                if (obj instanceof NetworkProtocol.ErrorMessage)
                                {
                                    NetworkProtocol.ErrorMessage errMsg = (NetworkProtocol.ErrorMessage) obj;
                                    System.err.println("Server error: " + errMsg.errorText);
                                    if (stateListener != null)
                                        stateListener.onError(errMsg.errorText);
                                }
                                break;

                            default:
                                System.out.println("Received message type: " + msg.type);
                        }
                    }
                }
                catch (EOFException e)
                {
                    break;  // Connection closed
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("Error listening for messages: " + e.getMessage());
        }
        finally
        {
            disconnect();
        }
    }

    /**
     * Send a message to the server.
     */
    public void send(Object message)
    {
        try
        {
            if (out != null && isConnected)
            {
                synchronized (out)
                {
                    out.writeObject(message);
                    out.reset();
                    out.flush();
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("Error sending message: " + e.getMessage());
            disconnect();
        }
    }

    /**
     * Send player input to server.
     */
    public void sendInput(boolean[] keys)
    {
        NetworkProtocol.PlayerInputMessage inputMsg = new NetworkProtocol.PlayerInputMessage();
        inputMsg.keys = keys;
        inputMsg.clientId = clientId;
        send(inputMsg);
    }

    /**
     * Disconnect from server.
     */
    public void disconnect()
    {
        isConnected = false;

        try
        {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        }
        catch (Exception e)
        {
            System.err.println("Error disconnecting: " + e.getMessage());
        }

        if (stateListener != null)
            stateListener.onDisconnected();
    }

    public boolean isConnected()
    {
        return isConnected;
    }

    public String getClientId()
    {
        return clientId;
    }

    public String getPlayerName()
    {
        return playerName;
    }
}
