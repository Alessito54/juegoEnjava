package org.drgnst.game;

import static java.awt.event.KeyEvent.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.drgnst.game.Level.Level;
import org.drgnst.game.entities.Enemy;
import org.drgnst.game.entities.Player;
import org.drgnst.game.entities.Medkit;
import org.drgnst.game.gfx.Weapon;
import org.drgnst.game.audio.AudioManager;


public class Game
{
    private static final int PLAYER_MAX_HEALTH = 100;
    private static final int SHOT_DAMAGE = 34;
    private static final double SHOT_MAX_DISTANCE = 6.0;
    private static final double SHOT_ANGLE_COS = 0.92;

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

        if (space && !spaceWasDown)
        {
            weapon.fire();
            audioManager.playSoundOnce("/home/alessandro/Java-3D-Rendering/sonidos/disparo.wav");
            shootEnemy();
        }

        spaceWasDown = space;

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

        for (int i = 0; i < enemies.size(); i++)
        {
            Enemy enemy = enemies.get(i);
            enemy.update(player, level);
            enemy.updateAttackAnimation();

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
                enemies.remove(bestTarget);
                kills++;
            }
        }
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
    }

    public int getDeaths()
    {
        return deaths;
    }
    
    /**
     * Limpia recursos cuando el juego termina
     */
    public void cleanup()
    {
        audioManager.stopMusic();
    }
}
