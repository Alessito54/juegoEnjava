package org.drgnst.game.profile;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa un perfil del jugador con sus estadísticas
 */
public class Profile implements Serializable, Comparable<Profile>
{
    private static final long serialVersionUID = 1L;
    
    private String name;
    private int maxScore;
    private int totalGamesPlayed;
    private int totalEnemiesKilled;
    private int totalBossesDefeated;
    private LocalDateTime createdDate;
    private LocalDateTime lastPlayedDate;
    private double playTimeHours;

    public Profile(String name)
    {
        this.name = name;
        this.maxScore = 0;
        this.totalGamesPlayed = 0;
        this.totalEnemiesKilled = 0;
        this.totalBossesDefeated = 0;
        this.createdDate = LocalDateTime.now();
        this.lastPlayedDate = LocalDateTime.now();
        this.playTimeHours = 0.0;
    }

    // Getters
    public String getName() { return name; }
    public int getMaxScore() { return maxScore; }
    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public int getTotalEnemiesKilled() { return totalEnemiesKilled; }
    public int getTotalBossesDefeated() { return totalBossesDefeated; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public LocalDateTime getLastPlayedDate() { return lastPlayedDate; }
    public double getPlayTimeHours() { return playTimeHours; }

    // Setters
    public void setMaxScore(int score)
    {
        if (score > this.maxScore)
            this.maxScore = score;
    }

    public void addGamePlayed()
    {
        this.totalGamesPlayed++;
        this.lastPlayedDate = LocalDateTime.now();
    }

    public void addEnemiesKilled(int count)
    {
        this.totalEnemiesKilled += count;
    }

    public void addBossDefeated()
    {
        this.totalBossesDefeated++;
    }

    public void addPlayTime(double hours)
    {
        this.playTimeHours += hours;
    }

    public String getFormattedCreatedDate()
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return createdDate.format(formatter);
    }

    public String getFormattedLastPlayedDate()
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return lastPlayedDate.format(formatter);
    }

    @Override
    public int compareTo(Profile other)
    {
        // Ordenar por puntuación máxima descendente
        return Integer.compare(other.maxScore, this.maxScore);
    }

    @Override
    public String toString()
    {
        return String.format("%s - Score: %d | Juegos: %d | Enemigos: %d | Jefes: %d",
                name, maxScore, totalGamesPlayed, totalEnemiesKilled, totalBossesDefeated);
    }
}
