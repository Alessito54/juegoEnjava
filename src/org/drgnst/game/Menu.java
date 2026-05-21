package org.drgnst.game;

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
    private static final int BUTTON_START_X = 160;
    private static final int BUTTON_START_Y = 250;
    private static final int BUTTON_START_W = 80;
    private static final int BUTTON_START_H = 40;
    
    private static final int BUTTON_EXIT_X = 260;
    private static final int BUTTON_EXIT_Y = 250;
    private static final int BUTTON_EXIT_W = 60;
    private static final int BUTTON_EXIT_H = 40;
    
    private int lastClickX = -1;
    private int lastClickY = -1;
    public static final int BUTTON_NONE = 0;
    public static final int BUTTON_START = 1;
    public static final int BUTTON_EXIT = 2;

    public Menu()
    {
        loadMenuImage();
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

        // Dibujar botones
        drawButton(screen, BUTTON_START_X, BUTTON_START_Y, BUTTON_START_W, BUTTON_START_H, "START", 0x28be46);
        drawButton(screen, BUTTON_EXIT_X, BUTTON_EXIT_Y, BUTTON_EXIT_W, BUTTON_EXIT_H, "EXIT", 0xdc3c3c);
    }
    
    public int checkClick(int mouseX, int mouseY, int screenWidth, int screenHeight)
    {
        // Escalar coordenadas del mouse a resolución del menú
        float scaleX = (float) BUTTON_START_W / screenWidth;
        float scaleY = (float) BUTTON_START_H / screenHeight;
        
        int menuMouseX = (int) (mouseX * 3);  // MENU_WIDTH = WIDTH * 3
        int menuMouseY = (int) (mouseY * 3);  // MENU_HEIGHT = HEIGHT * 3
        
        // Verificar botones
        if (menuMouseX >= BUTTON_START_X && menuMouseX < BUTTON_START_X + BUTTON_START_W &&
            menuMouseY >= BUTTON_START_Y && menuMouseY < BUTTON_START_Y + BUTTON_START_H)
        {
            return BUTTON_START;
        }
        
        if (menuMouseX >= BUTTON_EXIT_X && menuMouseX < BUTTON_EXIT_X + BUTTON_EXIT_W &&
            menuMouseY >= BUTTON_EXIT_Y && menuMouseY < BUTTON_EXIT_Y + BUTTON_EXIT_H)
        {
            return BUTTON_EXIT;
        }
        
        return BUTTON_NONE;
    }
    
    private void drawButton(Bitmap screen, int x, int y, int w, int h, String label, int color)
    {
        // Dibujar fondo del botón
        fillRect(screen, x, y, w, h, color);
        
        // Dibujar borde
        drawRect(screen, x - 1, y - 1, w + 2, h + 2, 0xffffff);
    }
    
    private void fillRect(Bitmap screen, int x, int y, int w, int h, int color)
    {
        for (int yy = y; yy < y + h; yy++)
        {
            if (yy < 0 || yy >= screen.height)
                continue;
            for (int xx = x; xx < x + w; xx++)
            {
                if (xx < 0 || xx >= screen.width)
                    continue;
                screen.pixels[xx + yy * screen.width] = color & 0xffffff;
            }
        }
    }
    
    private void drawRect(Bitmap screen, int x, int y, int w, int h, int color)
    {
        // Top and bottom
        for (int xx = x; xx < x + w; xx++)
        {
            if (xx >= 0 && xx < screen.width)
            {
                if (y >= 0 && y < screen.height)
                    screen.pixels[xx + y * screen.width] = color & 0xffffff;
                if (y + h - 1 >= 0 && y + h - 1 < screen.height)
                    screen.pixels[xx + (y + h - 1) * screen.width] = color & 0xffffff;
            }
        }
        
        // Left and right
        for (int yy = y; yy < y + h; yy++)
        {
            if (yy >= 0 && yy < screen.height)
            {
                if (x >= 0 && x < screen.width)
                    screen.pixels[x + yy * screen.width] = color & 0xffffff;
                if (x + w - 1 >= 0 && x + w - 1 < screen.width)
                    screen.pixels[x + w - 1 + yy * screen.width] = color & 0xffffff;
            }
        }
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
                // Mostrar el color directamente sin filtrar por alpha
                screen.pixels[px + py * screen.width] = color & 0xffffff;
            }
        }
    }

    private void drawMenuText(Bitmap screen, int x, int y, String text, int color)
    {
        // Placeholder para texto future
    }
}
