package featurecat.omega.ui;

import featurecat.omega.Omega;

import java.awt.event.*;

import static java.awt.event.KeyEvent.*;

public class Input implements MouseListener, KeyListener, MouseWheelListener, MouseMotionListener {
    private boolean controlIsPressed = false;

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        if (e.getButton() == MouseEvent.BUTTON1) // left mouse click
            Omega.frame.onClicked(x, y);
        else if (e.getButton() == MouseEvent.BUTTON3) // right mouse click
            undo();
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        Omega.frame.onMouseMoved(x, y);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    private void undo() {
        int movesToAdvance = 1;
        if (controlIsPressed)
            movesToAdvance = 10;

        for (int i = 0; i < movesToAdvance; i++)
            Omega.board.previousMove();
    }

    private void redo() {
        int movesToAdvance = 1;
        if (controlIsPressed)
            movesToAdvance = 10;

        for (int i = 0; i < movesToAdvance; i++)
            Omega.board.nextMove();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_CONTROL:
                controlIsPressed = true;
                break;

            case VK_RIGHT:
                redo();
                break;

            case VK_LEFT:
                undo();
                break;

            case VK_O:
                if (Omega.leelaz.isPondering())
                    Omega.leelaz.togglePonder();
                Omega.frame.openSgf();
                break;

            case VK_HOME:
                while (Omega.board.previousMove()) ;
                break;

            case VK_END:
                while (Omega.board.nextMove()) ;
                break;
            default:
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_CONTROL:
                controlIsPressed = false;
                break;

            default:
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() > 0) {
            redo();
        } else if (e.getWheelRotation() < 0) {
            undo();
        }
    }
}