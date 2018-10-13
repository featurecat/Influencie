package featurecat.omega.ui;

import featurecat.omega.Omega;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import static java.awt.event.KeyEvent.*;

public class Input implements MouseListener, KeyListener, MouseWheelListener, MouseMotionListener {
    private boolean controlIsPressed = false;

    private List<Command> commands = new ArrayList<>();

    public Input() {
        command(KeyEvent.VK_RIGHT, this::redo);
        command(KeyEvent.VK_LEFT, this::undo);
        command(KeyEvent.VK_O, () -> Omega.frame.openSgf());
        command(KeyEvent.VK_HOME, () -> {
            while (Omega.board.previousMove()) ;
        });
        command(KeyEvent.VK_END, () -> {
            while (Omega.board.nextMove()) ;
        });
        command(KeyEvent.VK_W, () -> Omega.placeMode = PlaceMode.WHITE);
        command(KeyEvent.VK_B, () -> Omega.placeMode = PlaceMode.BLACK);
        command(KeyEvent.VK_A, () -> Omega.placeMode = PlaceMode.ALTERNATING);
        command(KeyEvent.VK_R, () -> Omega.placeMode = PlaceMode.REMOVE);
    }

    private void command(int keyCode, Runnable action) {
        commands.add(new Command(keyCode, action));
    }

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

            default:
        }

        for (Command command : commands) {
            if (e.getKeyCode() == command.getKeyCode()) {
                command.call();
                break;
            }
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