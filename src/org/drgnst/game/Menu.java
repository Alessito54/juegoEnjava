package org.drgnst.game;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.drgnst.game.gfx.Bitmap;

/**
 * Main menu renderer with button support
 */
public class Menu
{
    private Bitmap menuImage;
    private Bitmap startButtonImage;
    private BufferedImage menuImageRaw;
    private BufferedImage startButtonRaw;

    private static final int BUTTON_MARGIN_RIGHT = 22;
    private static final int BUTTON_MARGIN_BOTTOM = 18;
    private static final int BUTTON_DRAW_WIDTH = 220;
    private static final int BUTTON_DRAW_HEIGHT = 72;
    
    public static final int BUTTON_NONE = 0;
    public static final int BUTTON_START = 1;

    public Menu()
    {
        loadMenuImage();
        loadStartButtonImage();
    }

    private void loadStartButtonImage()
    {
        try
        {
            BufferedImage image = ImageIO.read(new File("/home/alessandro/Java-3D-Rendering/image/botonInicio.png"));
            if (image == null)
            {
                System.err.println("✗ No se pudo cargar botonInicio.png");
                return;
            }

            startButtonRaw = image;
            startButtonImage = new Bitmap(image.getWidth(), image.getHeight());
            image.getRGB(0, 0, startButtonImage.width, startButtonImage.height, startButtonImage.pixels, 0, startButtonImage.width);
            System.out.println("✓ Botón inicio cargado: " + startButtonImage.width + "x" + startButtonImage.height);
        }
        catch (IOException e)
        {
            System.err.println("✗ Error cargando botonInicio.png");
            e.printStackTrace();
        }
    }

    private void loadMenuImage()
    {
        try
        {
            BufferedImage image = ImageIO.read(new File("/home/alessandro/Java-3D-Rendering/image/menu.png"));
            if (image == null)
            {
                System.err.println("✗ No se pudo cargar menu.png");
                return;
            }

            menuImageRaw = image;
            menuImage = new Bitmap(image.getWidth(), image.getHeight());
            image.getRGB(0, 0, menuImage.width, menuImage.height, menuImage.pixels, 0, menuImage.width);
            System.out.println("✓ Menu cargado: " + menuImage.width + "x" + menuImage.height);
        }
        catch (IOException e)
        {
            System.err.println("✗ Error cargando menu.png");
            e.printStackTrace();
        }
    }

    public void render(Bitmap screen)
    {
        if (menuImage == null)
        {
            // Fallback: mostrar fondo negro si falla la carga
            for (int i = 0; i < screen.pixels.length; i++)
            {
                screen.pixels[i] = 0x000000;
            }
            return;
        }

        // Escalar la imagen del menu para que ocupe toda la pantalla
        scaleAndRenderMenu(screen, menuImage);

        // Renderizar botón de inicio en esquina inferior derecha
        if (startButtonImage != null)
        {
            renderStartButton(screen);
        }
    }

    public void renderToGraphics(Graphics g, int screenWidth, int screenHeight)
    {
        if (menuImageRaw == null)
        {
            g.setColor(java.awt.Color.BLACK);
            g.fillRect(0, 0, screenWidth, screenHeight);
            return;
        }

        g.drawImage(menuImageRaw, 0, 0, screenWidth, screenHeight, null);

        if (startButtonRaw != null)
        {
            int bx = getButtonX(screenWidth);
            int by = getButtonY(screenHeight);
            g.drawImage(startButtonRaw, bx, by, BUTTON_DRAW_WIDTH, BUTTON_DRAW_HEIGHT, null);
        }
    }
    
    private void renderStartButton(Bitmap screen)
    {
        Bitmap btn = startButtonImage;
        int bx = getButtonX(screen.width);
        int by = getButtonY(screen.height);
        
        // Renderizar botón sin escala (usar tamaño original)
        for (int y = 0; y < btn.height && by + y < screen.height; y++)
        {
            for (int x = 0; x < btn.width && bx + x < screen.width; x++)
            {
                int color = btn.pixels[x + y * btn.width];
                int alpha = (color >>> 24) & 0xff;

                if (alpha == 0)
                    continue;

                int px = bx + x;
                int py = by + y;
                int idx = px + py * screen.width;

                int dst = screen.pixels[idx];
                int dr = (dst >> 16) & 0xff;
                int dg = (dst >> 8) & 0xff;
                int db = dst & 0xff;

                int sr = (color >> 16) & 0xff;
                int sg = (color >> 8) & 0xff;
                int sb = color & 0xff;

                int nr = (dr * (255 - alpha) + sr * alpha) / 255;
                int ng = (dg * (255 - alpha) + sg * alpha) / 255;
                int nb = (db * (255 - alpha) + sb * alpha) / 255;

                screen.pixels[idx] = (nr << 16) | (ng << 8) | nb;
            }
        }
    }
    
    public int checkClick(int mouseX, int mouseY, int screenWidth, int screenHeight)
    {
        if (startButtonRaw == null)
            return BUTTON_NONE;

        int bx = getButtonX(screenWidth);
        int by = getButtonY(screenHeight);

        if (mouseX >= bx && mouseX < bx + BUTTON_DRAW_WIDTH && mouseY >= by && mouseY < by + BUTTON_DRAW_HEIGHT)
        {
            return BUTTON_START;
        }

        return BUTTON_NONE;
    }

    private int getButtonX(int screenWidth)
    {
        return screenWidth - BUTTON_DRAW_WIDTH - BUTTON_MARGIN_RIGHT;
    }

    private int getButtonY(int screenHeight)
    {
        return screenHeight - BUTTON_DRAW_HEIGHT - BUTTON_MARGIN_BOTTOM;
    }

    private void scaleAndRenderMenu(Bitmap screen, Bitmap menu)
    {
        // Llenar pantalla con fondo negro primero
        for (int i = 0; i < screen.pixels.length; i++)
        {
            screen.pixels[i] = 0x000000;
        }

        // Calcular escala para mantener aspecto y llenar la pantalla
        float scaleX = (float) screen.width / menu.width;
        float scaleY = (float) screen.height / menu.height;
        float scale = Math.max(scaleX, scaleY);

        // Dimensiones escaladas
        int scaledW = (int) (menu.width * scale);
        int scaledH = (int) (menu.height * scale);
        int offsetX = (screen.width - scaledW) / 2;
        int offsetY = (screen.height - scaledH) / 2;

        // Renderizar con muestreo simple sin filtro de alpha
        for (int py = 0; py < screen.height; py++)
        {
            for (int px = 0; px < screen.width; px++)
            {
                // Mapear píxel de pantalla a píxel de fuente
                int srcX = (int) ((px - offsetX) / scale);
                int srcY = (int) ((py - offsetY) / scale);

                // Verificar límites
                if (srcX < 0 || srcX >= menu.width || srcY < 0 || srcY >= menu.height)
                    continue;

                int color = menu.pixels[srcX + srcY * menu.width];
                int alpha = (color >>> 24) & 0xff;

                if (alpha == 0)
                    continue;

                int idx = px + py * screen.width;
                if (alpha == 255)
                {
                    screen.pixels[idx] = color & 0xffffff;
                    continue;
                }

                int dst = screen.pixels[idx];
                int dr = (dst >> 16) & 0xff;
                int dg = (dst >> 8) & 0xff;
                int db = dst & 0xff;

                int sr = (color >> 16) & 0xff;
                int sg = (color >> 8) & 0xff;
                int sb = color & 0xff;

                int nr = (dr * (255 - alpha) + sr * alpha) / 255;
                int ng = (dg * (255 - alpha) + sg * alpha) / 255;
                int nb = (db * (255 - alpha) + sb * alpha) / 255;

                screen.pixels[idx] = (nr << 16) | (ng << 8) | nb;
            }
        }
    }

    private void drawMenuText(Bitmap screen, int x, int y, String text, int color)
    {
        // Placeholder para texto future
    }
}
