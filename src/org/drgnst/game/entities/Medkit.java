package org.drgnst.game.entities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.drgnst.game.gfx.Bitmap;

public class Medkit
{
    private static Bitmap sprite;
    private static final int HEAL_AMOUNT = 35;
    private static final double PICKUP_RANGE = 0.4;

    public double x;
    public double y;
    private boolean collected = false;

    public Medkit(double x, double y)
    {
        this.x = x;
        this.y = y;
        loadSprite();
    }

    public void checkPickup(Player player)
    {
        if (collected)
            return;

        double dx = player.x - x;
        double dy = player.y - y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist <= PICKUP_RANGE)
            collected = true;
    }

    public boolean isCollected()
    {
        return collected;
    }

    public int getHealAmount()
    {
        return HEAL_AMOUNT;
    }

    public Bitmap getSprite()
    {
        return sprite;
    }

    private static void loadSprite()
    {
        if (sprite != null)
            return;

        try
        {
            BufferedImage image = org.drgnst.game.ResourceLoader.loadImage("image/botiquin.png");
            if (image == null)
                return;

            Bitmap res = new Bitmap(image.getWidth(), image.getHeight());
            image.getRGB(0, 0, res.width, res.height, res.pixels, 0, res.width);
            sprite = res;
            System.out.println("✓ Sprite botiquín cargado: " + res.width + "x" + res.height);
        }
        catch (IOException e)
        {
            System.err.println("✗ Error cargando botiquin.png");
            e.printStackTrace();
        }
    }
}
