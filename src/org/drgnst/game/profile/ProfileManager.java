package org.drgnst.game.profile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Gestiona la creación, carga y almacenamiento de perfiles
 */
public class ProfileManager
{
    private static final Path PROFILES_DIR = Paths.get("res/profiles");
    private static final String PROFILE_EXTENSION = ".profile";
    
    private Map<String, Profile> profiles;
    private Profile currentProfile;

    public ProfileManager()
    {
        this.profiles = new HashMap<>();
        ensureProfilesDirectoryExists();
        loadAllProfiles();
    }

    private void ensureProfilesDirectoryExists()
    {
        try
        {
            if (!Files.exists(PROFILES_DIR))
            {
                Files.createDirectories(PROFILES_DIR);
                System.out.println("✓ Directorio de perfiles creado: " + PROFILES_DIR);
            }
        }
        catch (IOException e)
        {
            System.err.println("✗ Error creando directorio de perfiles");
            e.printStackTrace();
        }
    }

    public void createProfile(String name)
    {
        if (profiles.containsKey(name))
        {
            System.out.println("✗ El perfil '" + name + "' ya existe");
            return;
        }

        Profile newProfile = new Profile(name);
        profiles.put(name, newProfile);
        saveProfile(newProfile);
        System.out.println("✓ Perfil creado: " + name);
    }

    public void selectProfile(String name)
    {
        if (profiles.containsKey(name))
        {
            currentProfile = profiles.get(name);
            System.out.println("✓ Perfil seleccionado: " + name);
        }
        else
        {
            System.out.println("✗ Perfil no encontrado: " + name);
        }
    }

    public void deleteProfile(String name)
    {
        if (profiles.remove(name) != null)
        {
            try
            {
                Path profilePath = Paths.get(PROFILES_DIR.toString(), name + PROFILE_EXTENSION);
                Files.deleteIfExists(profilePath);
                System.out.println("✓ Perfil eliminado: " + name);
            }
            catch (IOException e)
            {
                System.err.println("✗ Error eliminando perfil");
                e.printStackTrace();
            }
        }
    }

    public void saveProfile(Profile profile)
    {
        if (profile == null) return;

        try
        {
            Path profilePath = Paths.get(PROFILES_DIR.toString(), profile.getName() + PROFILE_EXTENSION);
            
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    Files.newOutputStream(profilePath)))
            {
                oos.writeObject(profile);
            }
            
            System.out.println("✓ Perfil guardado: " + profile.getName());
        }
        catch (IOException e)
        {
            System.err.println("✗ Error guardando perfil: " + profile.getName());
            e.printStackTrace();
        }
    }

    public void loadAllProfiles()
    {
        profiles.clear();
        
        try
        {
            if (!Files.exists(PROFILES_DIR)) return;

            Files.list(PROFILES_DIR)
                .filter(path -> path.toString().endsWith(PROFILE_EXTENSION))
                .forEach(path -> {
                    try (ObjectInputStream ois = new ObjectInputStream(
                            Files.newInputStream(path)))
                    {
                        Profile profile = (Profile) ois.readObject();
                        profiles.put(profile.getName(), profile);
                        System.out.println("✓ Perfil cargado: " + profile.getName());
                    }
                    catch (IOException | ClassNotFoundException e)
                    {
                        System.err.println("✗ Error cargando perfil: " + path.getFileName());
                        e.printStackTrace();
                    }
                });
        }
        catch (IOException e)
        {
            System.err.println("✗ Error listando perfiles");
            e.printStackTrace();
        }
    }

    // Getters
    public Profile getCurrentProfile() { return currentProfile; }
    public Collection<Profile> getAllProfiles() { return profiles.values(); }
    public Profile getProfile(String name) { return profiles.get(name); }
    public boolean hasProfile(String name) { return profiles.containsKey(name); }
    public int getProfileCount() { return profiles.size(); }

    public List<Profile> getProfilesSorted()
    {
        List<Profile> sorted = new ArrayList<>(profiles.values());
        Collections.sort(sorted);
        return sorted;
    }

    public void updateCurrentProfileScore(int score)
    {
        if (currentProfile != null)
        {
            currentProfile.setMaxScore(score);
            saveProfile(currentProfile);
        }
    }

    public void updateCurrentProfileStats(int enemiesKilled, int bossesDefeated, double playTimeHours)
    {
        if (currentProfile != null)
        {
            currentProfile.addGamePlayed();
            currentProfile.addEnemiesKilled(enemiesKilled);
            currentProfile.addBossDefeated();
            currentProfile.addPlayTime(playTimeHours);
            saveProfile(currentProfile);
        }
    }
}
