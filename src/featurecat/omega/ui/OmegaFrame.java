package featurecat.omega.ui;

import featurecat.omega.Omega;
import featurecat.omega.rules.Board;
import featurecat.omega.analysis.SGFParser;
import featurecat.omega.rules.Search;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * The window used to display the game.
 */
public class OmegaFrame extends JFrame {
    private static BoardRenderer boardRenderer;

    private final BufferStrategy bs;

    private final List<Slider> sliders;

//    static {
//        // load fonts
//        try {
//            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, OmegaFrame.class.getResourceAsStream("/fonts/OpenSans-Regular.ttf")));
//            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, OmegaFrame.class.getResourceAsStream("/fonts/OpenSans-Semibold.ttf")));
//        } catch (IOException | FontFormatException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * Creates a window
     */
    public OmegaFrame() {
        super("Influencie");

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

        sliders = new ArrayList<>();

        Slider.insets = this.getInsets();

        int y = 5;
        int x = 10;
        int width = 30; // today just ISNT one of those days for writing good code.
        int length = 150;
        sliders.add(new Slider(x, y+0*width, length, "White Aura R", 0, 255, BoardRenderer.whiteAuraRed, new Color(0xa3, 0x36, 0x43).brighter(), Color.YELLOW, val -> BoardRenderer.whiteAuraRed = (int)val));
        sliders.add(new Slider(x, y+1*width, length, "White Aura G", 0, 255, BoardRenderer.whiteAuraGreen, new Color(0x4b, 0x91, 0x30).brighter(), Color.BLACK, val -> BoardRenderer.whiteAuraGreen = (int)val));
        sliders.add(new Slider(x, y+2*width, length, "White Aura B", 0, 255, BoardRenderer.whiteAuraBlue, new Color(0x2e, 0x42, 0x72).brighter().brighter(), Color.CYAN, val -> BoardRenderer.whiteAuraBlue = (int)val));

        y += width/2;

        sliders.add(new Slider(x, y+3*width, length, "Black Aura R", 0, 255, BoardRenderer.blackAuraRed, new Color(0xa3, 0x36, 0x43).brighter(), Color.YELLOW, val -> BoardRenderer.blackAuraRed = (int)val));
        sliders.add(new Slider(x, y+4*width, length, "Black Aura G", 0, 255, BoardRenderer.blackAuraGreen, new Color(0x4b, 0x91, 0x30).brighter(), Color.BLACK, val -> BoardRenderer.blackAuraGreen = (int)val));
        sliders.add(new Slider(x, y+5*width, length, "Black Aura B", 0, 255, BoardRenderer.blackAuraBlue, new Color(0x2e, 0x42, 0x72).brighter().brighter(), Color.CYAN, val -> BoardRenderer.blackAuraBlue = (int)val));

        y += width/2;

        sliders.add(new Slider(x, y+6*width, length, "Intensity", 0, 255, BoardRenderer.maxAlpha, new Color(0xB6,0x00,0x89).brighter(), Color.BLACK, val -> BoardRenderer.maxAlpha = (int)val));
        sliders.add(new Slider(x, y+7*width, length, "Stone Influence", 1, 20, Board.stoneInfluence, new Color(0xB6,0x00,0x89).brighter(), Color.BLACK, val -> Board.stoneInfluence = (int)val));
        sliders.add(new Slider(x, y+8*width, length, "Gradients", 0, 1, BoardRenderer.useGradient? 1 : 0, new Color(0xB6,0x00,0x89).brighter(), Color.BLACK, val -> BoardRenderer.useGradient = ((int)val) == 1));

        y += width/2;

        sliders.add(new Slider(x, y+9*width, length, "Board", 0, 3, BoardRenderer.boardTypeIndex, Color.PINK, Color.BLACK, val -> BoardRenderer.boardTypeIndex = (int) val));
        sliders.add(new Slider(x, y+10*width, length, "Intersection Color", 0, 2, BoardRenderer.intersectionColor, Color.WHITE, Color.BLACK, val -> BoardRenderer.intersectionColor = (int)val));
        sliders.add(new Slider(x, y+11*width, length, "Show Stones", 0, 1, Omega.showStones? 1 : 0, Color.PINK, Color.BLACK, val -> Omega.showStones = (int) val == 1));
        sliders.add(new Slider(x, y+12*width, length, "Show Heatmap", 0, 1, Omega.showHeatmap? 1 : 0, Color.PINK, Color.BLACK, val -> Omega.showHeatmap = (int) val == 1));
        sliders.add(new Slider(x, y+13*width, length, "Outline Black", 0, 1, BoardRenderer.blackStoneOutline? 1 : 0, Color.PINK, Color.BLACK, val -> BoardRenderer.blackStoneOutline = (int) val == 1));

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

        for (Slider s : sliders) {
            s.render(g);
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
        } else {
            for (Slider s : sliders) {
                if (s.onClicked(x, y)) {
                    break;
                }
            }
        }
    }

    public void onDragged(int x, int y) {
        // check for board click
        int[] boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);

        if (boardCoordinates != null) {
            // dont do anything for drags
        } else {
            // but in case its not on the board, good for sliders
            for (Slider s : sliders) {
                if (s.onClicked(x, y)) {
                    break;
                }
            }
        }
    }

    public void onMouseMoved(int x, int y) {
        // TODO do we need this method?
    }
}