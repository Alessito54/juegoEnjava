package org.drgnst.game.gfx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Arma en primera persona usando sprites base y de disparo.
 */
public class Weapon
{
    private final Bitmap baseSprite;
    private final Bitmap fireSprite;
    private final Bitmap reloadSprite;
    public float bobbingX = 0;
    public float bobbingY = 0;
    private float walkPhase = 0;
    private int fireTimer = 0;
    private boolean reloading = false;

    public Weapon()
    {
        baseSprite = loadSprite("/home/alessandro/Java-3D-Rendering/image/weaponBASE.png");
        fireSprite = loadSprite("/home/alessandro/Java-3D-Rendering/image/weaponDisparo.png");
        reloadSprite = loadSprite("/home/alessandro/Java-3D-Rendering/image/recarga.png");
    }

    public void update(double playerX, double playerY, boolean moving)
    {
        if (moving)
        {
            walkPhase += 0.18f;
        }
        else
        {
            walkPhase *= 0.85f;
        }

        bobbingX = (float) (Math.sin(walkPhase) * 8.0);
        bobbingY = (float) (Math.abs(Math.cos(walkPhase)) * 8.0);

        if (fireTimer > 0)
        {
            fireTimer--;
        }
    }

    public void fire()
    {
        fireTimer = 6;
    }

    public void setReloading(boolean reloading)
    {
        this.reloading = reloading;
        if (reloading)
            fireTimer = 0;
    }

    public void render(Bitmap screen)
    {
        Bitmap sprite;
        if (reloading && reloadSprite != null)
            sprite = reloadSprite;
        else if (fireTimer > 0 && fireSprite != null)
            sprite = fireSprite;
        else
            sprite = baseSprite;

        if (sprite == null)
            return;

        // Las imágenes son 1024x1024, las reducimos a un tamaño visible
        float displayWidth = 150;   // Aumentado para mayor visibilidad
        float displayHeight = 150;  // Aumentado para mayor visibilidad
        
        float scaleX = displayWidth / sprite.width;
        float scaleY = displayHeight / sprite.height;
        float scale = Math.min(scaleX, scaleY);

        int spriteWidth = (int)(sprite.width * scale);
        int spriteHeight = (int)(sprite.height * scale);

        int x0 = screen.width - spriteWidth + (int) bobbingX;
        // Bajar el arma unos píxeles para que no tape la pantalla (~40px)
        final int VERTICAL_OFFSET = 40;
        int y0 = screen.height - spriteHeight + (int) bobbingY + VERTICAL_OFFSET;

        if (fireTimer > 0)
        {
            x0 -= 4;
            y0 -= 5;
        }

        drawScaled(screen, sprite, x0, y0, scale);
    }

    private Bitmap loadSprite(String path)
    {
        try
        {
            BufferedImage image = ImageIO.read(new File(path));
            if (image == null)
            {
                System.err.println("✗ No se pudo cargar sprite (null): " + path);
                return null;
            }

            Bitmap res = new Bitmap(image.getWidth(), image.getHeight());
            image.getRGB(0, 0, res.width, res.height, res.pixels, 0, res.width);
            System.out.println("✓ Sprite cargado: " + path + " (" + res.width + "x" + res.height + ")");
            return res;
        }
        catch (IOException e)
        {
            System.err.println("✗ Error cargando sprite: " + path);
            e.printStackTrace();
            return null;
        }
    }

    private void drawScaled(Bitmap screen, Bitmap source, int x0, int y0, float scale)
    {
        // Si la escala es > 1, amplificamos; si es < 1, reducimos muestrando
        if (scale >= 1.0f)
        {
            // Aumentar escala: dibujar cada píxel múltiples veces
            int intScale = (int) scale;
            for (int y = 0; y < source.height; y++)
            {
                for (int x = 0; x < source.width; x++)
                {
                    int argb = source.pixels[x + y * source.width];
                    int alpha = (argb >>> 24) & 0xff;

                    if (alpha == 0)
                        continue;

                    if (alpha < 255)
                    {
                        int r = (argb >> 16) & 0xff;
                        int g = (argb >> 8) & 0xff;
                        int b = argb & 0xff;
                        argb = (255 << 24) | (r << 16) | (g << 8) | b;
                    }

                    for (int yy = 0; yy < intScale; yy++)
                    {
                        for (int xx = 0; xx < intScale; xx++)
                        {
                            int px = x0 + x * intScale + xx;
                            int py = y0 + y * intScale + yy;

                            if (px < 0 || px >= screen.width || py < 0 || py >= screen.height)
                                continue;

                            screen.pixels[px + py * screen.width] = argb;
                        }
                    }
                }
            }
        }
        else
        {
            // Reducir escala: muestreo inteligente cada N píxeles
            int step = Math.max(1, (int)(1.0f / scale));
            for (int y = 0; y < source.height; y += step)
            {
                for (int x = 0; x < source.width; x += step)
                {
                    int argb = source.pixels[x + y * source.width];
                    int alpha = (argb >>> 24) & 0xff;

                    if (alpha < 128)
                        continue;

                    if (alpha < 255)
                    {
                        int r = (argb >> 16) & 0xff;
                        int g = (argb >> 8) & 0xff;
                        int b = argb & 0xff;
                        argb = (255 << 24) | (r << 16) | (g << 8) | b;
                    }

                    int px = x0 + (int)(x * scale);
                    int py = y0 + (int)(y * scale);

                    if (px >= 0 && px < screen.width && py >= 0 && py < screen.height)
                    {
                        screen.pixels[px + py * screen.width] = argb;
                    }
                }
            }
        }
    }
}
