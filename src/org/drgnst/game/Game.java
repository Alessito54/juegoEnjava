package org.drgnst.game;

import static java.awt.event.KeyEvent.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

import org.drgnst.game.Level.Level;
import org.drgnst.game.entities.Enemy;
import org.drgnst.game.entities.Player;
import org.drgnst.game.entities.Medkit;
import org.drgnst.game.gfx.Weapon;
import org.drgnst.game.gfx.Bitmap;
import org.drgnst.game.audio.AudioManager;


public class Game
{
    private static final int PLAYER_MAX_HEALTH = 100;
    private static final int SHOT_DAMAGE = 34;
    private static final double SHOT_MAX_DISTANCE = 6.0;
    private static final double SHOT_ANGLE_COS = 0.92;
    private static final int MAX_AMMO = 5;
    private static final int RELOAD_DURATION_FRAMES = 343; // ~5.72s, igual al audio

    public Level level;
    public Player player;
    public List<Enemy> enemies;
    public Weapon weapon;
    public AudioManager audioManager;
    public int time;
    private boolean spaceWasDown;
    private Random random;
    private int enemySpawnTimer;
    private int playerHealth;
    private int kills;
    private int deaths;
    private int deathFlashTimer;
    private Bitmap jumpscareImage;
    private int jumpscareTimer;
    private int firingCooldown; // Cooldown entre disparos
    private int ammo;
    private int reloadTimer;

    public Game()
    {
        level = Level.loadLevel("level0");
        player = new Player(this);
        enemies = new ArrayList<Enemy>();
        weapon = new Weapon();
        audioManager = new AudioManager();
        random = new Random();
        enemySpawnTimer = 60;
        playerHealth = PLAYER_MAX_HEALTH;
        kills = 0;
        deaths = 0;
        jumpscareTimer = 0;
        firingCooldown = 0;
        ammo = MAX_AMMO;
        reloadTimer = 0;
        weapon.setReloading(false);
        loadJumpscareImage();

        // Iniciar música de fondo
        audioManager.playBackgroundMusic("/home/alessandro/Java-3D-Rendering/DoomEternalOST.wav");
    }

    public void update(boolean[] keys)
    {
        time++;

        boolean up = keys[VK_W];
        boolean down = keys[VK_S];
        boolean left = keys[VK_A];
        boolean right = keys[VK_D];
        boolean turnLeft = keys[VK_Q];
        boolean turnRight = keys[VK_E];
        boolean space = keys[VK_SPACE];
        boolean moving = up || down || left || right;


        player.update(up, down, left, right, turnLeft, turnRight, space);
        weapon.update(player.x, player.y, moving);
        updateMedkits();
        updateEnemies();

        if (reloadTimer > 0)
        {
            reloadTimer--;
            if (reloadTimer <= 0)
            {
                ammo = MAX_AMMO;
                weapon.setReloading(false);
                System.out.println("✓ Recarga completa");
            }
        }

        if (space && !spaceWasDown && firingCooldown <= 0 && reloadTimer <= 0 && ammo > 0)
        {
            weapon.fire();
            audioManager.playSoundOnce("/home/alessandro/Java-3D-Rendering/sonidos/disparo.wav");
            shootEnemy();
            firingCooldown = 60; // 1 segundo de cooldown a 60 FPS
            ammo--;

            if (ammo <= 0)
                startReload();
        }

        spaceWasDown = space;
        
        // Reducir cooldown de disparo
        if (firingCooldown > 0)
            firingCooldown--;

        // Si el jugador muere, registrar muerte y respawnear en spawn
        if (playerHealth <= 0)
        {
            recordDeath();
            deathFlashTimer = 30;
            player.x = level.xSpawn;
            player.y = level.ySpawn;
            playerHealth = PLAYER_MAX_HEALTH;
            System.out.println("✗ Has muerto. Respawn en spawn.");
        }

        if (deathFlashTimer > 0)
            deathFlashTimer--;

        if (jumpscareTimer > 0)
            jumpscareTimer--;
    }

    public int getDeathFlashTimer()
    {
        return deathFlashTimer;
    }

    private void updateMedkits()
    {
        for (int i = level.medkits.size() - 1; i >= 0; i--)
        {
            Medkit medkit = level.medkits.get(i);
            medkit.checkPickup(player);

            if (medkit.isCollected())
            {
                int healed = Math.min(medkit.getHealAmount(), PLAYER_MAX_HEALTH - playerHealth);
                if (healed > 0)
                {
                    playerHealth += healed;
                    System.out.println("✓ Botiquín recogido: " + healed + "% de vida restaurada");
                }
                level.medkits.remove(i);
            }
        }
    }
    private void updateEnemies()
    {
        if (enemySpawnTimer <= 0)
        {
            if (enemies.size() < 8)
                spawnEnemyNearPlayer();
            enemySpawnTimer = 180;
        }
        else
        {
            enemySpawnTimer--;
        }

        for (int i = enemies.size() - 1; i >= 0; i--)
        {
            Enemy enemy = enemies.get(i);
            enemy.updateAttackAnimation();

            if (enemy.isExpired())
            {
                enemies.remove(i);
                continue;
            }

            if (!enemy.isDead())
            {
                enemy.update(player, level);
            }

            int damage = enemy.attackIfReady(player);
            if (damage > 0)
            {
                playerHealth -= damage;
                if (playerHealth < 0)
                    playerHealth = 0;
            }
        }
    }

    private void shootEnemy()
    {
        Enemy bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        double lookX = -Math.sin(player.rot);
        double lookY = Math.cos(player.rot);

        for (int i = 0; i < enemies.size(); i++)
        {
            Enemy enemy = enemies.get(i);
            if (enemy.isDead())
                continue;

            double dx = enemy.x - player.x;
            double dy = enemy.y - player.y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > SHOT_MAX_DISTANCE || dist < 0.001)
                continue;

            double dirX = dx / dist;
            double dirY = dy / dist;
            double dot = dirX * lookX + dirY * lookY;

            if (dot < SHOT_ANGLE_COS)
                continue;

            // Verificar línea de vista: no disparar a través de paredes
            if (!hasLineOfSight(player.x, player.y, enemy.x, enemy.y))
                continue;

            if (dist < bestDistance)
            {
                bestDistance = dist;
                bestTarget = enemy;
            }
        }

        if (bestTarget != null)
        {
            bestTarget.takeDamage(SHOT_DAMAGE);
            if (bestTarget.isDead())
            {
                kills++;
            }
        }
    }

    private void startReload()
    {
        if (reloadTimer > 0)
            return;

        reloadTimer = RELOAD_DURATION_FRAMES;
        weapon.setReloading(true);
        audioManager.playSoundOnce("/home/alessandro/Java-3D-Rendering/sonidos/recarga.wav");
        System.out.println("↻ Recargando arma...");
    }

    private void spawnEnemyNearPlayer()
    {
        for (int i = 0; i < 30; i++)
        {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 2.0 + random.nextDouble() * 4.0;

            double ex = player.x + Math.cos(angle) * distance;
            double ey = player.y + Math.sin(angle) * distance;

            if (isSpawnFree(ex, ey))
            {
                enemies.add(new Enemy(ex, ey));
                return;
            }
        }
    }

    private boolean isSpawnFree(double ex, double ey)
    {
        int bx = (int) Math.round(ex);
        int by = (int) Math.round(ey);

        if (level.getBlock(bx, by).SOLID_MOTION)
            return false;

        double dx = ex - player.x;
        double dy = ey - player.y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        return dist > 1.6;
    }

    public int getPlayerHealthPercent()
    {
        return (int) Math.round((playerHealth * 100.0) / PLAYER_MAX_HEALTH);
    }

    public boolean isPlayerDead()
    {
        return playerHealth <= 0;
    }

    public int getKills()
    {
        return kills;
    }

    public void recordDeath()
    {
        deaths++;
        jumpscareTimer = 180; // Mostrar jumpscare durante 3 segundos (180 frames a 60 FPS)
        // Reproducir jumpscare con delay para que se escuche
        new Thread(() -> {
            try {
                audioManager.playSoundOnce("/home/alessandro/Java-3D-Rendering/sonidos/jumpscare.wav");
            } catch (Exception e) {
                System.err.println("Error reproduciendo jumpscare: " + e.getMessage());
            }
        }).start();
        System.out.println("☠ ¡JUMPSCARE! Sonido y imagen activados por 3 segundos");
    }

    public int getDeaths()
    {
        return deaths;
    }

    public int getAmmo()
    {
        return ammo;
    }

    public int getMaxAmmo()
    {
        return MAX_AMMO;
    }

    public boolean isReloading()
    {
        return reloadTimer > 0;
    }

    public int getJumpscareTimer()
    {
        return jumpscareTimer;
    }

    public Bitmap getJumpscareImage()
    {
        return jumpscareImage;
    }

    private void loadJumpscareImage()
    {
        try
        {
            BufferedImage img = ImageIO.read(new File("/home/alessandro/Java-3D-Rendering/image/jumpscare.png"));
            jumpscareImage = new Bitmap(img.getWidth(), img.getHeight());
            for (int y = 0; y < img.getHeight(); y++)
            {
                for (int x = 0; x < img.getWidth(); x++)
                {
                    jumpscareImage.pixels[x + y * img.getWidth()] = img.getRGB(x, y) & 0xffffff;
                }
            }
            System.out.println("✓ Jumpscare cargado: " + img.getWidth() + "x" + img.getHeight());
        }
        catch (Exception e)
        {
            System.out.println("✗ Error al cargar jumpscare: " + e.getMessage());
            jumpscareImage = null;
        }
    }
    
    private boolean hasLineOfSight(double x1, double y1, double x2, double y2)
    {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 0.1)
            return true;

        // Raycast en pasos pequeños
        int steps = (int) (dist * 4); // 4 muestras por unidad
        for (int i = 0; i <= steps; i++)
        {
            double t = (i == 0) ? 0 : (double) i / steps;
            double px = x1 + dx * t;
            double py = y1 + dy * t;

            int bx = (int) Math.round(px);
            int by = (int) Math.round(py);

            if (level.getBlock(bx, by).SOLID_MOTION)
                return false; // Hay pared en el camino
        }

        return true; // Línea de vista clara
    }

    /**
     * Limpia recursos cuando el juego termina
     */
    public void cleanup()
    {
        audioManager.stopMusic();
    }
}
