package featurecat.omega;

import featurecat.omega.analysis.Leelaz;
import featurecat.omega.rules.Board;
import featurecat.omega.rules.Search;
import featurecat.omega.rules.SearchData;
import featurecat.omega.rules.Stone;
import featurecat.omega.ui.OmegaFrame;
import featurecat.omega.ui.PlaceMode;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Main class.
 */
public class Omega {
    public static OmegaFrame frame;
    public static Leelaz leelaz;
    public static Board board;
    public static String version = "0.1";

    public static final String NETWORK_STRING = "network.gz"; // todo fix hardcoded values
    public static PlaceMode placeMode = PlaceMode.ALTERNATING;

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
