package org.drgnst.game;

import static java.awt.event.KeyEvent.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.drgnst.game.Level.Level;
import org.drgnst.game.entities.Boss;
import org.drgnst.game.entities.Enemy;
import org.drgnst.game.entities.Player;
import org.drgnst.game.entities.Medkit;
import org.drgnst.game.gfx.Weapon;
import org.drgnst.game.gfx.Bitmap;
import org.drgnst.game.audio.AudioManager;
import org.drgnst.game.network.NetworkProtocol;


public class Game
{
    private static final int PLAYER_MAX_HEALTH = 100;
    private static final int SHOT_DAMAGE = 34;
    private static final double SHOT_MAX_DISTANCE = 6.0;
    private static final double SHOT_ANGLE_COS = 0.92;
    private static final int MAX_AMMO = 5;
    private static final int RELOAD_DURATION_FRAMES = 120; // ~2.0s
    private static final int SCORE_PER_KILL = 100;
    private static final int SCORE_PER_BOSS_KILL = 1000;
    private static final int BOSS_TRIGGER_KILLS = 5;
    private static final String NORMAL_BACKGROUND_MUSIC = "DoomEternalOST.wav";
    private static final String BOSS_BACKGROUND_MUSIC = "sonidos/bossSound.wav";
    private static final Path SCORE_FILE = Paths.get("res/score.json");
    private static final Path PLAYER_FILE = Paths.get("res/players.json");
    private static final String DEFAULT_PLAYER_NAME = "Jugador";

    public Level level;
    public Player player;
    public Player player2;
    public List<Enemy> enemies;
    private Boss boss;
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
    private int score;
    private int maxScore;
    private String currentPlayerName;
    private int currentPlayerBestScore;
    private String topPlayerName;
    private boolean bossMusicPlaying;
    private boolean bossDefeatedRewarded;
    private boolean bossSpawnedOnce;
    private final Map<String, PlayerProfile> playerProfiles;
    private volatile boolean[] remoteKeys = new boolean[65535];
    private volatile boolean multiplayerEnabled = false;
    private volatile boolean localIsServer = true;
    private volatile String localRoleLabel = "JUGADOR 1 (SERVIDOR)";
    private volatile String remoteRoleLabel = "JUGADOR 2 (CLIENTE)";

    public Game()
    {
        playerProfiles = new LinkedHashMap<String, PlayerProfile>();
        level = Level.loadLevel("level0");
        enemies = new ArrayList<Enemy>();
        weapon = new Weapon();
        audioManager = new AudioManager();
        random = new Random();
        enemySpawnTimer = 60;
        kills = 0;
        deaths = 0;
        jumpscareTimer = 0;
        firingCooldown = 0;
        ammo = MAX_AMMO;
        reloadTimer = 0;
        weapon.setReloading(false);
        score = 0;
        loadJumpscareImage();
        loadPlayerProfiles();
        selectPlayer(currentPlayerName);
        resetRunState();
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

        if (multiplayerEnabled && player2 != null)
        {
            boolean[] rk = remoteKeys;
            if (rk != null && rk.length > VK_SPACE)
            {
                boolean rup = rk[VK_W];
                boolean rdown = rk[VK_S];
                boolean rleft = rk[VK_A];
                boolean rright = rk[VK_D];
                boolean rturnLeft = rk[VK_Q];
                boolean rturnRight = rk[VK_E];
                boolean rspace = rk[VK_SPACE];
                player2.update(rup, rdown, rleft, rright, rturnLeft, rturnRight, rspace);
            }
        }

        weapon.update(player.x, player.y, moving);
        updateMedkits();
        updateEnemies();
        updateBoss();

        if (playerHealth <= 0)
        {
            handlePlayerDeath();
            return;
        }

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
            audioManager.playSoundOnce("sonidos/disparo.wav");
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
            if ((boss == null || boss.isExpired()) && enemies.size() < 8)
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

    private void updateBoss()
    {
        if (!bossSpawnedOnce && boss == null && kills >= BOSS_TRIGGER_KILLS)
            spawnBossNearPlayer();

        if (boss == null)
            return;

        boss.updateAttackAnimation();

        if (boss.isDead())
        {
            if (!bossDefeatedRewarded)
                handleBossDefeat();

            if (boss.isExpired())
                boss = null;

            return;
        }

        boss.update(player, level);

        int damage = boss.attackIfReady(player);
        if (damage > 0)
        {
            playerHealth -= damage;
            if (playerHealth < 0)
                playerHealth = 0;
        }
    }

    private void shootEnemy()
    {
        Enemy bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        boolean bestTargetIsBoss = false;

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
                bestTargetIsBoss = false;
            }
        }

        if (boss != null && !boss.isDead())
        {
            double dx = boss.x - player.x;
            double dy = boss.y - player.y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist <= SHOT_MAX_DISTANCE && dist >= 0.001)
            {
                double dirX = dx / dist;
                double dirY = dy / dist;
                double dot = dirX * lookX + dirY * lookY;

                if (dot >= SHOT_ANGLE_COS && hasLineOfSight(player.x, player.y, boss.x, boss.y) && dist < bestDistance)
                {
                    bestDistance = dist;
                    bestTarget = null;
                    bestTargetIsBoss = true;
                }
            }
        }

        if (bestTargetIsBoss && boss != null)
        {
            boss.takeDamage(SHOT_DAMAGE);
            if (boss.isDead())
            {
                kills++;
                addScore(SCORE_PER_BOSS_KILL);
                handleBossDefeat();
            }
        }
        else if (bestTarget != null)
        {
            bestTarget.takeDamage(SHOT_DAMAGE);
            if (bestTarget.isDead())
            {
                kills++;
                addScore(SCORE_PER_KILL);
            }
        }
    }

    private void startReload()
    {
        if (reloadTimer > 0)
            return;

        reloadTimer = RELOAD_DURATION_FRAMES;
        weapon.setReloading(true);
        audioManager.playSoundOnce("sonidos/recarga.wav");
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

    private void spawnBossNearPlayer()
    {
        for (int i = 0; i < 40; i++)
        {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 4.0 + random.nextDouble() * 4.0;

            double bx = player.x + Math.cos(angle) * distance;
            double by = player.y + Math.sin(angle) * distance;

            if (isSpawnFree(bx, by))
            {
                boss = new Boss(bx, by);
                bossSpawnedOnce = true;
                bossDefeatedRewarded = false;
                startBossMusic();
                System.out.println("☠ Boss generado tras " + BOSS_TRIGGER_KILLS + " bajas");
                return;
            }
        }
    }

    private void startBossMusic()
    {
        if (bossMusicPlaying)
            return;

        audioManager.stopMusic();
        audioManager.playBackgroundMusic(BOSS_BACKGROUND_MUSIC);
        bossMusicPlaying = true;
    }

    private void restoreNormalMusic()
    {
        if (!bossMusicPlaying)
            return;

        audioManager.stopMusic();
        audioManager.playBackgroundMusic(NORMAL_BACKGROUND_MUSIC);
        bossMusicPlaying = false;
    }

    private void handleBossDefeat()
    {
        bossDefeatedRewarded = true;
        restoreNormalMusic();
        System.out.println("✓ Boss derrotado");
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

    public Boss getBoss()
    {
        return boss;
    }

    public int getScore()
    {
        return score;
    }

    public int getMaxScore()
    {
        return maxScore;
    }

    public String getCurrentPlayerName()
    {
        return currentPlayerName;
    }

    public int getCurrentPlayerBestScore()
    {
        return currentPlayerBestScore;
    }

    public String getTopPlayerName()
    {
        return topPlayerName;
    }

    public List<String> getPlayerNames()
    {
        return new ArrayList<String>(playerProfiles.keySet());
    }

    public int getPlayerBestScore(String playerName)
    {
        PlayerProfile profile = playerProfiles.get(sanitizePlayerName(playerName));
        return profile == null ? 0 : profile.maxScore;
    }

    public int getTopPlayerScore()
    {
        return maxScore;
    }

    public void selectPlayer(String playerName)
    {
        String sanitizedName = sanitizePlayerName(playerName);
        if (sanitizedName.isEmpty())
            sanitizedName = DEFAULT_PLAYER_NAME;

        PlayerProfile profile = getOrCreateProfile(sanitizedName);
        currentPlayerName = sanitizedName;
        currentPlayerBestScore = profile.maxScore;
        recalculateTopPlayer();
        savePlayerProfiles();
    }

    public void resetRun()
    {
        resetRunState();
    }

    public void recordDeath()
    {
        handlePlayerDeath();
    }

    public void syncInputState(boolean[] keys)
    {
        if (keys == null)
            return;

        if (VK_SPACE >= 0 && VK_SPACE < keys.length)
            spaceWasDown = keys[VK_SPACE];
    }

    private void handlePlayerDeath()
    {
        deaths++;
        updateCurrentPlayerBestScore(score);
        saveMaxScore();
        savePlayerProfiles();
        resetRunState();
        System.out.println("☠ Has muerto. Reiniciando partida completa.");
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
            BufferedImage img = org.drgnst.game.ResourceLoader.loadImage("image/jumpscare.png");
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
        savePlayerProfiles();
        saveMaxScore();
        audioManager.stopMusic();
    }

    public void enableMultiplayer(boolean localAsServer)
    {
        multiplayerEnabled = true;
        localIsServer = localAsServer;
        if (player2 == null)
            player2 = new Player(this);

        if (localAsServer)
        {
            localRoleLabel = "JUGADOR 1 (SERVIDOR)";
            remoteRoleLabel = "JUGADOR 2 (CLIENTE)";
        }
        else
        {
            localRoleLabel = "JUGADOR 2 (CLIENTE)";
            remoteRoleLabel = "JUGADOR 1 (SERVIDOR)";
        }
    }

    public void disableMultiplayer()
    {
        multiplayerEnabled = false;
        player2 = null;
        remoteKeys = new boolean[65535];
        localRoleLabel = "JUGADOR 1 (SERVIDOR)";
        remoteRoleLabel = "JUGADOR 2 (CLIENTE)";
    }

    public void setRemoteInput(boolean[] keys)
    {
        if (keys == null)
            return;

        boolean[] copy = new boolean[Math.max(65535, keys.length)];
        System.arraycopy(keys, 0, copy, 0, keys.length);
        remoteKeys = copy;
    }

    public void applyGameStateFromServer(NetworkProtocol.GameStateMessage state)
    {
        if (state == null)
            return;

        if (player == null)
            player = new Player(this);
        if (player2 == null)
            player2 = new Player(this);

        if (localIsServer)
        {
            player.x = state.p1X;
            player.y = state.p1Y;
            player.rot = state.p1Angle;
            player2.x = state.p2X;
            player2.y = state.p2Y;
            player2.rot = state.p2Angle;
        }
        else
        {
            player.x = state.p2X;
            player.y = state.p2Y;
            player.rot = state.p2Angle;
            player2.x = state.p1X;
            player2.y = state.p1Y;
            player2.rot = state.p1Angle;
        }
    }

    public boolean isMultiplayerEnabled()
    {
        return multiplayerEnabled;
    }

    public String getLocalRoleLabel()
    {
        return localRoleLabel;
    }

    public String getRemoteRoleLabel()
    {
        return remoteRoleLabel;
    }

    public Player getRemotePlayer()
    {
        return player2;
    }

    private void addScore(int points)
    {
        if (points <= 0)
            return;

        score += points;
        updateCurrentPlayerBestScore(score);
        if (score > maxScore)
        {
            maxScore = score;
            saveMaxScore();
        }
    }

    private int loadMaxScore()
    {
        try
        {
            if (!Files.exists(SCORE_FILE))
            {
                saveMaxScore(0);
                return 0;
            }

            String content = new String(Files.readAllBytes(SCORE_FILE), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("\"maxScore\"\\s*:\\s*(\\d+)").matcher(content);
            if (matcher.find())
                return Integer.parseInt(matcher.group(1));
        }
        catch (Exception e)
        {
            System.err.println("✗ Error cargando maxScore: " + e.getMessage());
        }

        return 0;
    }

    private void saveMaxScore()
    {
        saveMaxScore(maxScore);
    }

    private void saveMaxScore(int value)
    {
        try
        {
            if (SCORE_FILE.getParent() != null)
                Files.createDirectories(SCORE_FILE.getParent());

            String json = "{\n  \"maxScore\": " + Math.max(0, value) + "\n}\n";
            Files.write(SCORE_FILE, json.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            System.err.println("✗ Error guardando maxScore: " + e.getMessage());
        }
    }

    private void resetRunState()
    {
        level = Level.loadLevel("level0");
        player = new Player(this);
        if (multiplayerEnabled)
            player2 = new Player(this);
        else
            player2 = null;
        enemies = new ArrayList<Enemy>();
        boss = null;
        bossMusicPlaying = false;
        bossDefeatedRewarded = false;
        bossSpawnedOnce = false;
        enemySpawnTimer = 60;
        time = 0;
        playerHealth = PLAYER_MAX_HEALTH;
        kills = 0;
        score = 0;
        firingCooldown = 0;
        ammo = MAX_AMMO;
        reloadTimer = 0;
        deathFlashTimer = 0;
        jumpscareTimer = 0;
        spaceWasDown = false;
        weapon.setReloading(false);

        audioManager.stopMusic();
        audioManager.playBackgroundMusic(NORMAL_BACKGROUND_MUSIC);
    }

    private void loadPlayerProfiles()
    {
        playerProfiles.clear();

        boolean loadedFromFile = false;

        try
        {
            if (Files.exists(PLAYER_FILE))
            {
                String content = new String(Files.readAllBytes(PLAYER_FILE), StandardCharsets.UTF_8);
                Matcher currentMatcher = Pattern.compile("\\\"currentPlayer\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(content);
                if (currentMatcher.find())
                    currentPlayerName = sanitizePlayerName(currentMatcher.group(1));

                Matcher playerMatcher = Pattern.compile("\\{\\s*\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"maxScore\\\"\\s*:\\s*(\\d+)\\s*\\}").matcher(content);
                while (playerMatcher.find())
                {
                    String name = sanitizePlayerName(playerMatcher.group(1));
                    if (name.isEmpty())
                        continue;

                    int bestScore = Integer.parseInt(playerMatcher.group(2));
                    playerProfiles.put(name, new PlayerProfile(name, bestScore));
                }

                loadedFromFile = !playerProfiles.isEmpty();
            }
        }
        catch (Exception e)
        {
            System.err.println("✗ Error cargando jugadores: " + e.getMessage());
        }

        if (!loadedFromFile)
        {
            int legacyMax = loadMaxScore();
            playerProfiles.put(DEFAULT_PLAYER_NAME, new PlayerProfile(DEFAULT_PLAYER_NAME, legacyMax));
            currentPlayerName = DEFAULT_PLAYER_NAME;
        }

        if (currentPlayerName == null || !playerProfiles.containsKey(currentPlayerName))
            currentPlayerName = playerProfiles.keySet().iterator().next();

        recalculateTopPlayer();
        savePlayerProfiles();
    }

    private void savePlayerProfiles()
    {
        try
        {
            if (PLAYER_FILE.getParent() != null)
                Files.createDirectories(PLAYER_FILE.getParent());

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"currentPlayer\": \"").append(escapeJson(currentPlayerName)).append("\",\n");
            json.append("  \"players\": [\n");

            int index = 0;
            for (PlayerProfile profile : playerProfiles.values())
            {
                json.append("    {\"name\": \"").append(escapeJson(profile.name)).append("\", \"maxScore\": ").append(Math.max(0, profile.maxScore)).append("}");
                index++;
                if (index < playerProfiles.size())
                    json.append(",");
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}\n");

            Files.write(PLAYER_FILE, json.toString().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            System.err.println("✗ Error guardando jugadores: " + e.getMessage());
        }
    }

    private void updateCurrentPlayerBestScore(int runScore)
    {
        if (currentPlayerName == null)
            currentPlayerName = DEFAULT_PLAYER_NAME;

        PlayerProfile profile = getOrCreateProfile(currentPlayerName);
        if (runScore > profile.maxScore)
        {
            profile.maxScore = runScore;
            currentPlayerBestScore = runScore;
            savePlayerProfiles();
        }
        else
        {
            currentPlayerBestScore = profile.maxScore;
        }

        recalculateTopPlayer();
    }

    private PlayerProfile getOrCreateProfile(String name)
    {
        String key = sanitizePlayerName(name);
        if (key.isEmpty())
            key = DEFAULT_PLAYER_NAME;

        PlayerProfile profile = playerProfiles.get(key);
        if (profile == null)
        {
            profile = new PlayerProfile(key, 0);
            playerProfiles.put(key, profile);
        }
        return profile;
    }

    private void recalculateTopPlayer()
    {
        String bestName = DEFAULT_PLAYER_NAME;
        int bestScore = 0;

        for (PlayerProfile profile : playerProfiles.values())
        {
            if (profile.maxScore >= bestScore)
            {
                bestScore = profile.maxScore;
                bestName = profile.name;
            }
        }

        topPlayerName = bestName;
        maxScore = bestScore;
    }

    private String sanitizePlayerName(String value)
    {
        if (value == null)
            return "";

        String sanitized = value.trim().replaceAll("\\s+", " ");
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9 _-]", "");
        if (sanitized.length() > 18)
            sanitized = sanitized.substring(0, 18);
        return sanitized;
    }

    private String escapeJson(String value)
    {
        if (value == null)
            return "";

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class PlayerProfile
    {
        private final String name;
        private int maxScore;

        private PlayerProfile(String name, int maxScore)
        {
            this.name = name;
            this.maxScore = Math.max(0, maxScore);
        }
    }
}
