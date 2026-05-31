package org.drgnst.game.entities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.drgnst.game.Level.Level;
import org.drgnst.game.gfx.Bitmap;

public class Enemy
{
    private static Bitmap sprite;
    private static Bitmap fireSprite;
    private static Bitmap deathSprite;
    private static final double FOLLOW_STOP_DISTANCE = 1.25;

    public double x;
    public double y;
    private double speed = 0.015;

    public Enemy(double x, double y)
    {
        this.x = x;
        this.y = y;
        loadSprite();
    }

    // Salud y comportamiento de ataque
    private int health = 100;
    private int maxHealth = 100;
    private int attackCooldown = 60; // frames
    private int attackTimer = 0;
    private int attackDamage = 10;
    private int deathTimer = 0;
    private boolean dying = false;

    public void update(Player player, Level level)
    {
        if (dying)
            return;

        double dx = player.x - x;
        double dy = player.y - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 0.001)
            return;

        if (distance <= FOLLOW_STOP_DISTANCE)
            return;

        double moveX = dx / distance * speed;
        double moveY = dy / distance * speed;

        if (isFree(level, x + moveX, y))
            x += moveX;
        if (isFree(level, x, y + moveY))
            y += moveY;
    }

    public Bitmap getSprite()
    {
        if (dying && deathSprite != null)
            return deathSprite;

        return attackTimer > 0 && fireSprite != null ? fireSprite : sprite;
    }

    private static void loadSprite()
    {
        if (sprite != null)
            return;

        try
        {
            BufferedImage image = org.drgnst.game.ResourceLoader.loadImage("image/enemigo.png");
            if (image == null)
                return;

            Bitmap res = new Bitmap(image.getWidth(), image.getHeight());
            image.getRGB(0, 0, res.width, res.height, res.pixels, 0, res.width);
            sprite = res;
            System.out.println("✓ Sprite enemigo cargado: " + res.width + "x" + res.height);
        }
        catch (IOException e)
        {
            System.err.println("✗ Error cargando enemigo.png");
            e.printStackTrace();
        }
        // Cargar sprite de disparo (si existe)
        try
        {
            BufferedImage image2 = org.drgnst.game.ResourceLoader.loadImage("image/enemigoDisparo.png");
            if (image2 != null)
            {
                Bitmap res2 = new Bitmap(image2.getWidth(), image2.getHeight());
                image2.getRGB(0, 0, res2.width, res2.height, res2.pixels, 0, res2.width);
                fireSprite = res2;
                System.out.println("✓ Sprite enemigo (disparo) cargado: " + res2.width + "x" + res2.height);
            }
        }
        catch (IOException e)
        {
            // No es crítico si falla, seguir con sprite base
        }

        // Cargar sprite de muerte
        try
        {
            BufferedImage image3 = org.drgnst.game.ResourceLoader.loadImage("image/MUERTE.png");
            if (image3 != null)
            {
                Bitmap res3 = new Bitmap(image3.getWidth(), image3.getHeight());
                image3.getRGB(0, 0, res3.width, res3.height, res3.pixels, 0, res3.width);
                deathSprite = res3;
                System.out.println("✓ Sprite enemigo (muerte) cargado: " + res3.width + "x" + res3.height);
            }
        }
        catch (IOException e)
        {
            // No es crítico si falla, seguir sin sprite de muerte
        }
    }

    private boolean isFree(Level level, double xx, double yy)
    {
        double d = 0.2;

        int x0 = (int) (Math.round(xx - d));
        int x1 = (int) (Math.round(xx + d));
        int y0 = (int) (Math.round(yy - d));
        int y1 = (int) (Math.round(yy + d));

        if (level.getBlock(x0, y0).SOLID_MOTION)
            return false;
        if (level.getBlock(x1, y0).SOLID_MOTION)
            return false;
        if (level.getBlock(x0, y1).SOLID_MOTION)
            return false;
        if (level.getBlock(x1, y1).SOLID_MOTION)
            return false;

        return true;
    }

    public void updateAttackAnimation()
    {
        if (dying)
        {
            if (deathTimer > 0)
                deathTimer--;
            return;
        }

        if (attackTimer > 0)
            attackTimer--;
    }

    public int attackIfReady(Player player)
    {
        if (dying)
            return 0;

        double dx = player.x - x;
        double dy = player.y - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 0.7 && attackTimer <= 0)
        {
            attackTimer = attackCooldown;
            return attackDamage;
        }

        return 0;
    }

    public void takeDamage(int dmg)
    {
        if (dying)
            return;

        health -= dmg;
        if (health < 0)
            health = 0;

        if (health <= 0)
        {
            dying = true;
            deathTimer = 120; // 2 segundos a 60 FPS
            attackTimer = 0;
        }
    }

    public boolean isDead()
    {
        return health <= 0;
    }

    public int getHealthPercent()
    {
        return (int) Math.round((health * 100.0) / maxHealth);
    }

    public boolean isExpired()
    {
        return dying && deathTimer <= 0;
    }
}