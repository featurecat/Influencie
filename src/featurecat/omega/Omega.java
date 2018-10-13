package featurecat.omega;

import featurecat.omega.analysis.Leelaz;
import featurecat.omega.rules.Board;
import featurecat.omega.ui.OmegaFrame;

import javax.swing.*;
import java.io.IOException;

/**
 * Main class.
 */
public class Omega {
    public static OmegaFrame frame;
    public static Leelaz leelaz;
    public static Board board;
    public static String version = "0.1";

    public static final String NETWORK_STRING = "network.gz"; // todo fix hardcoded values

    /**
     * Launches the game window, and runs the game.
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        leelaz = new Leelaz();
        leelaz.togglePonder();

        board = new Board();
        frame = new OmegaFrame();
    }

    public static void shutdown() {
        leelaz.shutdown();
        System.exit(0);
    }

}
