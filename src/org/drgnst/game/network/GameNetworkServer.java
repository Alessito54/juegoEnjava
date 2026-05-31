package org.drgnst.game.network;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Game server that accepts client connections and manages multiplayer game state.
 */
public class GameNetworkServer
{
    private static final int SERVER_PORT = 5000;
    private static final int DISCOVERY_LISTEN_PORT = 5001;
    private static final int MAX_CLIENTS = 2;

    private ServerSocket serverSocket;
    private DatagramSocket discoverySocket;
    private List<ClientHandler> connectedClients;
    private boolean isRunning;
    private boolean gameStarted;
    private Thread acceptThread;
    private Thread discoveryThread;
    private ServerStateListener stateListener;

    public interface ServerStateListener
    {
        void onClientConnected(String clientId, String playerName);
        void onClientDisconnected(String clientId);
        default void onAllClientsReady() {}
    }

    public GameNetworkServer(ServerStateListener listener)
    {
        this.connectedClients = Collections.synchronizedList(new ArrayList<>());
        this.isRunning = false;
        this.gameStarted = false;
        this.stateListener = listener;
    }

    public GameNetworkServer(java.util.function.BiConsumer<String, String> onConnect, java.util.function.Consumer<String> onDisconnect)
    {
        this.connectedClients = Collections.synchronizedList(new ArrayList<>());
        this.isRunning = false;
        this.gameStarted = false;
        this.stateListener = new ServerStateListener()
        {
            @Override
            public void onClientConnected(String clientId, String playerName)
            {
                onConnect.accept(clientId, playerName);
            }

            @Override
            public void onClientDisconnected(String clientId)
            {
                onDisconnect.accept(clientId);
            }
        };
    }

    /**
     * Start the server and listen for connections.
     */
    public void start() throws IOException
    {
        if (isRunning)
            return;

        isRunning = true;
        System.out.println("Starting server...");

        // TCP server
        serverSocket = new ServerSocket(SERVER_PORT);
        System.out.println("TCP Server listening on port " + SERVER_PORT);

        // UDP discovery
        discoverySocket = new DatagramSocket(DISCOVERY_LISTEN_PORT);
        discoverySocket.setBroadcast(true);
        System.out.println("UDP Discovery listening on port " + DISCOVERY_LISTEN_PORT);

        // Accept clients in background thread
        acceptThread = new Thread(() -> acceptClients());
        acceptThread.setName("ServerAcceptThread");
        acceptThread.start();

        // Listen for discovery broadcasts in background thread
        discoveryThread = new Thread(() -> listenForDiscovery());
        discoveryThread.setName("ServerDiscoveryThread");
        discoveryThread.start();
    }

    /**
     * Accept incoming client connections.
     */
    private void acceptClients()
    {
        try
        {
            while (isRunning && !gameStarted)
            {
                Socket clientSocket = serverSocket.accept();
                String clientAddr = clientSocket.getInetAddress().getHostAddress();
                System.out.println("Client connecting from " + clientAddr);

                if (connectedClients.size() < MAX_CLIENTS)
                {
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    connectedClients.add(handler);
                    new Thread(handler).start();
                    System.out.println("Client " + handler.clientId + " accepted. Total: " + connectedClients.size());
                }
                else
                {
                    System.out.println("Server full, rejecting client from " + clientAddr);
                    clientSocket.close();
                }
            }
        }
        catch (Exception e)
        {
            if (isRunning)
                System.err.println("Error in acceptClients: " + e.getMessage());
        }
    }

    /**
     * Listen for network discovery broadcasts.
     */
    private void listenForDiscovery()
    {
        try
        {
            byte[] buffer = new byte[512];
            
            while (isRunning)
            {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                discoverySocket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                
                if (message.startsWith("PERSPECTIVE_DISCOVER"))
                {
                    System.out.println("Received discovery request from " + packet.getAddress().getHostAddress());
                    
                    // Send response
                    String response = "PERSPECTIVE_SERVER:" + SERVER_PORT + ":" + connectedClients.size();
                    byte[] responseBytes = response.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes,
                        responseBytes.length,
                        packet.getAddress(),
                        packet.getPort()
                    );
                    discoverySocket.send(responsePacket);
                    System.out.println("Discovery response sent to " + packet.getAddress().getHostAddress());
                }
            }
        }
        catch (Exception e)
        {
            if (isRunning)
                System.err.println("Error in listenForDiscovery: " + e.getMessage());
        }
    }

    /**
     * Get connected clients list.
     */
    public List<ClientHandler> getConnectedClients()
    {
        return new ArrayList<>(connectedClients);
    }

    /**
     * Remove a client from the server.
     */
    public void removeClient(ClientHandler handler)
    {
        connectedClients.remove(handler);
        System.out.println("Client " + handler.clientId + " disconnected. Total: " + connectedClients.size());
        
        if (stateListener != null)
            stateListener.onClientDisconnected(handler.clientId);
    }

    /**
     * Notify all clients that the game is starting.
     */
    public void startGame()
    {
        gameStarted = true;
        System.out.println("Server starting game with " + connectedClients.size() + " clients");
        
        NetworkProtocol.Message startMsg = new NetworkProtocol.Message(NetworkProtocol.MessageType.START_GAME);
        
        for (ClientHandler client : connectedClients)
        {
            client.send(startMsg);
        }
    }

    /**
     * Broadcast game state to all clients.
     */
    public void broadcastGameState(NetworkProtocol.GameStateMessage gameState)
    {
        if (!gameStarted)
            return;
            
        for (ClientHandler client : connectedClients)
        {
            client.send(gameState);
        }
    }

    /**
     * Stop the server.
     */
    public void stop()
    {
        isRunning = false;
        System.out.println("Stopping server...");
        
        try
        {
            for (ClientHandler client : connectedClients)
            {
                client.disconnect();
            }
            connectedClients.clear();
            
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
            if (discoverySocket != null && !discoverySocket.isClosed())
                discoverySocket.close();
        }
        catch (Exception e)
        {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    /**
     * Handles a connected client.
     */
    public static class ClientHandler implements Runnable
    {
        private Socket socket;
        private GameNetworkServer server;
        public String clientId;
        public String playerName;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private boolean isConnected;

        public ClientHandler(Socket socket, GameNetworkServer server)
        {
            this.socket = socket;
            this.server = server;
            this.clientId = UUID.randomUUID().toString().substring(0, 8);
            this.isConnected = true;
        }

        @Override
        public void run()
        {
            try
            {
                // Initialize streams (output first to avoid deadlock)
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                // Send client ID
                NetworkProtocol.Message welcomeMsg = new NetworkProtocol.Message(NetworkProtocol.MessageType.CLIENT_CONNECTED);
                welcomeMsg.clientId = clientId;
                send(welcomeMsg);

                // Listen for messages from client
                while (isConnected && server.isRunning)
                {
                    try
                    {
                        Object obj = in.readObject();
                        
                        if (obj instanceof NetworkProtocol.ClientConnectMessage)
                        {
                            NetworkProtocol.ClientConnectMessage msg = (NetworkProtocol.ClientConnectMessage) obj;
                            playerName = msg.playerName;
                            System.out.println("Client " + clientId + " identified as: " + playerName);
                            
                            if (server.stateListener != null)
                                server.stateListener.onClientConnected(clientId, playerName);
                            
                            // Send updated player list to all clients
                            sendPlayerListUpdate();
                        }
                        else if (obj instanceof NetworkProtocol.PlayerInputMessage)
                        {
                            // Forward input to other clients if game is running
                            NetworkProtocol.PlayerInputMessage msg = (NetworkProtocol.PlayerInputMessage) obj;
                            msg.clientId = clientId;
                            // TODO: Process input
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
                System.err.println("Error in ClientHandler: " + e.getMessage());
            }
            finally
            {
                disconnect();
            }
        }

        private void sendPlayerListUpdate()
        {
            List<ClientHandler> clients = server.getConnectedClients();
            NetworkProtocol.PlayerListMessage playerList = new NetworkProtocol.PlayerListMessage();
            playerList.playerNames = new String[clients.size()];
            playerList.clientIds = new String[clients.size()];
            
            for (int i = 0; i < clients.size(); i++)
            {
                playerList.playerNames[i] = clients.get(i).playerName != null ? clients.get(i).playerName : "Unnamed";
                playerList.clientIds[i] = clients.get(i).clientId;
            }

            for (ClientHandler client : clients)
            {
                client.send(playerList);
            }
        }

        public void send(Object message)
        {
            try
            {
                if (out != null)
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
                System.err.println("Error sending message to client: " + e.getMessage());
                disconnect();
            }
        }

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
                System.err.println("Error closing client connection: " + e.getMessage());
            }
            
            server.removeClient(this);
        }
    }
}
