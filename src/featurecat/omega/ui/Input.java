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
        if (Omega.frame.isPlayingAgainstLeelaz) {
            Omega.frame.isPlayingAgainstLeelaz = false;
        }
        int movesToAdvance = 1;
        if (controlIsPressed)
            movesToAdvance = 10;

        for (int i = 0; i < movesToAdvance; i++)
            Omega.board.previousMove();
    }

    private void redo() {
        if (Omega.frame.isPlayingAgainstLeelaz) {
            Omega.frame.isPlayingAgainstLeelaz = false;
        }
        int movesToAdvance = 1;
        if (controlIsPressed)
            movesToAdvance = 10;

        for (int i = 0; i < movesToAdvance; i++)
            Omega.board.nextMove();
    }

    @Override
    public void keyPressed(KeyEvent e) {

        int movesToAdvance = 1; // number of moves to advance if control is held down
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

            case VK_SPACE:
                if (Omega.frame.isPlayingAgainstLeelaz) {
                    Omega.frame.isPlayingAgainstLeelaz = false;
                    Omega.leelaz.togglePonder(); // we must toggle twice for it to restart pondering
                }
                Omega.leelaz.togglePonder();
                break;

            case VK_P:
                Omega.board.pass();
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

            case VK_C:
                Omega.frame.toggleCoordinates();
                break;
            default:
        }
    }

    private boolean wasPonderingWhenControlsShown = false;

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_CONTROL:
                controlIsPressed = false;
                break;

            case VK_X:
                if (wasPonderingWhenControlsShown)
                    Omega.leelaz.togglePonder();
                Omega.frame.showControls = false;
                Omega.frame.repaint();
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