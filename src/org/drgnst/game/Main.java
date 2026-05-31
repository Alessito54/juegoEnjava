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

    private enum GameState
    {
        MENU,
        CREATE_PLAYER,
        SELECT_PLAYER,
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

        if (state == GameState.MENU || state == GameState.CREATE_PLAYER || state == GameState.SELECT_PLAYER || state == GameState.PAUSED)
        {
            if (state == GameState.MENU || state == GameState.CREATE_PLAYER || state == GameState.SELECT_PLAYER)
            {
                menu.renderToGraphics(g, WINDOW_WIDTH, WINDOW_HEIGHT);
                if (state == GameState.MENU)
                    renderMainMenuOverlay(g);
                else if (state == GameState.CREATE_PLAYER)
                    renderCreatePlayerOverlay(g);
                else
                    renderSelectPlayerOverlay(g);
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
        else if (state == GameState.PAUSED)
        {
            handlePauseInput();
        }
        else
        {
            game.update(inputHandler.keys);
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
        int h = 420;
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
        int h = 420;
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

    private BufferedImage loadBackgroundImage()
    {
        try
        {
            return ImageIO.read(new File("/home/alessandro/Java-3D-Rendering/image/fondo.png"));
        }
        catch (IOException e)
        {
            System.err.println("Error cargando fondo.png: " + e.getMessage());
            return null;
        }
    }
}