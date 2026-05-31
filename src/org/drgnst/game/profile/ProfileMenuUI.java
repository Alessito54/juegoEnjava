package org.drgnst.game.profile;

import org.drgnst.game.gfx.Bitmap;
import java.util.*;

/**
 * Interfaz de usuario para la gestión de perfiles
 */
public class ProfileMenuUI
{
    public static final int BUTTON_WIDTH = 280;
    public static final int BUTTON_HEIGHT = 40;
    public static final int BUTTON_GAP = 10;
    public static final int TEXT_COLOR = 0xFFFFFF;
    public static final int TEXT_HIGHLIGHT_COLOR = 0xFFFF00;
    public static final int HEADER_COLOR = 0x00FF00;
    public static final int BACKGROUND_COLOR = 0x1a1a2e;
    public static final int BORDER_COLOR = 0x0f3460;

    private ProfileManager profileManager;
    private int selectedIndex;
    private String inputText;
    private boolean isCreatingProfile;
    private int scrollOffset;

    public ProfileMenuUI(ProfileManager profileManager)
    {
        this.profileManager = profileManager;
        this.selectedIndex = 0;
        this.inputText = "";
        this.isCreatingProfile = false;
        this.scrollOffset = 0;
    }

    public void renderProfileSelection(Bitmap screen)
    {
        fillScreen(screen, BACKGROUND_COLOR);
        drawBorder(screen);

        // Título
        drawCenteredText(screen, "GESTIÓN DE PERFILES", screen.height / 8, HEADER_COLOR);

        List<Profile> profiles = new ArrayList<>(profileManager.getAllProfiles());
        Collections.sort(profiles);

        // Si no hay perfiles
        if (profiles.isEmpty())
        {
            drawCenteredText(screen, "No hay perfiles creados", screen.height / 2 - 40, TEXT_COLOR);
            drawCenteredText(screen, "Presiona ENTER para crear uno", screen.height / 2, TEXT_COLOR);
            return;
        }

        // Mostrar perfiles disponibles
        int startY = screen.height / 6;
        int maxVisible = (screen.height - startY - 80) / (BUTTON_HEIGHT + BUTTON_GAP);

        for (int i = 0; i < Math.min(maxVisible, profiles.size()); i++)
        {
            Profile profile = profiles.get(i);
            int yPos = startY + i * (BUTTON_HEIGHT + BUTTON_GAP);
            
            int color = (i == selectedIndex) ? TEXT_HIGHLIGHT_COLOR : TEXT_COLOR;
            drawButton(screen, yPos, profile.getName() + " - Score: " + profile.getMaxScore(), 
                      i == selectedIndex, color);
            
            // Mostrar información del perfil seleccionado
            if (i == selectedIndex)
            {
                int infoY = yPos + BUTTON_HEIGHT + 20;
                drawProfileInfo(screen, profile, infoY);
            }
        }

        // Instrucciones
        drawCenteredText(screen, "↑/↓ Navegar | ENTER Seleccionar | N Nueva | D Descargar | ESC Atrás", 
                        screen.height - 40, 0x888888);
    }

    public void renderProfileCreation(Bitmap screen)
    {
        fillScreen(screen, BACKGROUND_COLOR);
        drawBorder(screen);

        // Título
        drawCenteredText(screen, "CREAR NUEVO PERFIL", screen.height / 3, HEADER_COLOR);

        // Caja de entrada
        int inputBoxY = screen.height / 2 - 20;
        int inputBoxWidth = 300;
        int inputBoxX = (screen.width - inputBoxWidth) / 2;
        
        drawRectangle(screen, inputBoxX, inputBoxY, inputBoxWidth, 40, BORDER_COLOR);
        drawString(screen, inputBoxX + 10, inputBoxY + 12, inputText + "_", TEXT_COLOR);

        // Texto de ayuda
        drawCenteredText(screen, "Ingresa el nombre del perfil (máx 20 caracteres)", 
                        inputBoxY - 60, 0xCCCCCC);
        drawCenteredText(screen, "ENTER Crear | ESC Cancelar | BACKSPACE Borrar", 
                        screen.height - 40, 0x888888);
    }

    public void renderProfileInfo(Bitmap screen)
    {
        fillScreen(screen, BACKGROUND_COLOR);
        drawBorder(screen);

        Profile profile = profileManager.getCurrentProfile();
        if (profile == null)
        {
            drawCenteredText(screen, "No hay perfil seleccionado", screen.height / 2, TEXT_COLOR);
            return;
        }

        int startY = 40;
        int lineHeight = 30;
        int x = 60;

        // Encabezado
        drawString(screen, x, startY, "PERFIL: " + profile.getName(), HEADER_COLOR);

        // Información detallada
        startY += lineHeight * 2;
        drawString(screen, x, startY, "Puntuación Máxima: " + profile.getMaxScore(), TEXT_COLOR);
        startY += lineHeight;
        drawString(screen, x, startY, "Juegos Jugados: " + profile.getTotalGamesPlayed(), TEXT_COLOR);
        startY += lineHeight;
        drawString(screen, x, startY, "Enemigos Eliminados: " + profile.getTotalEnemiesKilled(), TEXT_COLOR);
        startY += lineHeight;
        drawString(screen, x, startY, "Jefes Derrotados: " + profile.getTotalBossesDefeated(), TEXT_COLOR);
        startY += lineHeight;
        drawString(screen, x, startY, "Tiempo de Juego: " + String.format("%.1f", profile.getPlayTimeHours()) + " horas", TEXT_COLOR);
        startY += lineHeight;
        drawString(screen, x, startY, "Perfil Creado: " + profile.getFormattedCreatedDate(), TEXT_COLOR);
        startY += lineHeight;
        drawString(screen, x, startY, "Último Juego: " + profile.getFormattedLastPlayedDate(), TEXT_COLOR);

        startY += lineHeight * 2;
        drawString(screen, x, startY, "D Descargar Datos | JUGAR Comenzar | ESC Atrás", 0x00FF00);
    }

    private void drawProfileInfo(Bitmap screen, Profile profile, int y)
    {
        int x = 80;
        int color = 0xAAAAAA;
        drawString(screen, x, y, "Juegos: " + profile.getTotalGamesPlayed() + " | " +
                                 "Enemigos: " + profile.getTotalEnemiesKilled() + " | " +
                                 "Jefes: " + profile.getTotalBossesDefeated(), color);
    }

    private void drawButton(Bitmap screen, int y, String text, boolean highlighted, int textColor)
    {
        int x = (screen.width - BUTTON_WIDTH) / 2;
        int bgColor = highlighted ? 0x0f3460 : 0x0a0a1a;
        int borderColor = highlighted ? 0x00FF00 : BORDER_COLOR;

        drawRectangle(screen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, bgColor);
        drawRectangleBorder(screen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, borderColor, 2);
        drawCenteredText(screen, text, y + BUTTON_HEIGHT / 2 - 4, textColor);
    }

    private void fillScreen(Bitmap screen, int color)
    {
        for (int i = 0; i < screen.pixels.length; i++)
        {
            screen.pixels[i] = color;
        }
    }

    private void drawBorder(Bitmap screen)
    {
        int color = 0x00AA00;
        int thickness = 3;
        
        // Top
        for (int x = 0; x < screen.width; x++)
            for (int t = 0; t < thickness; t++)
                screen.pixels[x + t * screen.width] = color;

        // Bottom
        for (int x = 0; x < screen.width; x++)
            for (int t = 0; t < thickness; t++)
                screen.pixels[x + (screen.height - 1 - t) * screen.width] = color;

        // Left
        for (int y = 0; y < screen.height; y++)
            for (int t = 0; t < thickness; t++)
                screen.pixels[t + y * screen.width] = color;

        // Right
        for (int y = 0; y < screen.height; y++)
            for (int t = 0; t < thickness; t++)
                screen.pixels[screen.width - 1 - t + y * screen.width] = color;
    }

    private void drawRectangle(Bitmap screen, int x, int y, int w, int h, int color)
    {
        for (int yy = 0; yy < h; yy++)
        {
            for (int xx = 0; xx < w; xx++)
            {
                int px = x + xx;
                int py = y + yy;
                if (px >= 0 && px < screen.width && py >= 0 && py < screen.height)
                {
                    screen.pixels[px + py * screen.width] = color;
                }
            }
        }
    }

    private void drawRectangleBorder(Bitmap screen, int x, int y, int w, int h, int color, int thickness)
    {
        // Top
        for (int xx = x; xx < x + w; xx++)
            for (int t = 0; t < thickness; t++)
                if (xx >= 0 && xx < screen.width && y + t >= 0 && y + t < screen.height)
                    screen.pixels[xx + (y + t) * screen.width] = color;

        // Bottom
        for (int xx = x; xx < x + w; xx++)
            for (int t = 0; t < thickness; t++)
                if (xx >= 0 && xx < screen.width && y + h - 1 - t >= 0 && y + h - 1 - t < screen.height)
                    screen.pixels[xx + (y + h - 1 - t) * screen.width] = color;

        // Left
        for (int yy = y; yy < y + h; yy++)
            for (int t = 0; t < thickness; t++)
                if (x + t >= 0 && x + t < screen.width && yy >= 0 && yy < screen.height)
                    screen.pixels[x + t + yy * screen.width] = color;

        // Right
        for (int yy = y; yy < y + h; yy++)
            for (int t = 0; t < thickness; t++)
                if (x + w - 1 - t >= 0 && x + w - 1 - t < screen.width && yy >= 0 && yy < screen.height)
                    screen.pixels[x + w - 1 - t + yy * screen.width] = color;
    }

    private void drawCenteredText(Bitmap screen, String text, int y, int color)
    {
        int textWidth = text.length() * 4; // Aproximado
        int x = (screen.width - textWidth) / 2;
        drawString(screen, x, y, text, color);
    }

    private void drawString(Bitmap screen, int x, int y, String text, int color)
    {
        int[] font = getSimpleFont();
        
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            int charCode = (int) c;
            
            if (charCode < 32 || charCode > 126) continue;

            drawCharacter(screen, x + i * 8, y, c, color);
        }
    }

    private void drawCharacter(Bitmap screen, int x, int y, char c, int color)
    {
        // Dibujar un carácter simple usando píxeles
        int charSize = 8;
        
        for (int yy = 0; yy < charSize; yy++)
        {
            for (int xx = 0; xx < 4; xx++)
            {
                int px = x + xx;
                int py = y + yy;
                
                if (px >= 0 && px < screen.width && py >= 0 && py < screen.height)
                {
                    // Patrón simple para caracteres
                    if ((charSize - yy) % 2 == 0 && xx % 2 == 0)
                    {
                        screen.pixels[px + py * screen.width] = color;
                    }
                }
            }
        }
    }

    private int[] getSimpleFont()
    {
        return new int[256];
    }

    // Getters y setters
    public int getSelectedIndex() { return selectedIndex; }
    public void setSelectedIndex(int index) { this.selectedIndex = Math.max(0, index); }
    public void moveSelection(int direction) { selectedIndex += direction; }

    public String getInputText() { return inputText; }
    public void setInputText(String text) { this.inputText = text; }
    public void addInputCharacter(char c)
    {
        if (inputText.length() < 20)
            inputText += c;
    }
    public void removeLastInputCharacter()
    {
        if (!inputText.isEmpty())
            inputText = inputText.substring(0, inputText.length() - 1);
    }

    public boolean isCreatingProfile() { return isCreatingProfile; }
    public void setCreatingProfile(boolean creating) { this.isCreatingProfile = creating; }

    public int getScrollOffset() { return scrollOffset; }
    public void setScrollOffset(int offset) { this.scrollOffset = offset; }
}
