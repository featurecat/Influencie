package featurecat.omega.ui;

import featurecat.omega.Omega;

import java.awt.*;
import java.util.function.Consumer;

public class Slider {
    public static Insets insets;

    private int x, y, length;
    private String label;

    private int min, max, defaultValue;
    private Color color, text;

    private int currentValue;

    private Consumer set;

    public Slider(int x, int y, int length, String label, int min, int max, int defaultValue, Color color, Color text, Consumer set) {
        this.x = x;
        this.y = y;
        this.length = length;
        this.label = label;

        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;

        this.color = color;
        this.text = text;

        this.currentValue = defaultValue;
        this.set = set;
    }

    public boolean onClicked(int xx, int yy) {
        int x = this.x + insets.left;
        int y = this.y + insets.top;

        if (x - margin <= xx && xx <= x + length + 2*margin && y <= yy && yy <= y + fontSize + margin) {
            // it is a match. collision detected
            currentValue = (int) Math.max(0, Math.min(max, Math.round((double) (xx-(x - margin)) / (length + 2*margin) * max + min)));
            set.accept(currentValue);
            Omega.frame.repaint();
            return true;
        } else {
            return false;
        }
    }

    static final int fontSize = 20;
    static final int margin = 5;
    static final int strokeWidth = 2;

    public void render(Graphics2D g) {
        int x = this.x + insets.left;
        int y = this.y + insets.top;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(x - margin, y, length + 2 * margin, fontSize + margin);

        g.setColor(color);
        g.fillRect(x - margin, y, (length + 2 * margin) * (currentValue - min) / (max-min) , fontSize + margin);

        if (max - min < length/2) {
            g.setColor(text);
            for (int i = 0; i <= max-min; i++) {
                int len = (max-min > 1) ? length + 2*margin : length;
                int xx = x + (int) ((double) i / (max - min) * len);
                if (max - min > 1) {
                    xx -= margin;
                }
                g.drawLine(xx, y + margin, xx, y);
            }
        }

        g.setColor(text);
        g.setFont(new Font("Calibri", Font.BOLD, fontSize));

        g.drawString(label, x, y + fontSize);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(strokeWidth));
        g.drawRect(x - margin, y, length + 2 * margin, fontSize + margin);

        g.setStroke(new BasicStroke(1));
    }
}
