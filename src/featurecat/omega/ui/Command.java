package featurecat.omega.ui;

public class Command {
    private int keyCode;
    private Runnable action;

    public Command(int keyCode, Runnable action) {
        this.keyCode = keyCode;
        this.action = action;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void call() {
        action.run();
    }
}
