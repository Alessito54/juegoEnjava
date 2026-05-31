package org.drgnst.game;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Input handling utilities
 */
public class InputHandler implements KeyListener, FocusListener, MouseMotionListener, MouseListener, MouseWheelListener
{
    public boolean keys[];
    public int mouseX;
    public int mouseY;
    public boolean mousePressed;
    private boolean mouseClickPending;
    private StringBuilder typedCharacters;

    public InputHandler()
    {
        keys = new boolean[65535];
        mouseX = 0;
        mouseY = 0;
        mousePressed = false;
        mouseClickPending = false;
        typedCharacters = new StringBuilder();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent arg0)
    {
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        mouseX = e.getX();
        mouseY = e.getY();
        mouseClickPending = true;
    }

    @Override
    public void mouseEntered(MouseEvent arg0)
    {
    }

    @Override
    public void mouseExited(MouseEvent arg0)
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        mouseX = e.getX();
        mouseY = e.getY();
        mousePressed = true;
        mouseClickPending = true;
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        mouseX = e.getX();
        mouseY = e.getY();
        mousePressed = false;
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void focusGained(FocusEvent arg0)
    {
    }

    @Override
    public void focusLost(FocusEvent arg0)
    {
        for (int i = 0; i < keys.length; i++)
            keys[i] = false;
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        keys[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent arg0)
    {
        char c = arg0.getKeyChar();
        if (!Character.isISOControl(c))
            typedCharacters.append(c);
    }

    public boolean consumeMouseClick()
    {
        if (mouseClickPending)
        {
            mouseClickPending = false;
            return true;
        }
        return false;
    }

    public String consumeTypedCharacters()
    {
        if (typedCharacters.length() == 0)
            return "";

        String typed = typedCharacters.toString();
        typedCharacters.setLength(0);
        return typed;
    }

}
