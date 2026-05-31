package org.drgnst.game;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import javax.swing.JFrame;

import org.drgnst.game.gfx.Screen;
import org.drgnst.game.network.*;

public class Main extends Canvas implements Runnable
{
    private static final long serialVersionUID = 1L;

    public static final int WIDTH = 160;
    public static final int HEIGHT = WIDTH * 3 / 4;
    public static final int SCALE = 4;
    public static final int WINDOW_WIDTH = 1672;
    public static final int WINDOW_HEIGHT = 941;
    public static final int GAME_DRAW_WIDTH = WIDTH * SCALE;
    public static final int GAME_DRAW_HEIGHT = HEIGHT * SCALE;
    public static final int MENU_WIDTH = WIDTH * 3;  // 480
    public static final int MENU_HEIGHT = HEIGHT * 3;  // 360
    public static final int MENU_SCALE = SCALE / 3;  // ~1.3x para menu
    public static final String TITLE = "Perspective";
    public static final double FRAME_LIMIT = 60.0;

    private static final int BUTTON_WIDTH = 320;
    private static final int BUTTON_HEIGHT = 52;
    private static final int BUTTON_GAP = 14;
    private static final int PLAYER_INPUT_MAX_LENGTH = 18;
    private static final int CLIENT_SERVER_ROW_HEIGHT = 38;
    private static final int CLIENT_SERVER_ROW_GAP = 10;

    private enum GameState
    {
        MENU,
        CREATE_PLAYER,
        SELECT_PLAYER,
        MULTIPLAYER_MENU,
        NETWORK_SERVER,
        NETWORK_CLIENT,
        PLAYING,
        PAUSED
    }

    private boolean isRunning = false;

    public final BufferedImage image;
    public final BufferedImage imageMenu;
    public final BufferedImage imageBackground;
    public final int[] pixels;
    public final int[] pixelsMenu;

    private Game game;
    private Screen screen;
    private Screen screenMenu;
    private InputHandler inputHandler;
    private Menu menu;
    private ScoreWindow scoreWindow;
    private JFrame hostFrame;
    private GameState state;
    private boolean spaceWasDown;
    private boolean escapeWasDown;
    private boolean enterWasDown;
    private boolean backspaceWasDown;
    private boolean cWasDown;
    private boolean selectWasDown;
    private boolean leftSelectWasDown;
    private boolean rightSelectWasDown;
    private boolean restartWasDown;
    private boolean menuWasDown;
    private String pendingPlayerName;
    private List<String> playerList;
    private int selectedPlayerIndex;
    
    // Networking
    private GameNetworkServer networkServer;
    private GameNetworkClient networkClient;
    private List<NetworkDiscovery.ServerInfo> discoveredServers;
    private int selectedServerIndex;
    private String[] lobbyPlayerNames;
    private String[] lobbyClientIds;
    private boolean isServerReady;

    public Main()
    {
        Dimension d = new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT);
        setMinimumSize(d);
        setMaximumSize(d);
        setPreferredSize(d);

        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        imageMenu = new BufferedImage(MENU_WIDTH, MENU_HEIGHT, BufferedImage.TYPE_INT_RGB);
        pixelsMenu = ((DataBufferInt) imageMenu.getRaster().getDataBuffer()).getData();

        imageBackground = loadBackgroundImage();

        inputHandler = new InputHandler();
        pendingPlayerName = "";
        playerList = new ArrayList<String>();
        selectedPlayerIndex = 0;
        
        // Networking initialization
        discoveredServers = new ArrayList<>();
        selectedServerIndex = 0;
        lobbyPlayerNames = new String[0];
        lobbyClientIds = new String[0];
        isServerReady = false;

        addKeyListener(inputHandler);
        addMouseListener(inputHandler);
        addFocusListener(inputHandler);
        addMouseMotionListener(inputHandler);
        addMouseWheelListener(inputHandler);
    }

    public void start()
    {
        if (isRunning)
            return;

        isRunning = true;

        init();
        new Thread(this).start();
    }

    public void init()
    {
        game = new Game();
        screen = new Screen(WIDTH, HEIGHT);
        screenMenu = new Screen(MENU_WIDTH, MENU_HEIGHT);
        menu = new Menu();
        scoreWindow = new ScoreWindow();
        if (hostFrame != null)
            scoreWindow.pinNear(hostFrame);
        scoreWindow.setHidden();
        state = GameState.MENU;
        refreshPlayerList();
        spaceWasDown = false;
        escapeWasDown = false;
        enterWasDown = false;
        backspaceWasDown = false;
        cWasDown = false;
        selectWasDown = false;
        leftSelectWasDown = false;
        rightSelectWasDown = false;
        restartWasDown = false;
        menuWasDown = false;
    }

    public void run()
    {
        final double nsPerUpdate = 1000000000.0 / FRAME_LIMIT;

        long lastTime = System.nanoTime();
        double unprocessedTime = 0;

        int frames = 0;
        int updates = 0;

        long frameCounter = System.currentTimeMillis();

        while (isRunning)
        {
            long currentTime = System.nanoTime();
            long passedTime = currentTime - lastTime;
            lastTime = currentTime;
            unprocessedTime += passedTime;

            boolean updated = false;

            if (unprocessedTime >= nsPerUpdate)
            {
                while (unprocessedTime >= nsPerUpdate)
                {
                    unprocessedTime -= nsPerUpdate;
                    update();
                    updates++;
                    updated = true;
                }
            }

            if (updated || frames == 0)
            {
                render();
                frames++;
            }
            else
            {
                try
                {
                    Thread.sleep(1);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (System.currentTimeMillis() - frameCounter >= 1000)
            {
                System.out.println("Frames : " + frames + ", Updates :" +
                updates);
                frames = 0;
                updates = 0;
                frameCounter += 1000;
            }
        }

        dispose();
    }

    public void render()
    {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null)
        {
            createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();

        if (state == GameState.MENU || state == GameState.CREATE_PLAYER || state == GameState.SELECT_PLAYER || 
            state == GameState.MULTIPLAYER_MENU || state == GameState.NETWORK_SERVER || state == GameState.NETWORK_CLIENT || 
            state == GameState.PAUSED)
        {
            if (state == GameState.MENU || state == GameState.CREATE_PLAYER || state == GameState.SELECT_PLAYER ||
                state == GameState.MULTIPLAYER_MENU || state == GameState.NETWORK_SERVER || state == GameState.NETWORK_CLIENT)
            {
                menu.renderToGraphics(g, WINDOW_WIDTH, WINDOW_HEIGHT);
                if (state == GameState.MENU)
                    renderMainMenuOverlay(g);
                else if (state == GameState.CREATE_PLAYER)
                    renderCreatePlayerOverlay(g);
                else if (state == GameState.SELECT_PLAYER)
                    renderSelectPlayerOverlay(g);
                else if (state == GameState.MULTIPLAYER_MENU)
                    renderMultiplayerMenuOverlay(g);
                else if (state == GameState.NETWORK_SERVER)
                    renderServerOverlay(g);
                else if (state == GameState.NETWORK_CLIENT)
                    renderClientOverlay(g);
            }
            else
            {
                if (imageBackground != null)
                {
                    g.drawImage(imageBackground, 0, 0, null);
                }

                screen.render(game);

                for (int i = 0; i < pixels.length; i++)
                {
                    pixels[i] = screen.pixels[i];
                }

                int gameX = (WINDOW_WIDTH - GAME_DRAW_WIDTH) / 2;
                int gameY = (WINDOW_HEIGHT - GAME_DRAW_HEIGHT) / 2;
                g.drawImage(image, gameX, gameY, GAME_DRAW_WIDTH, GAME_DRAW_HEIGHT, null);

                if (game != null && game.isMultiplayerEnabled())
                    renderMultiplayerRoleOverlay(g, gameX, gameY);

                renderPauseOverlay(g);
            }
        }
        else
        {
            if (imageBackground != null)
            {
                g.drawImage(imageBackground, 0, 0, null);
            }

            screen.render(game);

            for (int i = 0; i < pixels.length; i++)
            {
                pixels[i] = screen.pixels[i];
            }

            int gameX = (WINDOW_WIDTH - GAME_DRAW_WIDTH) / 2;
            int gameY = (WINDOW_HEIGHT - GAME_DRAW_HEIGHT) / 2;
            g.drawImage(image, gameX, gameY, GAME_DRAW_WIDTH, GAME_DRAW_HEIGHT, null);

            if (game != null && game.isMultiplayerEnabled())
                renderMultiplayerRoleOverlay(g, gameX, gameY);
        }

        g.dispose();
        bs.show();
    }

    public void update()
    {
        if (state == GameState.MENU)
        {
            boolean spaceDown = inputHandler.keys[KeyEvent.VK_SPACE];
            boolean cDown = inputHandler.keys[KeyEvent.VK_C];
            boolean pDown = inputHandler.keys[KeyEvent.VK_P];
            if (spaceDown && !spaceWasDown)
                startGame();
            if (cDown && !cWasDown)
            {
                pendingPlayerName = "";
                state = GameState.CREATE_PLAYER;
            }
            if (pDown && !selectWasDown)
            {
                openPlayerSelector();
            }
            spaceWasDown = spaceDown;
            cWasDown = cDown;
            selectWasDown = pDown;

            if (inputHandler.consumeMouseClick())
            {
                int clicked = checkMainMenuClick(inputHandler.mouseX, inputHandler.mouseY);
                if (clicked == Menu.BUTTON_START)
                {
                    startGame();
                }
                else if (clicked == Menu.BUTTON_START + 1)
                {
                    pendingPlayerName = "";
                    state = GameState.CREATE_PLAYER;
                }
                else if (clicked == Menu.BUTTON_START + 2)
                {
                    openPlayerSelector();
                }
                else if (clicked == Menu.BUTTON_START + 3)
                {
                    state = GameState.MULTIPLAYER_MENU;
                }
            }

            screenMenu.update();
        }
        else if (state == GameState.CREATE_PLAYER)
        {
            handleCreatePlayerInput();
        }
        else if (state == GameState.SELECT_PLAYER)
        {
            handleSelectPlayerInput();
        }
        else if (state == GameState.MULTIPLAYER_MENU)
        {
            handleMultiplayerMenuInput();
        }
        else if (state == GameState.NETWORK_SERVER)
        {
            handleServerInput();
        }
        else if (state == GameState.NETWORK_CLIENT)
        {
            handleClientInput();
        }
        else if (state == GameState.PAUSED)
        {
            handlePauseInput();
        }
        else
        {
            if (networkServer != null)
            {
                game.update(inputHandler.keys);
                networkServer.broadcastGameState(buildServerGameState());
            }
            else if (networkClient != null && networkClient.isConnected())
            {
                networkClient.sendInput(inputHandler.keys);
            }
            else
            {
                game.update(inputHandler.keys);
            }

            screen.update();

            if (inputHandler.keys[KeyEvent.VK_ESCAPE] && !escapeWasDown)
            {
                state = GameState.PAUSED;
                game.syncInputState(inputHandler.keys);
            }

            escapeWasDown = inputHandler.keys[KeyEvent.VK_ESCAPE];
        }

        syncScoreWindow();
    }

    public void stop()
    {
        if (!isRunning)
            return;

        isRunning = false;
    }

    public void dispose()
    {
        if (scoreWindow != null)
        {
            scoreWindow.setHidden();
            scoreWindow.dispose();
        }

        if (game != null)
        {
            game.cleanup();
        }
        System.exit(0);
    }

    public void attachHostFrame(JFrame frame)
    {
        hostFrame = frame;
        if (scoreWindow != null)
            scoreWindow.pinNear(hostFrame);
    }

    private void startGame()
    {
        game.disableMultiplayer();
        game.resetRun();
        state = GameState.PLAYING;
        spaceWasDown = inputHandler.keys[KeyEvent.VK_SPACE];
        escapeWasDown = inputHandler.keys[KeyEvent.VK_ESCAPE];
        enterWasDown = inputHandler.keys[KeyEvent.VK_ENTER];
        backspaceWasDown = inputHandler.keys[KeyEvent.VK_BACK_SPACE];
        syncScoreWindow();
    }

    private void openPlayerSelector()
    {
        refreshPlayerList();
        if (playerList.isEmpty())
        {
            pendingPlayerName = "";
            state = GameState.CREATE_PLAYER;
            return;
        }

        String current = game.getCurrentPlayerName();
        selectedPlayerIndex = 0;
        for (int i = 0; i < playerList.size(); i++)
        {
            if (playerList.get(i).equalsIgnoreCase(current))
            {
                selectedPlayerIndex = i;
                break;
            }
        }

        state = GameState.SELECT_PLAYER;
    }

    private void handleCreatePlayerInput()
    {
        String typed = inputHandler.consumeTypedCharacters();
        if (!typed.isEmpty())
        {
            for (int i = 0; i < typed.length(); i++)
            {
                char c = typed.charAt(i);
                if (Character.isISOControl(c))
                    continue;

                if (pendingPlayerName.length() >= PLAYER_INPUT_MAX_LENGTH)
                    break;

                if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-')
                    pendingPlayerName += c;
            }
        }

        boolean enterDown = inputHandler.keys[KeyEvent.VK_ENTER];
        boolean backspaceDown = inputHandler.keys[KeyEvent.VK_BACK_SPACE];
        boolean escapeDown = inputHandler.keys[KeyEvent.VK_ESCAPE];

        if (backspaceDown && !backspaceWasDown && pendingPlayerName.length() > 0)
            pendingPlayerName = pendingPlayerName.substring(0, pendingPlayerName.length() - 1);

        if (enterDown && !enterWasDown)
        {
            String playerName = pendingPlayerName.trim();
            if (playerName.isEmpty())
                playerName = "Jugador";

            game.selectPlayer(playerName);
            startGame();
            return;
        }

        if (escapeDown && !escapeWasDown)
        {
            pendingPlayerName = "";
            state = GameState.MENU;
        }

        enterWasDown = enterDown;
        backspaceWasDown = backspaceDown;
        escapeWasDown = escapeDown;
        game.syncInputState(inputHandler.keys);
    }

    private void handleSelectPlayerInput()
    {
        if (playerList.isEmpty())
        {
            state = GameState.CREATE_PLAYER;
            return;
        }

        boolean leftDown = inputHandler.keys[KeyEvent.VK_LEFT] || inputHandler.keys[KeyEvent.VK_A];
        boolean rightDown = inputHandler.keys[KeyEvent.VK_RIGHT] || inputHandler.keys[KeyEvent.VK_D];
        boolean enterDown = inputHandler.keys[KeyEvent.VK_ENTER];
        boolean escapeDown = inputHandler.keys[KeyEvent.VK_ESCAPE];

        if (leftDown && !leftSelectWasDown)
        {
            selectedPlayerIndex = (selectedPlayerIndex - 1 + playerList.size()) % playerList.size();
        }
        else if (rightDown && !rightSelectWasDown)
        {
            selectedPlayerIndex = (selectedPlayerIndex + 1) % playerList.size();
        }

        if (enterDown && !enterWasDown)
        {
            game.selectPlayer(playerList.get(selectedPlayerIndex));
            startGame();
            return;
        }

        if (escapeDown && !escapeWasDown)
        {
            state = GameState.MENU;
        }

        leftSelectWasDown = leftDown;
        rightSelectWasDown = rightDown;
        enterWasDown = enterDown;
        escapeWasDown = escapeDown;
        game.syncInputState(inputHandler.keys);
    }

    private void handlePauseInput()
    {
        boolean escapeDown = inputHandler.keys[KeyEvent.VK_ESCAPE];
        boolean enterDown = inputHandler.keys[KeyEvent.VK_ENTER];
        boolean restartDown = inputHandler.keys[KeyEvent.VK_R];
        boolean menuDown = inputHandler.keys[KeyEvent.VK_M];

        if (escapeDown && !escapeWasDown)
        {
            state = GameState.PLAYING;
            game.syncInputState(inputHandler.keys);
        }
        else if (enterDown && !enterWasDown)
        {
            state = GameState.PLAYING;
            game.syncInputState(inputHandler.keys);
        }
        else if (restartDown && !restartWasDown)
        {
            game.resetRun();
            state = GameState.PLAYING;
            game.syncInputState(inputHandler.keys);
        }
        else if (menuDown && !menuWasDown)
        {
            game.resetRun();
            state = GameState.MENU;
            pendingPlayerName = "";
        }

        escapeWasDown = escapeDown;
        enterWasDown = enterDown;
        backspaceWasDown = inputHandler.keys[KeyEvent.VK_BACK_SPACE];
        restartWasDown = restartDown;
        menuWasDown = menuDown;
    }

    private void syncScoreWindow()
    {
        if (scoreWindow == null)
            return;

        if (hostFrame != null)
            scoreWindow.pinNear(hostFrame);

        if (state == GameState.PLAYING || state == GameState.PAUSED)
        {
            scoreWindow.updateFromGame(game, state == GameState.PAUSED);
        }
        else
        {
            scoreWindow.setHidden();
        }
    }

    private void renderMainMenuOverlay(Graphics g)
    {
        renderCenteredMenuCard(g);
    }

    private void renderCreatePlayerOverlay(Graphics g)
    {
        int w = 680;
        int h = 260;
        int x = (WINDOW_WIDTH - w) / 2;
        int y = (WINDOW_HEIGHT - h) / 2;

        g.setColor(new java.awt.Color(10, 10, 14, 230));
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(java.awt.Color.WHITE);
        g.drawRoundRect(x, y, w, h, 20, 20);

        g.setFont(g.getFont().deriveFont(30f));
        g.drawString("Crear jugador", x + 20, y + 42);
        g.setFont(g.getFont().deriveFont(24f));
        g.drawString("Nombre", x + 20, y + 92);
        g.setFont(g.getFont().deriveFont(28f));
        g.drawString(pendingPlayerName + "|", x + 20, y + 132);
        g.setFont(g.getFont().deriveFont(20f));
        g.drawString("Enter: guardar   Backspace: borrar   ESC: volver", x + 20, y + 182);
        g.drawString("Solo letras, numeros, espacios, guion y guion bajo.", x + 20, y + 216);
    }

    private void renderSelectPlayerOverlay(Graphics g)
    {
        int w = 760;
        int h = 310;
        int x = (WINDOW_WIDTH - w) / 2;
        int y = (WINDOW_HEIGHT - h) / 2;

        g.setColor(new java.awt.Color(10, 10, 14, 230));
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(java.awt.Color.WHITE);
        g.drawRoundRect(x, y, w, h, 24, 24);

        g.setFont(g.getFont().deriveFont(30f));
        g.drawString("Elegir jugador", x + 24, y + 44);

        String current = playerList.isEmpty() ? "Sin jugadores" : playerList.get(selectedPlayerIndex);
        int currentScore = playerList.isEmpty() ? 0 : game.getPlayerBestScore(current);

        g.setFont(g.getFont().deriveFont(34f));
        g.drawString("< " + current + " >", x + 24, y + 120);
        g.setFont(g.getFont().deriveFont(24f));
        g.drawString("Mejor score: " + currentScore, x + 24, y + 165);
        g.drawString("Flechas o A/D para cambiar", x + 24, y + 205);
        g.drawString("Enter para jugar   ESC para volver", x + 24, y + 240);

        g.setFont(g.getFont().deriveFont(18f));
        String top = "Top actual: " + game.getTopPlayerName() + " - " + game.getTopPlayerScore();
        g.drawString(top, x + 24, y + 275);
    }

    private void renderPauseOverlay(Graphics g)
    {
        g.setColor(new java.awt.Color(0, 0, 0, 150));
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        int w = 460;
        int h = 220;
        int x = (WINDOW_WIDTH - w) / 2;
        int y = (WINDOW_HEIGHT - h) / 2;

        g.setColor(new java.awt.Color(20, 20, 20, 220));
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(java.awt.Color.WHITE);
        g.drawRoundRect(x, y, w, h, 24, 24);

        g.setFont(g.getFont().deriveFont(32f));
        g.drawString("PAUSA", x + 170, y + 46);
        g.setFont(g.getFont().deriveFont(22f));
        g.drawString("ESC o ENTER para continuar", x + 38, y + 94);
        g.drawString("R para reiniciar la partida", x + 38, y + 130);
        g.drawString("M para volver al menu", x + 38, y + 166);
    }

    private void renderCenteredMenuCard(Graphics g)
    {
        int w = 820;
        int h = 480;
        int x = (WINDOW_WIDTH - w) / 2;
        int y = (WINDOW_HEIGHT - h) / 2;

        g.setColor(new java.awt.Color(10, 10, 14, 220));
        g.fillRoundRect(x, y, w, h, 26, 26);
        g.setColor(java.awt.Color.WHITE);
        g.drawRoundRect(x, y, w, h, 26, 26);

        g.setFont(g.getFont().deriveFont(42f));
        g.drawString("PERSPECTIVE", x + 24, y + 56);

        g.setFont(g.getFont().deriveFont(24f));
        g.drawString("Jugador actual: " + game.getCurrentPlayerName(), x + 24, y + 102);
        g.drawString("Mejor marca: " + game.getCurrentPlayerBestScore(), x + 24, y + 134);
        g.drawString("Top global: " + game.getTopPlayerName() + " - " + game.getTopPlayerScore(), x + 24, y + 166);

        drawMenuButton(g, x + 24, y + 210, BUTTON_WIDTH, BUTTON_HEIGHT, "JUGAR", true);
        drawMenuButton(g, x + 24, y + 210 + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT, "CREAR JUGADOR", false);
        drawMenuButton(g, x + 24, y + 210 + (BUTTON_HEIGHT + BUTTON_GAP) * 2, BUTTON_WIDTH, BUTTON_HEIGHT, "ELEGIR JUGADOR", false);
        drawMenuButton(g, x + 24, y + 210 + (BUTTON_HEIGHT + BUTTON_GAP) * 3, BUTTON_WIDTH, BUTTON_HEIGHT, "MULTIJUGADOR", false);
    }

    private void drawMenuButton(Graphics g, int x, int y, int w, int h, String text, boolean primary)
    {
        boolean hover = inputHandler.mouseX >= x && inputHandler.mouseX < x + w && inputHandler.mouseY >= y && inputHandler.mouseY < y + h;
        java.awt.Color fill = primary ? new java.awt.Color(85, 25, 135, 220) : new java.awt.Color(42, 42, 52, 220);
        if (hover)
            fill = primary ? new java.awt.Color(115, 35, 180, 230) : new java.awt.Color(60, 60, 72, 230);

        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 18, 18);
        g.setColor(java.awt.Color.WHITE);
        g.drawRoundRect(x, y, w, h, 18, 18);
        g.setFont(g.getFont().deriveFont(22f));
        g.drawString(text, x + 20, y + 33);
    }

    private int checkMainMenuClick(int mouseX, int mouseY)
    {
        int w = 820;
        int h = 480;
        int x = (WINDOW_WIDTH - w) / 2;
        int y = (WINDOW_HEIGHT - h) / 2;

        int bx = x + 24;
        int by = y + 210;

        if (isInside(mouseX, mouseY, bx, by, BUTTON_WIDTH, BUTTON_HEIGHT))
            return Menu.BUTTON_START;
        if (isInside(mouseX, mouseY, bx, by + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT))
            return Menu.BUTTON_START + 1;
        if (isInside(mouseX, mouseY, bx, by + (BUTTON_HEIGHT + BUTTON_GAP) * 2, BUTTON_WIDTH, BUTTON_HEIGHT))
            return Menu.BUTTON_START + 2;
        if (isInside(mouseX, mouseY, bx, by + (BUTTON_HEIGHT + BUTTON_GAP) * 3, BUTTON_WIDTH, BUTTON_HEIGHT))
            return Menu.BUTTON_START + 3;

        return Menu.BUTTON_NONE;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int w, int h)
    {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void refreshPlayerList()
    {
        playerList = new ArrayList<String>(game.getPlayerNames());
        if (playerList.isEmpty())
            selectedPlayerIndex = 0;
        else if (selectedPlayerIndex >= playerList.size())
            selectedPlayerIndex = playerList.size() - 1;
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.setTitle(TITLE);
        frame.setResizable(false);
        Main game = new Main();
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);

        game.attachHostFrame(frame);

        game.start();
    }

    private void renderMultiplayerMenuOverlay(Graphics g)
    {
        int w = 680;
        int h = 320;
        int x = (WINDOW_WIDTH - w) / 2;
        int y = (WINDOW_HEIGHT - h) / 2;

        g.setColor(new java.awt.Color(10, 10, 14, 230));
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(java.awt.Color.WHITE);
        g.drawRoundRect(x, y, w, h, 20, 20);

        g.setFont(g.getFont().deriveFont(30f));
        g.drawString("MULTIJUGADOR", x + 20, y + 42);

        drawMenuButton(g, x + 20, y + 90, BUTTON_WIDTH, BUTTON_HEIGHT, "SERVIDOR", true);
        drawMenuButton(g, x + 20, y + 90 + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT, "CLIENTE", false);

        g.setFont(g.getFont().deriveFont(18f));
        g.drawString("ESC: volver al menu", x + 20, y + 290);
    }

    private void renderServerOverlay(Graphics g)
    {
        int w = 700;
        int h = 400;
        int x = (WINDOW_WIDTH - w) / 2;
        int y = (WINDOW_HEIGHT - h) / 2;

        g.setColor(new java.awt.Color(10, 10, 14, 230));
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(java.awt.Color.WHITE);
        g.drawRoundRect(x, y, w, h, 20, 20);

        g.setFont(g.getFont().deriveFont(28f));
        g.drawString("SERVIDOR DE JUEGO", x + 20, y + 42);

        g.setFont(g.getFont().deriveFont(20f));
        g.drawString("Estado: " + (networkServer != null && networkServer.getConnectedClients().size() > 0 ? "ESPERANDO" : "INICIALIZANDO..."), x + 20, y + 90);

        List<GameNetworkServer.ClientHandler> clients = networkServer != null ? networkServer.getConnectedClients() : new ArrayList<>();
        g.drawString("Jugadores conectados: " + clients.size() + "/1", x + 20, y + 130);

        int clientY = y + 170;
        for (int i = 0; i < clients.size(); i++)
        {
            GameNetworkServer.ClientHandler client = clients.get(i);
            String name = client.playerName != null ? client.playerName : "Conectando...";
            g.drawString((i + 1) + ". " + name, x + 40, clientY);
            clientY += 30;
        }

        if (clients.size() == 1 && isServerReady)
        {
            drawMenuButton(g, x + 20, y + 330, BUTTON_WIDTH, BUTTON_HEIGHT, "INICIAR JUEGO", true);
        }

        g.setFont(g.getFont().deriveFont(16f));
        g.drawString("ESC: volver", x + 20, y + 370);
    }

    private void renderClientOverlay(Graphics g)
    {
        int w = 700;
        int h = 400;
        int x = (WINDOW_WIDTH - w) / 2;
        int y = (WINDOW_HEIGHT - h) / 2;

        g.setColor(new java.awt.Color(10, 10, 14, 230));
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(java.awt.Color.WHITE);
        g.drawRoundRect(x, y, w, h, 20, 20);

        g.setFont(g.getFont().deriveFont(28f));
        g.drawString("CLIENTE", x + 20, y + 42);

        g.setFont(g.getFont().deriveFont(20f));
        g.drawString("Servidores disponibles:", x + 20, y + 92);

        int rowX = x + 20;
        int rowY = y + 112;
        int rowW = w - 40;

        if (discoveredServers.isEmpty())
        {
            g.setColor(new java.awt.Color(36, 36, 44, 230));
            g.fillRoundRect(rowX, rowY, rowW, CLIENT_SERVER_ROW_HEIGHT, 12, 12);
            g.setColor(java.awt.Color.WHITE);
            g.drawRoundRect(rowX, rowY, rowW, CLIENT_SERVER_ROW_HEIGHT, 12, 12);
            g.drawString("No hay servidores - pulsa ACTUALIZAR", rowX + 14, rowY + 25);
        }
        else
        {
            int maxRows = Math.min(discoveredServers.size(), 4);
            for (int i = 0; i < maxRows; i++)
            {
                NetworkDiscovery.ServerInfo info = discoveredServers.get(i);
                int yRow = rowY + i * (CLIENT_SERVER_ROW_HEIGHT + CLIENT_SERVER_ROW_GAP);
                boolean selected = i == selectedServerIndex;
                boolean hover = isInside(inputHandler.mouseX, inputHandler.mouseY, rowX, yRow, rowW, CLIENT_SERVER_ROW_HEIGHT);

                java.awt.Color fill = selected ? new java.awt.Color(85, 25, 135, 220) : new java.awt.Color(42, 42, 52, 220);
                if (hover)
                    fill = selected ? new java.awt.Color(115, 35, 180, 230) : new java.awt.Color(60, 60, 72, 230);

                g.setColor(fill);
                g.fillRoundRect(rowX, yRow, rowW, CLIENT_SERVER_ROW_HEIGHT, 12, 12);
                g.setColor(java.awt.Color.WHITE);
                g.drawRoundRect(rowX, yRow, rowW, CLIENT_SERVER_ROW_HEIGHT, 12, 12);

                String text = info.serverIp + ":" + info.port + "  (" + info.connectedPlayers + "/1)";
                g.drawString(text, rowX + 14, yRow + 25);
            }
        }

        int buttonsY = y + h - 82;
        drawMenuButton(g, x + 20, buttonsY, 220, BUTTON_HEIGHT, "ACTUALIZAR", false);
        drawMenuButton(g, x + 250, buttonsY, 220, BUTTON_HEIGHT, "CONECTAR", true);
        drawMenuButton(g, x + 480, buttonsY, 190, BUTTON_HEIGHT, "VOLVER", false);

        g.setFont(g.getFont().deriveFont(16f));
        g.drawString("Click para seleccionar servidor. Enter tambien conecta.", x + 20, y + h - 18);
    }

    private void handleMultiplayerMenuInput()
    {
        boolean spaceDown = inputHandler.keys[KeyEvent.VK_SPACE];
        boolean sDown = inputHandler.keys[KeyEvent.VK_S];
        boolean cDown = inputHandler.keys[KeyEvent.VK_C];
        boolean escapeDown = inputHandler.keys[KeyEvent.VK_ESCAPE];

        if (spaceDown && !spaceWasDown)
        {
            startServer();
        }

        if (sDown && !selectWasDown)
        {
            startServer();
        }

        if (cDown && !cWasDown)
        {
            startClient();
        }

        if (escapeDown && !escapeWasDown)
        {
            state = GameState.MENU;
            if (networkServer != null)
                networkServer.stop();
        }

        if (inputHandler.consumeMouseClick())
        {
            int w = 680;
            int h = 320;
            int x = (WINDOW_WIDTH - w) / 2;
            int y = (WINDOW_HEIGHT - h) / 2;
            int bx = x + 20;
            int by = y + 90;

            if (isInside(inputHandler.mouseX, inputHandler.mouseY, bx, by, BUTTON_WIDTH, BUTTON_HEIGHT))
            {
                startServer();
            }
            else if (isInside(inputHandler.mouseX, inputHandler.mouseY, bx, by + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT))
            {
                startClient();
            }
        }

        spaceWasDown = spaceDown;
        selectWasDown = sDown;
        cWasDown = cDown;
        escapeWasDown = escapeDown;
        screenMenu.update();
    }

    private void handleServerInput()
    {
        boolean escapeDown = inputHandler.keys[KeyEvent.VK_ESCAPE];
        boolean enterDown = inputHandler.keys[KeyEvent.VK_ENTER];

        if (escapeDown && !escapeWasDown)
        {
            state = GameState.MULTIPLAYER_MENU;
            if (networkServer != null)
                networkServer.stop();
            networkServer = null;
        }

        if (enterDown && !enterWasDown && networkServer != null && networkServer.getConnectedClients().size() == 1 && isServerReady)
        {
            networkServer.startGame();
            state = GameState.PLAYING;
            game.enableMultiplayer(true);
            game.resetRun();
        }

        if (inputHandler.consumeMouseClick())
        {
            int w = 700;
            int h = 400;
            int x = (WINDOW_WIDTH - w) / 2;
            int y = (WINDOW_HEIGHT - h) / 2;
            int bx = x + 20;
            int by = y + 330;

            if (networkServer != null && networkServer.getConnectedClients().size() == 1 && isServerReady)
            {
                if (isInside(inputHandler.mouseX, inputHandler.mouseY, bx, by, BUTTON_WIDTH, BUTTON_HEIGHT))
                {
                    networkServer.startGame();
                    state = GameState.PLAYING;
                    game.enableMultiplayer(true);
                    game.resetRun();
                }
            }
        }

        escapeWasDown = escapeDown;
        enterWasDown = enterDown;
        screenMenu.update();
    }

    private void handleClientInput()
    {
        boolean escapeDown = inputHandler.keys[KeyEvent.VK_ESCAPE];
        boolean upDown = inputHandler.keys[KeyEvent.VK_UP] || inputHandler.keys[KeyEvent.VK_W];
        boolean downDown = inputHandler.keys[KeyEvent.VK_DOWN] || inputHandler.keys[KeyEvent.VK_S];
        boolean enterDown = inputHandler.keys[KeyEvent.VK_ENTER];

        if (upDown && !leftSelectWasDown && !discoveredServers.isEmpty())
        {
            selectedServerIndex = (selectedServerIndex - 1 + discoveredServers.size()) % discoveredServers.size();
        }
        else if (downDown && !rightSelectWasDown && !discoveredServers.isEmpty())
        {
            selectedServerIndex = (selectedServerIndex + 1) % discoveredServers.size();
        }

        if (enterDown && !enterWasDown && !discoveredServers.isEmpty())
        {
            NetworkDiscovery.ServerInfo server = discoveredServers.get(selectedServerIndex);
            connectToServer(server.serverIp, server.port);
        }

        if (escapeDown && !escapeWasDown)
        {
            state = GameState.MULTIPLAYER_MENU;
            if (networkClient != null)
                networkClient.disconnect();
            networkClient = null;
            game.disableMultiplayer();
        }

        if (inputHandler.consumeMouseClick())
        {
            int w = 700;
            int h = 400;
            int x = (WINDOW_WIDTH - w) / 2;
            int y = (WINDOW_HEIGHT - h) / 2;

            int rowX = x + 20;
            int rowY = y + 112;
            int rowW = w - 40;

            int maxRows = Math.min(discoveredServers.size(), 4);
            for (int i = 0; i < maxRows; i++)
            {
                int yRow = rowY + i * (CLIENT_SERVER_ROW_HEIGHT + CLIENT_SERVER_ROW_GAP);
                if (isInside(inputHandler.mouseX, inputHandler.mouseY, rowX, yRow, rowW, CLIENT_SERVER_ROW_HEIGHT))
                {
                    selectedServerIndex = i;
                    break;
                }
            }

            int buttonsY = y + h - 82;
            if (isInside(inputHandler.mouseX, inputHandler.mouseY, x + 20, buttonsY, 220, BUTTON_HEIGHT))
            {
                startClient();
            }
            else if (isInside(inputHandler.mouseX, inputHandler.mouseY, x + 250, buttonsY, 220, BUTTON_HEIGHT) && !discoveredServers.isEmpty())
            {
                NetworkDiscovery.ServerInfo server = discoveredServers.get(selectedServerIndex);
                connectToServer(server.serverIp, server.port);
            }
            else if (isInside(inputHandler.mouseX, inputHandler.mouseY, x + 480, buttonsY, 190, BUTTON_HEIGHT))
            {
                state = GameState.MULTIPLAYER_MENU;
            }
        }

        leftSelectWasDown = upDown;
        rightSelectWasDown = downDown;
        enterWasDown = enterDown;
        escapeWasDown = escapeDown;
        screenMenu.update();
    }

    private void startServer()
    {
        try
        {
            networkServer = new GameNetworkServer((clientId, playerName) ->
            {
                System.out.println("Client connected: " + playerName);
                if (networkServer.getConnectedClients().size() == 1)
                {
                    isServerReady = true;
                }
            }, (clientId) ->
            {
                System.out.println("Client disconnected: " + clientId);
                isServerReady = false;
            });
            networkServer.setPlayerInputListener((clientId, keys) -> game.setRemoteInput(keys));
            networkServer.start();
            state = GameState.NETWORK_SERVER;
            System.out.println("Server started");
        }
        catch (Exception e)
        {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startClient()
    {
        try
        {
            pollServers();
            state = GameState.NETWORK_CLIENT;
        }
        catch (Exception e)
        {
            System.err.println("Error in startClient: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void connectToServer(String serverIp, int port)
    {
        try
        {
            networkClient = new GameNetworkClient((clientId) ->
            {
                System.out.println("Connected to server with ID: " + clientId);
            }, (playerNames, clientIds) ->
            {
                lobbyPlayerNames = playerNames;
                lobbyClientIds = clientIds;
                System.out.println("Player list updated: " + java.util.Arrays.toString(playerNames));
            }, () ->
            {
                System.out.println("Game is starting!");
                state = GameState.PLAYING;
                game.resetRun();
                game.enableMultiplayer(false);
            }, (gameState) ->
            {
                game.applyGameStateFromServer(gameState);
            }, (error) ->
            {
                System.err.println("Server error: " + error);
            }, () ->
            {
                System.out.println("Disconnected from server");
                state = GameState.NETWORK_CLIENT;
                game.disableMultiplayer();
            });

            networkClient.connect(serverIp, port, game.getCurrentPlayerName());
            System.out.println("Connected to server at " + serverIp + ":" + port);
        }
        catch (Exception e)
        {
            System.err.println("Failed to connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private BufferedImage loadBackgroundImage()
    {
        try
        {
            return org.drgnst.game.ResourceLoader.loadImage("image/fondo.png");
        }
        catch (IOException e)
        {
            System.err.println("Error cargando fondo.png: " + e.getMessage());
            return null;
        }
    }

    private void pollServers()
    {
        System.out.println("Scanning for servers on LAN...");
        new Thread(() ->
        {
            discoveredServers = new ArrayList<>(NetworkDiscovery.discoverServers());
            selectedServerIndex = 0;
            System.out.println("Found " + discoveredServers.size() + " servers");
        }).start();
    }

    private NetworkProtocol.GameStateMessage buildServerGameState()
    {
        NetworkProtocol.GameStateMessage stateMessage = new NetworkProtocol.GameStateMessage();
        stateMessage.p1X = game.player.x;
        stateMessage.p1Y = game.player.y;
        stateMessage.p1Angle = game.player.rot;
        stateMessage.p1Health = game.getPlayerHealthPercent();
        stateMessage.p1Ammo = game.getAmmo();
        stateMessage.p1Score = game.getScore();
        stateMessage.p1Kills = game.getKills();

        if (game.getRemotePlayer() != null)
        {
            stateMessage.p2X = game.getRemotePlayer().x;
            stateMessage.p2Y = game.getRemotePlayer().y;
            stateMessage.p2Angle = game.getRemotePlayer().rot;
        }
        else
        {
            stateMessage.p2X = game.player.x;
            stateMessage.p2Y = game.player.y;
            stateMessage.p2Angle = game.player.rot;
        }
        stateMessage.p2Health = game.getPlayerHealthPercent();
        stateMessage.p2Ammo = game.getAmmo();
        stateMessage.p2Score = game.getScore();
        stateMessage.p2Kills = game.getKills();

        stateMessage.numEnemies = game.enemies != null ? game.enemies.size() : 0;
        stateMessage.gameTicks = game.time;
        stateMessage.bossDead = game.getBoss() == null || game.getBoss().isDead();
        return stateMessage;
    }

    private void renderMultiplayerRoleOverlay(Graphics g, int gameX, int gameY)
    {
        String text = game.getLocalRoleLabel() + "   |   " + game.getRemoteRoleLabel();
        g.setFont(g.getFont().deriveFont(20f));
        java.awt.FontMetrics metrics = g.getFontMetrics();
        int w = metrics.stringWidth(text) + 24;
        int h = 34;
        int x = gameX + (GAME_DRAW_WIDTH - w) / 2;
        int y = gameY + 10;

        g.setColor(new java.awt.Color(10, 10, 14, 220));
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(java.awt.Color.WHITE);
        g.drawRoundRect(x, y, w, h, 14, 14);
        g.drawString(text, x + 12, y + 23);
    }
}