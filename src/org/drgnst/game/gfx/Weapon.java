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
        baseSprite = loadSprite("image/weaponBASE.png");
        fireSprite = loadSprite("image/weaponDisparo.png");
        reloadSprite = loadSprite("image/recarga.png");
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
        fireTimer = 10;
    }

    public boolean isFiring()
    {
        return fireTimer > 0;
    }

    public void setReloading(boolean reloading)
    {
        this.reloading = reloading;
        if (reloading)
            fireTimer = 0;
    }

    public void render(Bitmap screen)
    {
        boolean firing = fireTimer > 0;
        Bitmap sprite;
        if (reloading && reloadSprite != null)
            sprite = reloadSprite;
        else if (firing && fireSprite != null)
            sprite = fireSprite;
        else
            sprite = baseSprite;

        if (sprite == null)
        {
            drawFallbackWeapon(screen, firing, reloading);
            return;
        }

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

        if (firing)
        {
            x0 -= 4;
            y0 -= 5;
        }

        drawScaled(screen, sprite, x0, y0, scale);

        if (firing)
            drawMuzzleFlash(screen, x0 + spriteWidth - 10, y0 + spriteHeight / 3);
        else if (reloading)
            drawReloadGlow(screen, x0 + spriteWidth / 2, y0 + spriteHeight / 2);

        // Refuerzo visual para que el arma siempre sea visible en cualquier cliente/host
        drawWeaponFrame(screen, x0, y0, spriteWidth, spriteHeight, firing, reloading);
    }

    private void drawFallbackWeapon(Bitmap screen, boolean firing, boolean reloading)
    {
        int w = 120;
        int h = 48;
        int x0 = screen.width - w - 18 + (int) bobbingX;
        int y0 = screen.height - h - 28 + (int) bobbingY;

        int body = reloading ? 0x7d5a22 : 0x3a3a44;
        int accent = firing ? 0xffb000 : 0x9c9ca8;
        fillRect(screen, x0, y0, w, h, body);
        fillRect(screen, x0 + 10, y0 + 10, w - 24, h - 20, accent);

        if (firing)
            drawMuzzleFlash(screen, x0 + w - 8, y0 + h / 2);
        if (reloading)
            drawReloadGlow(screen, x0 + w / 2, y0 + h / 2);
    }

    private void drawMuzzleFlash(Bitmap screen, int cx, int cy)
    {
        int flashColor = 0xffd75a;
        fillRect(screen, cx - 8, cy - 3, 18, 6, flashColor);
        fillRect(screen, cx - 2, cy - 8, 6, 16, 0xfff0a0);
    }

    private void drawReloadGlow(Bitmap screen, int cx, int cy)
    {
        fillRect(screen, cx - 6, cy - 6, 12, 12, 0x4ecbff);
    }

    private void drawWeaponFrame(Bitmap screen, int x, int y, int w, int h, boolean firing, boolean reloading)
    {
        int outline = reloading ? 0x4ecbff : (firing ? 0xffd75a : 0x2a2a32);
        int inner = reloading ? 0x15465a : (firing ? 0x7f5a00 : 0x454553);

        // marco principal
        fillRect(screen, x + w - 92, y + h - 52, 74, 22, outline);
        fillRect(screen, x + w - 84, y + h - 46, 54, 10, inner);
        // cañón
        fillRect(screen, x + w - 20, y + h - 48, 18, 6, outline);
        fillRect(screen, x + w - 16, y + h - 46, 10, 2, inner);
        // empuñadura
        fillRect(screen, x + w - 70, y + h - 34, 12, 20, inner);
    }

    private void fillRect(Bitmap screen, int x, int y, int w, int h, int color)
    {
        for (int yy = 0; yy < h; yy++)
        {
            int py = y + yy;
            if (py < 0 || py >= screen.height)
                continue;
            for (int xx = 0; xx < w; xx++)
            {
                int px = x + xx;
                if (px < 0 || px >= screen.width)
                    continue;
                screen.pixels[px + py * screen.width] = color;
            }
        }
    }

    private Bitmap loadSprite(String path)
    {
        try
        {
            BufferedImage image = org.drgnst.game.ResourceLoader.loadImage(path);
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
