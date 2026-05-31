package org.drgnst.game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.drgnst.game.entities.Boss;

public class ScoreWindow extends JFrame
{
    private final StatsPanel statsPanel;

    public ScoreWindow()
    {
        setTitle("Score");
        setSize(360, 460);
        setResizable(false);
        setLocation(20, 20);
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        statsPanel = new StatsPanel();
        setContentPane(statsPanel);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                setVisible(false);
            }
        });
    }

    public void updateFromGame(Game game, boolean paused)
    {
        if (game == null)
            return;

        statsPanel.updateFromGame(game, paused);
        if (!isVisible())
            setVisible(true);

        setAlwaysOnTop(true);
        toFront();
        statsPanel.repaint();
    }

    public void setHidden()
    {
        if (isVisible())
            setVisible(false);
    }

    public void pinNear(Window owner)
    {
        if (owner == null)
            return;

        int x = owner.getX() + owner.getWidth() + 8;
        int y = owner.getY();
        setLocation(x, y);
        setAlwaysOnTop(true);
        if (isVisible())
            toFront();
    }

    private static class StatsPanel extends JPanel
    {
        private String playerName = "-";
        private String topPlayerName = "-";
        private int currentScore;
        private int maxScore;
        private int kills;
        private int deaths;
        private int health;
        private int ammo;
        private int maxAmmo;
        private int bossHealth;
        private boolean hasBoss;
        private boolean paused;

        private StatsPanel()
        {
            setBackground(new Color(14, 14, 20));
            setPreferredSize(new Dimension(360, 460));
        }

        private void updateFromGame(Game game, boolean paused)
        {
            playerName = safe(game.getCurrentPlayerName());
            topPlayerName = safe(game.getTopPlayerName());
            currentScore = game.getScore();
            maxScore = game.getMaxScore();
            kills = game.getKills();
            deaths = game.getDeaths();
            health = game.getPlayerHealthPercent();
            ammo = game.getAmmo();
            maxAmmo = game.getMaxAmmo();
            Boss boss = game.getBoss();
            hasBoss = boss != null && !boss.isExpired();
            bossHealth = hasBoss ? boss.getHealthPercent() : 0;
            this.paused = paused;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawBackground(g2);

                int margin = 14;
                int fullW = getWidth() - margin * 2;
                int gap = 16;
                int halfW = (fullW - gap) / 2;

                drawHeader(g2, margin, 12, fullW, 56);
                drawCard(g2, margin, 80, fullW, 74, "Jugador", playerName, new Color(255, 255, 255));

                drawCard(g2, margin, 166, halfW, 82, "Score", String.valueOf(currentScore), new Color(255, 196, 72));
                drawCard(g2, margin + halfW + gap, 166, halfW, 82, "Max", String.valueOf(maxScore), new Color(120, 220, 255));

                drawCard(g2, margin, 260, halfW, 82, "Kills", String.valueOf(kills), new Color(230, 230, 230));
                drawCard(g2, margin + halfW + gap, 260, halfW, 82, "Deaths", String.valueOf(deaths), new Color(255, 136, 136));

                drawBarCard(g2, margin, 354, halfW, 50, "HP", health, new Color(40, 190, 70), new Color(60, 60, 60));
                drawAmmoCard(g2, margin + halfW + gap, 354, halfW, 50, ammo, maxAmmo, new Color(255, 196, 72), new Color(60, 60, 60));

                if (hasBoss)
                {
                    drawCard(g2, margin, 416, fullW, 30, "Boss", bossHealth + "%", new Color(217, 124, 255));
                }
                else
                {
                    drawCard(g2, margin, 416, fullW, 30, "Top", topPlayerName, new Color(120, 220, 255));
                }
            }
            finally
            {
                g2.dispose();
            }
        }

        private void drawBackground(Graphics2D g2)
        {
            g2.setColor(new Color(13, 13, 19));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(22, 22, 30));
            g2.fillRoundRect(6, 6, getWidth() - 12, getHeight() - 12, 20, 20);
            g2.setColor(new Color(70, 70, 90));
            g2.drawRoundRect(6, 6, getWidth() - 12, getHeight() - 12, 20, 20);
        }

        private void drawHeader(Graphics2D g2, int x, int y, int w, int h)
        {
            g2.setColor(new Color(32, 32, 44));
            g2.fillRoundRect(x, y, w, h, 18, 18);
            g2.setColor(new Color(88, 88, 110));
            g2.drawRoundRect(x, y, w, h, 18, 18);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 22));
            g2.drawString("SCORE PANEL", x + 12, y + 34);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g2.setColor(new Color(190, 190, 205));
            g2.drawString("Estadisticas en vivo", x + 14, y + 50);

            if (paused)
            {
                int badgeW = 74;
                int badgeH = 24;
                int bx = x + w - badgeW - 12;
                int by = y + (h - badgeH) / 2;
                g2.setColor(new Color(255, 196, 72));
                g2.fillRoundRect(bx, by, badgeW, badgeH, 14, 14);
                g2.setColor(new Color(50, 34, 6));
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString("PAUSA", bx + 14, by + 16);
            }
        }

        private void drawCard(Graphics2D g2, int x, int y, int w, int h, String label, String value, Color accent)
        {
            g2.setColor(new Color(24, 24, 32));
            g2.fillRoundRect(x, y, w, h, 14, 14);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 145));
            g2.drawRoundRect(x, y, w, h, 14, 14);

            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(new Color(205, 205, 220));
            g2.drawString(label, x + 12, y + 18);

            g2.setFont(new Font("SansSerif", Font.BOLD, 19));
            g2.setColor(accent);
            g2.drawString(shorten(value, 18), x + 12, y + h - 14);
        }

        private void drawBarCard(Graphics2D g2, int x, int y, int w, int h, String label, int percent, Color fillColor, Color backColor)
        {
            g2.setColor(new Color(24, 24, 32));
            g2.fillRoundRect(x, y, w, h, 14, 14);
            g2.setColor(new Color(80, 80, 90));
            g2.drawRoundRect(x, y, w, h, 14, 14);

            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(new Color(210, 210, 210));
            g2.drawString(label, x + 12, y + 16);

            int barX = x + 12;
            int barY = y + 24;
            int barW = w - 24;
            int barH = 12;
            int clamped = Math.max(0, Math.min(100, percent));
            int fillW = (int) (barW * (clamped / 100.0));

            g2.setColor(backColor);
            g2.fillRoundRect(barX, barY, barW, barH, 8, 8);
            g2.setColor(fillColor);
            g2.fillRoundRect(barX, barY, fillW, barH, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(clamped + "%", x + w - 42, y + 16);
        }

        private void drawAmmoCard(Graphics2D g2, int x, int y, int w, int h, int ammo, int maxAmmo, Color fillColor, Color backColor)
        {
            g2.setColor(new Color(24, 24, 32));
            g2.fillRoundRect(x, y, w, h, 14, 14);
            g2.setColor(new Color(80, 80, 90));
            g2.drawRoundRect(x, y, w, h, 14, 14);

            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(new Color(210, 210, 210));
            g2.drawString("Ammo", x + 12, y + 16);

            int safeMax = Math.max(0, maxAmmo);
            int safeAmmo = Math.max(0, Math.min(ammo, safeMax));
            String ammoText = safeAmmo + " / " + safeMax;
            g2.setColor(fillColor);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2.drawString(ammoText, x + 12, y + 41);

            drawAmmoDots(g2, x + 84, y + 24, safeAmmo, safeMax, fillColor, backColor);
        }

        private void drawAmmoDots(Graphics2D g2, int x, int y, int ammo, int maxAmmo, Color fillColor, Color backColor)
        {
            int maxDots = Math.min(Math.max(maxAmmo, 0), 8);
            if (maxDots <= 0)
                return;

            int dotSize = 8;
            int gap = 4;
            for (int i = 0; i < maxDots; i++)
            {
                int dx = x + i * (dotSize + gap);
                g2.setColor(i < ammo ? fillColor : backColor);
                g2.fillOval(dx, y, dotSize, dotSize);
            }
        }

        private String safe(String value)
        {
            return value == null || value.trim().isEmpty() ? "-" : value.trim();
        }

        private String shorten(String value, int max)
        {
            if (value == null)
                return "-";
            if (value.length() <= max)
                return value;
            return value.substring(0, Math.max(0, max - 3)) + "...";
        }
    }
}
