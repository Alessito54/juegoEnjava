package org.drgnst.game.gfx;

import java.util.Random;

import org.drgnst.game.Game;

/**
 * Screen and rendering utilities
 */
public class Screen extends Bitmap
{
    public Random r = new Random();

    public Bitmap test;
    public Bitmap3D perspectiveVision;

    public Screen(int width, int height)
    {
        super(width, height);

        test = new Bitmap(50, 50);
        for (int i = 0; i < test.pixels.length; i++)
            test.pixels[i] = r.nextInt();

        perspectiveVision = new Bitmap3D(width, height);
    }

    public void render(Game game)
    {
        if (game == null)
            return;
        clear();
        perspectiveVision.render(game);
        perspectiveVision.renderFog(3);
        render(perspectiveVision, 0, 0);
        
        // Renderizar el arma
        game.weapon.render(this);
        // Kills + Deaths (compact, top-right)
        int hudScale = 1;
        int spacing = 2;
        String ks = Integer.toString(Math.max(0, game.getKills()));
        int kWidth = ks.length() * (3 * hudScale + 1);
        drawNumberRight(width - 6, 6, game.getKills(), 0xffffff, hudScale);
        drawNumberRight(width - 6, 14, game.getDeaths(), 0xff8888, hudScale);

        // Barra de vida más discreta (bottom-left)
        drawHealthBar(6, height - 12, 60, 6, game.getPlayerHealthPercent());

        // Jumpscare a pantalla completa cuando muere
        if (game.getJumpscareTimer() > 0 && game.getJumpscareImage() != null)
        {
            Bitmap jumpscare = game.getJumpscareImage();
            // Escalar la imagen de jumpscare al tamaño de la pantalla
            renderJumpscareFullScreen(jumpscare, width, height);
        }

        // Overlay cuando muere: borde rojo sutil que se desvanece
        if (game.getDeathFlashTimer() > 0)
        {
            double t = game.getDeathFlashTimer() / 30.0; // 0..1
            if (t > 1.0)
                t = 1.0;
            int alpha = (int) (t * 200); // menos intenso
            int thickness = 3;

            // top/bottom border
            for (int y = 0; y < thickness; y++)
            {
                for (int x = 0; x < width; x++)
                {
                    blendPixel(x, y, 0xff0000, alpha);
                    blendPixel(x, height - 1 - y, 0xff0000, alpha);
                }
            }

            // left/right border
            for (int x = 0; x < thickness; x++)
            {
                for (int y = 0; y < height; y++)
                {
                    blendPixel(x, y, 0xff0000, alpha);
                    blendPixel(width - 1 - x, y, 0xff0000, alpha);
                }
            }
        }
    }

    public void update()
    {

    }

    private static final int[][] DIGITS = new int[][] {
        {0b111,0b101,0b101,0b101,0b111}, //0
        {0b010,0b110,0b010,0b010,0b111}, //1
        {0b111,0b001,0b111,0b100,0b111}, //2
        {0b111,0b001,0b111,0b001,0b111}, //3
        {0b101,0b101,0b111,0b001,0b001}, //4
        {0b111,0b100,0b111,0b001,0b111}, //5
        {0b111,0b100,0b111,0b101,0b111}, //6
        {0b111,0b001,0b010,0b010,0b010}, //7
        {0b111,0b101,0b111,0b101,0b111}, //8
        {0b111,0b101,0b111,0b001,0b111}  //9
    };

    private void drawDigit(int ox, int oy, int d, int color, int scale)
    {
        if (d < 0 || d > 9)
            return;
        int[] pat = DIGITS[d];
        for (int ry = 0; ry < 5; ry++)
        {
            int row = pat[ry];
            for (int rx = 0; rx < 3; rx++)
            {
                boolean on = ((row >> (2 - rx)) & 1) == 1;
                if (!on)
                    continue;
                for (int sy = 0; sy < scale; sy++)
                {
                    for (int sx = 0; sx < scale; sx++)
                    {
                        int px = ox + rx * scale + sx;
                        int py = oy + ry * scale + sy;
                        if (px < 0 || px >= width || py < 0 || py >= height)
                            continue;
                        pixels[px + py * width] = color & 0xffffff;
                    }
                }
            }
        }
    }

    private void drawNumber(int x, int y, int value, int color)
    {
        String s = Integer.toString(Math.max(0, value));
        int scale = 2;
        int spacing = 1;
        int dx = 0;
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
                continue;
            int d = c - '0';
            drawDigit(x + dx, y, d, color, scale);
            dx += 3 * scale + spacing;
        }
    }

    private void drawNumberRight(int rightX, int y, int value, int color, int scale)
    {
        String s = Integer.toString(Math.max(0, value));
        int spacing = 1;
        int w = s.length() * (3 * scale + spacing);
        int x = rightX - w;
        int dx = 0;
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
                continue;
            int d = c - '0';
            drawDigit(x + dx, y, d, color, scale);
            dx += 3 * scale + spacing;
        }
    }

    private void drawHealthBar(int x, int y, int w, int h, int percent)
    {
        // Fondo
        fillRect(x - 1, y - 1, w + 2, h + 2, 0x222222);
        fillRect(x, y, w, h, 0x444444);

        int fill = (int) (w * (Math.max(0, Math.min(100, percent)) / 100.0));
        int color;
        if (percent > 60)
            color = 0x28be46; // verde
        else if (percent > 30)
            color = 0xf5b428; // amarillo
        else
            color = 0xdc3c3c; // rojo

        fillRect(x, y, fill, h, color);
    }

    private void blendPixel(int px, int py, int color, int alpha)
    {
        if (px < 0 || px >= width || py < 0 || py >= height)
            return;
        int idx = px + py * width;
        int dst = pixels[idx];
        int dr = (dst >> 16) & 0xff;
        int dg = (dst >> 8) & 0xff;
        int db = dst & 0xff;

        int sr = (color >> 16) & 0xff;
        int sg = (color >> 8) & 0xff;
        int sb = color & 0xff;

        int nr = (dr * (255 - alpha) + sr * alpha) / 255;
        int ng = (dg * (255 - alpha) + sg * alpha) / 255;
        int nb = (db * (255 - alpha) + sb * alpha) / 255;

        pixels[idx] = (nr << 16) | (ng << 8) | nb;
    }

    private void fillRect(int x, int y, int w, int h, int color)
    {
        for (int yy = y; yy < y + h; yy++)
        {
            if (yy < 0 || yy >= height)
                continue;
            for (int xx = x; xx < x + w; xx++)
            {
                if (xx < 0 || xx >= width)
                    continue;
                pixels[xx + yy * width] = color & 0xffffff;
            }
        }
    }

    private void renderJumpscareFullScreen(Bitmap jumpscare, int screenWidth, int screenHeight)
    {
        // Escalar la imagen de jumpscare al tamaño completo de la pantalla
        double scaleX = (double) screenWidth / jumpscare.width;
        double scaleY = (double) screenHeight / jumpscare.height;

        for (int y = 0; y < screenHeight; y++)
        {
            for (int x = 0; x < screenWidth; x++)
            {
                int srcX = (int) (x / scaleX);
                int srcY = (int) (y / scaleY);
                
                // Clamp to bounds
                if (srcX < 0) srcX = 0;
                if (srcX >= jumpscare.width) srcX = jumpscare.width - 1;
                if (srcY < 0) srcY = 0;
                if (srcY >= jumpscare.height) srcY = jumpscare.height - 1;

                if (x < 0 || x >= screenWidth || y < 0 || y >= screenHeight)
                    continue;

                pixels[x + y * screenWidth] = jumpscare.pixels[srcX + srcY * jumpscare.width];
            }
        }
    }
}
