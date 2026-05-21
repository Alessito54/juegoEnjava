package org.drgnst.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.drgnst.game.gfx.Bitmap;

/**
 * Main menu renderer
 */
public class Menu
{
    private Bitmap menuImage;

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

        // Mostrar texto: "Presiona SPACE para continuar"
        drawMenuText(screen, 6, screen.height - 12, "PRESS SPACE TO START", 0xffffff);
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
        // Dibujar texto simple de propósito (letras grandes)
        // Aquí simplemente dejamos espacio para añadir texto si es necesario
        // Por ahora solo lo dejamos como placeholder
    }
}
