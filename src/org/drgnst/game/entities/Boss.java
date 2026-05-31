package org.drgnst.game.entities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.drgnst.game.Level.Level;
import org.drgnst.game.gfx.Bitmap;
import org.drgnst.game.network.NetworkProtocol;

public class Boss
{
    private static Bitmap sprite;
    private static Bitmap attackSprite;
    private static Bitmap deathSprite;
    private static final double FOLLOW_STOP_DISTANCE = 1.4;

    public double x;
    public double y;
    private double speed = 0.025;

    private int health = 420;
    private int maxHealth = 420;
    private int attackCooldown = 45;
    private int attackTimer = 0;
    private int attackDamage = 22;
    private int deathTimer = 180;
    private boolean dying = false;

    public Boss(double x, double y)
    {
        this.x = x;
        this.y = y;
        loadSprite();
    }

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

        return attackTimer > 0 && attackSprite != null ? attackSprite : sprite;
    }

    private static void loadSprite()
    {
        if (sprite != null)
            return;

        try
        {
            BufferedImage image = org.drgnst.game.ResourceLoader.loadImage("image/boss.png");
            if (image != null)
            {
                Bitmap res = new Bitmap(image.getWidth(), image.getHeight());
                image.getRGB(0, 0, res.width, res.height, res.pixels, 0, res.width);
                sprite = res;
                System.out.println("✓ Sprite boss cargado: " + res.width + "x" + res.height);
            }
        }
        catch (IOException e)
        {
            System.err.println("✗ Error cargando boss.png");
        }

        try
        {
            BufferedImage image = org.drgnst.game.ResourceLoader.loadImage("image/bossAtack.png");
            if (image != null)
            {
                Bitmap res = new Bitmap(image.getWidth(), image.getHeight());
                image.getRGB(0, 0, res.width, res.height, res.pixels, 0, res.width);
                attackSprite = res;
                System.out.println("✓ Sprite boss (ataque) cargado: " + res.width + "x" + res.height);
            }
        }
        catch (IOException e)
        {
            // Opcional
        }

        try
        {
            BufferedImage image = org.drgnst.game.ResourceLoader.loadImage("image/bossMuerte.png");
            if (image != null)
            {
                Bitmap res = new Bitmap(image.getWidth(), image.getHeight());
                image.getRGB(0, 0, res.width, res.height, res.pixels, 0, res.width);
                deathSprite = res;
                System.out.println("✓ Sprite boss (muerte) cargado: " + res.width + "x" + res.height);
            }
        }
        catch (IOException e)
        {
            // Opcional
        }
    }

    private boolean isFree(Level level, double xx, double yy)
    {
        double d = 0.45;

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

        if (distance < 1.1 && attackTimer <= 0)
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
            deathTimer = 180;
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

    public void applyNetworkState(NetworkProtocol.BossState state)
    {
        if (state == null)
            return;

        this.x = state.x;
        this.y = state.y;
        this.attackTimer = state.attackTimer;
        this.deathTimer = state.deathTimer;
        this.dying = state.dying;
        this.health = Math.max(0, state.health);
    }

    public int getAttackTimer()
    {
        return attackTimer;
    }

    public int getDeathTimer()
    {
        return deathTimer;
    }

    public boolean isDying()
    {
        return dying;
    }

    public int getHealth()
    {
        return health;
    }
}