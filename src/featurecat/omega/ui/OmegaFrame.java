package featurecat.omega.ui;

import featurecat.omega.Omega;
import featurecat.omega.rules.Board;
import featurecat.omega.analysis.SGFParser;
import featurecat.omega.rules.Search;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


/**
 * The window used to display the game.
 */
public class OmegaFrame extends JFrame {
    private static BoardRenderer boardRenderer;

    private final BufferStrategy bs;

    static {
        // load fonts
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, OmegaFrame.class.getResourceAsStream("/fonts/OpenSans-Regular.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, OmegaFrame.class.getResourceAsStream("/fonts/OpenSans-Semibold.ttf")));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a window
     */
    public OmegaFrame() {
        super("Omega");

        boardRenderer = new BoardRenderer();

        // we need a default size or else problems in Linux
        setSize(657, 687);
        setLocationRelativeTo(null); // start centered
        setExtendedState(Frame.MAXIMIZED_BOTH); // start maximized

        setVisible(true);

        createBufferStrategy(2);
        bs = getBufferStrategy();

        Input input = new Input();

        this.addMouseListener(input);
        this.addKeyListener(input);
        this.addMouseWheelListener(input);
        this.addMouseMotionListener(input);

        // necessary for Windows users - otherwise Lizzie shows a blank white screen on startup until updates occur.
        repaint();

        // when the window is closed: save the SGF file, then run shutdown()
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Omega.shutdown();
            }
        });
    }

    public static void openSgf() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter("*.sgf", "SGF");
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(filter);
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getPath().endsWith(".sgf")) {
                file = new File(file.getPath() + ".sgf");
            }
            try {
                System.out.println(file.getPath());
                SGFParser.load(file.getPath());
            } catch (IOException err) {
                JOptionPane.showConfirmDialog(null, "Failed to open the SGF file.", "Error", JOptionPane.ERROR);
            }
        }
    }

    public static void openFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            System.out.println(file.getPath());
            Search.directory = file.getPath();
        }
    }

    BufferedImage cachedImage;

    /**
     * Draws the game board and interface
     *
     * @param g0 not used
     */
    public void paint(Graphics g0) {
        if (bs == null)
            return;

        // initialize
        cachedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) cachedImage.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int topInset = this.getInsets().top;

        try {
            BufferedImage background = ImageIO.read(new File("assets/background.jpg"));
            int drawWidth = Math.max(background.getWidth(), getWidth());
            int drawHeight = Math.max(background.getHeight(), getHeight());
            g.drawImage(background, 0, 0, drawWidth, drawHeight, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int maxSize = (int) (Math.min(getWidth(), getHeight() - topInset) * 0.98);
        maxSize = Math.max(maxSize, Board.BOARD_SIZE + 5); // don't let maxWidth become too small

        int boardX = (getWidth() - maxSize) / 2;
        int boardY = topInset + (getHeight() - topInset - maxSize) / 2 + 3;
        boardRenderer.setLocation(boardX, boardY);
        boardRenderer.setBoardLength(maxSize);
        boardRenderer.draw(g);

        // cleanup
        g.dispose();

        // draw the image
        Graphics2D bsGraphics = (Graphics2D) bs.getDrawGraphics();
        bsGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        bsGraphics.drawImage(cachedImage, 0, 0, null);

        // cleanup
        bsGraphics.dispose();
        bs.show();
    }

    /**
     * Checks whether or not something was clicked and performs the appropriate action
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void onClicked(int x, int y) {
        // check for board click
        int[] boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);

        if (boardCoordinates != null) {
            Omega.board.place(boardCoordinates[0], boardCoordinates[1]);
        }
    }

    public void onMouseMoved(int x, int y) {
        // TODO do we need this method?
    }
}