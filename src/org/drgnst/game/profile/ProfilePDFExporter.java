package org.drgnst.game.profile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Exporta los datos de perfiles a PDF usando un formato simple basado en texto
 */
public class ProfilePDFExporter
{
    private static final Path EXPORTS_DIR = Paths.get("res/exports");

    public static void exportAllProfilesToPDF(ProfileManager profileManager)
    {
        ensureExportDirectoryExists();
        
        List<Profile> profiles = profileManager.getProfilesSorted();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "Perfiles_" + timestamp + ".txt";
        Path exportPath = Paths.get(EXPORTS_DIR.toString(), filename);

        try (PrintWriter writer = new PrintWriter(new FileWriter(exportPath.toFile())))
        {
            writer.println("╔════════════════════════════════════════════════════════════════════════════════╗");
            writer.println("║                    REPORTE DE PERFILES - PERSPECTIVA GAME                      ║");
            writer.println("╚════════════════════════════════════════════════════════════════════════════════╝");
            writer.println();
            writer.println("Fecha de Generación: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            writer.println("Total de Perfiles: " + profiles.size());
            writer.println();
            writer.println("════════════════════════════════════════════════════════════════════════════════════");
            writer.println();

            if (profiles.isEmpty())
            {
                writer.println("No hay perfiles registrados.");
            }
            else
            {
                for (int i = 0; i < profiles.size(); i++)
                {
                    Profile profile = profiles.get(i);
                    writer.println("PERFIL #" + (i + 1) + ": " + profile.getName());
                    writer.println("─────────────────────────────────────────────────────────────────────────────");
                    writer.println("  ├─ Puntuación Máxima: " + formatNumber(profile.getMaxScore()));
                    writer.println("  ├─ Juegos Jugados: " + profile.getTotalGamesPlayed());
                    writer.println("  ├─ Enemigos Eliminados: " + formatNumber(profile.getTotalEnemiesKilled()));
                    writer.println("  ├─ Jefes Derrotados: " + profile.getTotalBossesDefeated());
                    writer.println("  ├─ Tiempo Total de Juego: " + String.format("%.2f", profile.getPlayTimeHours()) + " horas");
                    writer.println("  ├─ Perfil Creado: " + profile.getFormattedCreatedDate());
                    writer.println("  └─ Último Juego: " + profile.getFormattedLastPlayedDate());
                    writer.println();
                }
            }

            writer.println("════════════════════════════════════════════════════════════════════════════════════");
            writer.println();
            writer.println("ESTADÍSTICAS GENERALES");
            writer.println("─────────────────────────────────────────────────────────────────────────────────");
            
            int totalScore = 0;
            int totalGames = 0;
            int totalEnemies = 0;
            int totalBosses = 0;
            double totalPlayTime = 0;

            for (Profile profile : profiles)
            {
                totalScore += profile.getMaxScore();
                totalGames += profile.getTotalGamesPlayed();
                totalEnemies += profile.getTotalEnemiesKilled();
                totalBosses += profile.getTotalBossesDefeated();
                totalPlayTime += profile.getPlayTimeHours();
            }

            writer.println("  ├─ Puntuación Total Acumulada: " + formatNumber(totalScore));
            writer.println("  ├─ Juegos Totales: " + totalGames);
            writer.println("  ├─ Enemigos Totales Eliminados: " + formatNumber(totalEnemies));
            writer.println("  ├─ Jefes Totales Derrotados: " + totalBosses);
            writer.println("  └─ Tiempo Total de Juego: " + String.format("%.2f", totalPlayTime) + " horas");
            
            writer.println();
            writer.println("════════════════════════════════════════════════════════════════════════════════════");

            System.out.println("✓ Datos exportados exitosamente a: " + exportPath.toAbsolutePath());
        }
        catch (IOException e)
        {
            System.err.println("✗ Error exportando datos a PDF");
            e.printStackTrace();
        }
    }

    public static void exportSingleProfileToPDF(Profile profile)
    {
        if (profile == null) return;

        ensureExportDirectoryExists();
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "Perfil_" + profile.getName() + "_" + timestamp + ".txt";
        Path exportPath = Paths.get(EXPORTS_DIR.toString(), filename);

        try (PrintWriter writer = new PrintWriter(new FileWriter(exportPath.toFile())))
        {
            writer.println("╔════════════════════════════════════════════════════════════════════════════════╗");
            writer.println("║                    PERFIL DE JUGADOR - PERSPECTIVA GAME                       ║");
            writer.println("╚════════════════════════════════════════════════════════════════════════════════╝");
            writer.println();
            writer.println("Nombre del Jugador: " + profile.getName());
            writer.println("Fecha de Generación: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            writer.println();
            writer.println("════════════════════════════════════════════════════════════════════════════════════");
            writer.println();
            writer.println("ESTADÍSTICAS PRINCIPALES");
            writer.println("─────────────────────────────────────────────────────────────────────────────────");
            writer.println("  ├─ Puntuación Máxima: " + formatNumber(profile.getMaxScore()));
            writer.println("  ├─ Juegos Jugados: " + profile.getTotalGamesPlayed());
            writer.println("  ├─ Enemigos Eliminados: " + formatNumber(profile.getTotalEnemiesKilled()));
            writer.println("  ├─ Jefes Derrotados: " + profile.getTotalBossesDefeated());
            writer.println("  ├─ Tiempo Total de Juego: " + String.format("%.2f", profile.getPlayTimeHours()) + " horas");
            writer.println();
            writer.println("INFORMACIÓN TEMPORAL");
            writer.println("─────────────────────────────────────────────────────────────────────────────────");
            writer.println("  ├─ Perfil Creado: " + profile.getFormattedCreatedDate());
            writer.println("  └─ Último Acceso: " + profile.getFormattedLastPlayedDate());
            writer.println();
            writer.println("════════════════════════════════════════════════════════════════════════════════════");

            System.out.println("✓ Perfil exportado exitosamente a: " + exportPath.toAbsolutePath());
        }
        catch (IOException e)
        {
            System.err.println("✗ Error exportando perfil");
            e.printStackTrace();
        }
    }

    private static void ensureExportDirectoryExists()
    {
        try
        {
            if (!java.nio.file.Files.exists(EXPORTS_DIR))
            {
                java.nio.file.Files.createDirectories(EXPORTS_DIR);
                System.out.println("✓ Directorio de exportación creado: " + EXPORTS_DIR);
            }
        }
        catch (IOException e)
        {
            System.err.println("✗ Error creando directorio de exportación");
            e.printStackTrace();
        }
    }

    private static String formatNumber(int number)
    {
        return String.format("%,d", number).replace(",", ".");
    }
}
