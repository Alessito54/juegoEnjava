package org.drgnst.game.Level;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.drgnst.game.gfx.Sprite;
import org.drgnst.game.entities.Medkit;

import java.util.ArrayList;

/**
 * Level loader and container
 */
public class Level
{
    private static Map<String, Level> levels = new HashMap<String, Level>();

    public String name;
    public int width;
    public int height;
    public int[] pixels;
    public Block[] tile;
    public ArrayList<Medkit> medkits;

    public int xSpawn;
    public int ySpawn;

    public Level(String name, int width, int height)
    {
        this.name = name;
        this.width = width;
        this.height = height;
        tile = new Block[width * height];
        this.pixels = new int[width * height];
        this.medkits = new ArrayList<Medkit>();
    }

    public void load()
    {
        for (int i = 0; i < width * height; i++)
            pixels[i] = pixels[i] & 0xffffff;

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                Block block = new Block();

                int type = pixels[x + y * width];

                if (type == 0xFFFFFF)
                {
                    block = new SolidBlock();
                    block.col = 0x667CDB & 0x555555;
                }
                else if (type == 0xFF6B6B)
                {
                    block = new SolidBlock();
                    block.col = 0xFF6B6B & 0x333333;
                }
                else if (type == 0x4ECDC4)
                {
                    block = new SolidBlock();
                    block.col = 0x4ECDC4 & 0x333333;
                }
                else if (type == 0x88FF88)
                {
                    // grass / outdoor floor (non-solid)
                    block.floorCol = 0x88AA66;
                    block.floorTex = 0;
                }
                else if (type == 0xCCCCCC)
                {
                    // classroom floor / tiles
                    block.floorCol = 0x999999;
                    block.floorTex = 0;
                }
                else if (type == 0xFFFF00)
                {
                    xSpawn = x;
                    ySpawn = y;
                }
                else if (type == 0x00FFFF)
                {
                    medkits.add(new Medkit(x + 0.5, y + 0.5));
                }
                else if (type == 0x00FF00)
                {
                    block.addSprite(new Sprite(0, 0, 0, 0, 0x003300));
                }
                else if (type == 0xff00ff)
                {
                    block.ceilCol = 0x550055;
                    block.floorCol = 0x550000;
                }

                tile[x + y * width] = block;
            }
        }
    }

    public Block getBlock(int x, int y)
    {
        if (x < 0 || y < 0 || x >= width || y >= height)
            return new SolidBlock();

        return tile[x + y * width];
    }

    public static Level loadLevel(String name)
    {
        if (levels.containsKey(name))
            return levels.get(name);

        try
        {
            BufferedImage image = ImageIO.read(Level.class.getResourceAsStream("/levels/" + name + ".png"));
            int w = image.getWidth();
            int h = image.getHeight();

            Level res = new Level(name, w, h);
            image.getRGB(0, 0, res.width, res.height, res.pixels, 0, res.width);
            res.load();

            levels.put(name, res);
            return res;
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        // If loading from resource failed, generate a varied level procedurally
        Level gen = createVariedLevel(name);
        levels.put(name, gen);
        return gen;
    }

    private static Level createVariedLevel(String name)
    {
        int w = 64;
        int h = 48;
        Level res = new Level(name, w, h);

        // Fill default floor with dark tiles
        for (int i = 0; i < w * h; i++)
            res.pixels[i] = 0x000000; // default neutral floor

        // Classroom top-left: surrounded by walls and rows of desks
        int cx = 2, cy = 2, cw = 28, ch = 18;
        for (int y = cy; y < cy + ch; y++)
        {
            for (int x = cx; x < cx + cw; x++)
            {
                // walls around
                if (x == cx || x == cx + cw - 1 || y == cy || y == cy + ch - 1)
                {
                    res.pixels[x + y * w] = 0xFFFFFF; // solid wall
                }
                else
                {
                    // desks pattern
                    if (((x - cx) % 4 == 2) && ((y - cy) % 3 != 0))
                        res.pixels[x + y * w] = 0x4ECDC4; // desk / solid
                    else
                        res.pixels[x + y * w] = 0xCCCCCC; // classroom floor
                }
            }
        }

        // Field bottom-right: open grass with some stones/trees
        int fx = 34, fy = 20, fw = 28, fh = 24;
        for (int y = fy; y < fy + fh; y++)
        {
            for (int x = fx; x < fx + fw; x++)
            {
                // mostly grass
                res.pixels[x + y * w] = 0x88FF88;
            }
        }

        // Add scattered solid 'trees' and stones
        for (int i = 0; i < 60; i++)
        {
            int tx = fx + (int) (Math.random() * fw);
            int ty = fy + (int) (Math.random() * fh);
            res.pixels[tx + ty * w] = 0xFF6B6B; // solid obstacle
        }

        // Connect classroom and field with a corridor / broken tiles
        for (int x = cx + cw; x < fx; x++)
        {
            int y = cy + ch / 2;
            res.pixels[x + y * w] = 0xFFFFFF; // corridor walls (edges)
            if (y + 1 < h) res.pixels[x + (y + 1) * w] = 0xCCCCCC;
        }

        // Place spawn in classroom center
        res.pixels[(cx + 4) + (cy + 4) * w] = 0xFFFF00;

        // Place some medkits in field
        for (int i = 0; i < 6; i++)
        {
            int mx = fx + 2 + (int) (Math.random() * (fw - 4));
            int my = fy + 2 + (int) (Math.random() * (fh - 4));
            res.pixels[mx + my * w] = 0x00FFFF;
        }

        res.load();
        return res;
    }
}
