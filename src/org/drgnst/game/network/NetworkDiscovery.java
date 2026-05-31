package org.drgnst.game.network;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Discovers available game servers on the local network.
 */
public class NetworkDiscovery
{
    private static final int DISCOVERY_PORT = 5001;
    private static final int DISCOVERY_TIMEOUT = 3000;  // 3 seconds
    private static final int SOCKET_TIMEOUT = 500;      // 500ms per response

    public static class ServerInfo
    {
        public String ip;
        public int port;
        public String serverIp;
        public int connectedPlayers;

        public ServerInfo(String ip, int port, String serverIp, int connectedPlayers)
        {
            this.ip = ip;
            this.port = port;
            this.serverIp = serverIp;
            this.connectedPlayers = connectedPlayers;
        }

        @Override
        public String toString()
        {
            return serverIp + ":" + port + " (" + connectedPlayers + " players)";
        }
    }

    /**
     * Scan for available servers on the local network.
     * Broadcasts discovery packet and waits for responses.
     */
    public static List<ServerInfo> discoverServers()
    {
        List<ServerInfo> servers = new ArrayList<>();
        
        try
        {
            // Get local network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements())
            {
                NetworkInterface ni = interfaces.nextElement();
                
                if (ni.isLoopback() || !ni.isUp())
                    continue;
                
                // Get broadcast address for this interface
                for (InterfaceAddress ia : ni.getInterfaceAddresses())
                {
                    InetAddress broadcast = ia.getBroadcast();
                    if (broadcast == null)
                        continue;
                    
                    System.out.println("Scanning network: " + broadcast);
                    broadcastDiscovery(broadcast, servers);
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("Error discovering servers: " + e.getMessage());
            e.printStackTrace();
        }
        
        return servers;
    }

    /**
     * Send broadcast discovery packet and collect responses.
     */
    private static void broadcastDiscovery(InetAddress broadcastAddr, List<ServerInfo> servers)
    {
        try
        {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(SOCKET_TIMEOUT);
            
            // Send discovery packet
            byte[] message = "PERSPECTIVE_DISCOVER".getBytes();
            DatagramPacket packet = new DatagramPacket(
                message, 
                message.length, 
                broadcastAddr, 
                DISCOVERY_PORT
            );
            
            socket.send(packet);
            System.out.println("Discovery packet sent to " + broadcastAddr);
            
            // Listen for responses
            long startTime = System.currentTimeMillis();
            byte[] responseBuffer = new byte[512];
            
            while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT)
            {
                try
                {
                    DatagramPacket responsePacket = new DatagramPacket(
                        responseBuffer, 
                        responseBuffer.length
                    );
                    socket.receive(responsePacket);
                    
                    String responseStr = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    System.out.println("Discovery response: " + responseStr);
                    
                    // Parse response: "PERSPECTIVE_SERVER:port:players"
                    if (responseStr.startsWith("PERSPECTIVE_SERVER:"))
                    {
                        String[] parts = responseStr.split(":");
                        if (parts.length >= 3)
                        {
                            int port = Integer.parseInt(parts[1]);
                            int players = Integer.parseInt(parts[2]);
                            String serverIp = responsePacket.getAddress().getHostAddress();
                            
                            ServerInfo info = new ServerInfo(
                                responsePacket.getAddress().getHostAddress(),
                                responsePacket.getPort(),
                                serverIp,
                                players
                            );
                            info.port = port;
                            
                            // Avoid duplicates
                            boolean found = false;
                            for (ServerInfo s : servers)
                            {
                                if (s.serverIp.equals(info.serverIp) && s.port == info.port)
                                {
                                    found = true;
                                    break;
                                }
                            }
                            
                            if (!found)
                            {
                                servers.add(info);
                                System.out.println("Found server: " + info);
                            }
                        }
                    }
                }
                catch (SocketTimeoutException e)
                {
                    // Timeout, continue listening
                }
            }
            
            socket.close();
        }
        catch (Exception e)
        {
            System.err.println("Error in broadcast discovery: " + e.getMessage());
        }
    }
}
