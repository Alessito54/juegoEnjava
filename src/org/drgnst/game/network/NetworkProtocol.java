package org.drgnst.game.network;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Defines the protocol for multiplayer networking.
 * Messages are sent as serialized objects over TCP.
 */
public class NetworkProtocol
{
    public enum MessageType
    {
        DISCOVER,              // Client: "Are there servers?"
        DISCOVER_RESPONSE,     // Server: "Yes, I'm here"
        CLIENT_CONNECT,        // Client: "I want to connect with player name X"
        CLIENT_CONNECTED,      // Server: "You're in, waiting for more players"
        PLAYER_LIST_UPDATE,    // Server: "Current players in lobby"
        START_GAME,            // Server: "Game starting now!"
        GAME_STATE_UPDATE,     // Server→Client: Sync position, ammo, etc.
        PLAYER_INPUT,          // Client→Server: Send my input
        PLAYER_LEFT,           // Any: Player disconnected
        ERROR
    }

    /**
     * Base message class
     */
    public static class Message implements Serializable
    {
        private static final long serialVersionUID = 1L;
        public MessageType type;
        public String clientId;        // Unique ID for client
        public String playerName;      // Player name
        public long timestamp;

        public Message(MessageType type)
        {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Server discovery message (broadcast over UDP)
     */
    public static class DiscoverMessage implements Serializable
    {
        private static final long serialVersionUID = 1L;
        public static final String MAGIC = "PERSPECTIVE_SERVER";
        public String magic = MAGIC;
        public String serverIp;
        public int serverPort;
        public int connectedPlayers;
        public int maxPlayers = 2;
    }

    /**
     * Client connect request
     */
    public static class ClientConnectMessage extends Message
    {
        private static final long serialVersionUID = 1L;
        public String playerName;

        public ClientConnectMessage(String playerName)
        {
            super(MessageType.CLIENT_CONNECT);
            this.playerName = playerName;
        }
    }

    /**
     * Player list in lobby
     */
    public static class PlayerListMessage extends Message
    {
        private static final long serialVersionUID = 1L;
        public String[] playerNames;
        public String[] clientIds;

        public PlayerListMessage()
        {
            super(MessageType.PLAYER_LIST_UPDATE);
        }
    }

    /**
     * Game state sync (position, ammo, health, etc.)
     */
    public static class GameStateMessage extends Message
    {
        private static final long serialVersionUID = 1L;
        
        // Player 1 state
        public double p1X, p1Y, p1Angle;
        public int p1Health, p1Ammo, p1Score, p1Kills;
        public boolean p1Firing;
        public boolean p1Moving;
        public boolean p1Reloading;
        
        // Player 2 state
        public double p2X, p2Y, p2Angle;
        public int p2Health, p2Ammo, p2Score, p2Kills;
        public boolean p2Firing;
        public boolean p2Moving;
        public boolean p2Reloading;

        public ArrayList<EnemyState> enemyStates = new ArrayList<>();
        public BossState bossState;
        
        // Global state
        public int numEnemies;
        public int gameTicks;
        public boolean bossDead;

        public GameStateMessage()
        {
            super(MessageType.GAME_STATE_UPDATE);
        }
    }

    public static class EnemyState implements Serializable
    {
        private static final long serialVersionUID = 1L;
        public double x;
        public double y;
        public int health;
        public int attackTimer;
        public int deathTimer;
        public boolean dying;
    }

    public static class BossState implements Serializable
    {
        private static final long serialVersionUID = 1L;
        public double x;
        public double y;
        public int health;
        public int attackTimer;
        public int deathTimer;
        public boolean dying;
    }

    /**
     * Player input (W, A, S, D, Q, E, SPACE, etc.)
     */
    public static class PlayerInputMessage extends Message
    {
        private static final long serialVersionUID = 1L;
        public boolean[] keys;  // Key states
        public double x, y, rot;
        public double xa, ya, ra;
        public boolean firing;

        public PlayerInputMessage()
        {
            super(MessageType.PLAYER_INPUT);
        }
    }

    /**
     * Error message
     */
    public static class ErrorMessage extends Message
    {
        private static final long serialVersionUID = 1L;
        public String errorText;

        public ErrorMessage(String errorText)
        {
            super(MessageType.ERROR);
            this.errorText = errorText;
        }
    }
}
