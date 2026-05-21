package org.drgnst.game.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Gestor de audio para reproducir música de fondo
 */
public class AudioManager
{
    private Clip musicClip;
    private boolean isPlaying = false;
    
    /**
     * Carga y reproduce un archivo de audio en bucle
     */
    public void playBackgroundMusic(String filePath)
    {
        try
        {
            File audioFile = new File(filePath);
            if (!audioFile.exists())
            {
                System.err.println("Archivo de audio no encontrado: " + filePath);
                return;
            }
            
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            musicClip = AudioSystem.getClip();
            musicClip.open(audioInputStream);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);  // Reproducir en bucle infinito
            musicClip.start();
            isPlaying = true;
            System.out.println("Música: " + filePath + " está siendo reproducida");
        }
        catch (UnsupportedAudioFileException e)
        {
            System.err.println("Formato de audio no soportado: " + e.getMessage());
        }
        catch (IOException e)
        {
            System.err.println("Error al leer archivo de audio: " + e.getMessage());
        }
        catch (LineUnavailableException e)
        {
            System.err.println("Línea de audio no disponible: " + e.getMessage());
        }
    }

    /**
     * Reproduce un sonido corto una sola vez.
     */
    public void playSoundOnce(String filePath)
    {
        new Thread(() -> {
            try
            {
                File audioFile = new File(filePath);
                if (!audioFile.exists())
                {
                    System.err.println("Archivo de audio no encontrado: " + filePath);
                    return;
                }

                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();

                while (clip.isRunning())
                {
                    Thread.sleep(10);
                }

                clip.close();
                audioInputStream.close();
            }
            catch (UnsupportedAudioFileException e)
            {
                System.err.println("Formato de audio no soportado: " + e.getMessage());
            }
            catch (IOException e)
            {
                System.err.println("Error al leer archivo de audio: " + e.getMessage());
            }
            catch (LineUnavailableException e)
            {
                System.err.println("Línea de audio no disponible: " + e.getMessage());
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Detiene la música
     */
    public void stopMusic()
    {
        if (musicClip != null && isPlaying)
        {
            musicClip.stop();
            musicClip.close();
            isPlaying = false;
        }
    }
    
    /**
     * Pausa la música
     */
    public void pauseMusic()
    {
        if (musicClip != null && isPlaying)
        {
            musicClip.stop();
        }
    }
    
    /**
     * Reanuda la música
     */
    public void resumeMusic()
    {
        if (musicClip != null && !isPlaying)
        {
            musicClip.start();
            isPlaying = true;
        }
    }
    
    /**
     * Ajusta el volumen (0.0 a 1.0)
     */
    public void setVolume(float volume)
    {
        if (musicClip != null)
        {
            FloatControl gainControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        }
    }
    
    public boolean isPlaying()
    {
        return isPlaying;
    }
}
