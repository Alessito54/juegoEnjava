package org.drgnst.game;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

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
    public static final int MENU_SCALE = SCALE / 3;  // ~1.3x para menú
    public static final String TITLE = "Perspective";
    public static final double FRAME_LIMIT = 60.0;

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
    private boolean menuActive;
    private boolean spaceWasDown;

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
        menuActive = true;
        spaceWasDown = false;
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

        if (menuActive)
        {
            menu.renderToGraphics(g, WINDOW_WIDTH, WINDOW_HEIGHT);
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
        if (menuActive)
        {
            // Detectar SPACE o click en START para iniciar el juego
            if (inputHandler.keys[java.awt.event.KeyEvent.VK_SPACE] && !spaceWasDown)
            {
                menuActive = false;
                spaceWasDown = true;
            }
            spaceWasDown = inputHandler.keys[java.awt.event.KeyEvent.VK_SPACE];
            
            // Detectar click en botón START del menú (evento latched)
            if (inputHandler.consumeMouseClick())
            {
                int buttonClicked = menu.checkClick(inputHandler.mouseX, inputHandler.mouseY, getWidth(), getHeight());
                
                if (buttonClicked == Menu.BUTTON_START)
                {
                    menuActive = false;
                }
            }
            
            screenMenu.update();
        }
        else
        {
            game.update(inputHandler.keys);
            screen.update();
        }
    }

    public void stop()
    {
        if (!isRunning)
            return;

        isRunning = false;
    }

    public void dispose()
    {
        if (game != null)
        {
            game.cleanup();
        }
        System.exit(0);
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
            System.err.println("✗ Error cargando fondo.png: " + e.getMessage());
            return null;
        }
    }
}