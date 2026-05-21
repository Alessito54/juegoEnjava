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
    private Bitmap startButtonImage;
    
    // Posición del botón en esquina inferior derecha
    private static final int BUTTON_START_X = 380;
    private static final int BUTTON_START_Y = 300;
    
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
    
    private void renderStartButton(Bitmap screen)
    {
        Bitmap btn = startButtonImage;
        int bx = BUTTON_START_X;
        int by = BUTTON_START_Y;
        
        // Renderizar botón sin escala (usar tamaño original)
        for (int y = 0; y < btn.height && by + y < screen.height; y++)
        {
            for (int x = 0; x < btn.width && bx + x < screen.width; x++)
            {
                int color = btn.pixels[x + y * btn.width];
                int alpha = (color >>> 24) & 0xff;
                
                if (alpha >= 128)  // Solo dibujar si no es transparente
                {
                    screen.pixels[(bx + x) + (by + y) * screen.width] = color & 0xffffff;
                }
            }
        }
    }
    
    public int checkClick(int mouseX, int mouseY, int screenWidth, int screenHeight)
    {
        if (startButtonImage == null)
            return BUTTON_NONE;
        
        // Escalar coordenadas del mouse a resolución del menú (480x360)
        int menuMouseX = (int) (mouseX * 480 / screenWidth);
        int menuMouseY = (int) (mouseY * 360 / screenHeight);
        
        // Verificar si está dentro del área del botón de inicio
        if (menuMouseX >= BUTTON_START_X && menuMouseX < BUTTON_START_X + startButtonImage.width &&
            menuMouseY >= BUTTON_START_Y && menuMouseY < BUTTON_START_Y + startButtonImage.height)
        {
            return BUTTON_START;
        }
        
        return BUTTON_NONE;
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
