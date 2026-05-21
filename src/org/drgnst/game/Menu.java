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
        // Calcular factor de escala
        float scaleX = (float) screen.width / menu.width;
        float scaleY = (float) screen.height / menu.height;
        float scale = Math.min(scaleX, scaleY);

        // Centrar la imagen escalada
        int scaledW = (int) (menu.width * scale);
        int scaledH = (int) (menu.height * scale);
        int offsetX = (screen.width - scaledW) / 2;
        int offsetY = (screen.height - scaledH) / 2;

        // Renderizar con escala
        for (int sy = 0; sy < menu.height; sy++)
        {
            for (int sx = 0; sx < menu.width; sx++)
            {
                int color = menu.pixels[sx + sy * menu.width];
                int alpha = (color >>> 24) & 0xff;

                if (alpha < 128)
                    continue;

                int intScale = (int) scale;
                for (int yy = 0; yy < intScale; yy++)
                {
                    for (int xx = 0; xx < intScale; xx++)
                    {
                        int px = offsetX + sx * intScale + xx;
                        int py = offsetY + sy * intScale + yy;

                        if (px >= 0 && px < screen.width && py >= 0 && py < screen.height)
                        {
                            screen.pixels[px + py * screen.width] = color & 0xffffff;
                        }
                    }
                }
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
