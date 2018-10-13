package featurecat.omega;

import featurecat.omega.analysis.Leelaz;
import featurecat.omega.rules.Board;
import featurecat.omega.ui.OmegaFrame;

import java.io.IOException;

/**
 * Main class.
 */
public class Omega {
    public static OmegaFrame frame;
    public static Leelaz leelaz;
    public static Board board;
    public static String version = "0.1";

    /**
     * Launches the game window, and runs the game.
     */
    public static void main(String[] args) throws IOException {
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
