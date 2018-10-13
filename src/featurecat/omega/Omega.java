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

        Stone[] position = new Stone[Board.BOARD_SIZE * Board.BOARD_SIZE];
        for (int x = 0; x < Board.BOARD_SIZE; x++) {
            for (int y = 0; y < Board.BOARD_SIZE; y++) {
                if (x == 13 && y == 2 || x == 15 && y == 2 || x == 16 && y == 3 || x == 16 && y == 4) {
                    position[x * Board.BOARD_SIZE + y] = Stone.BLACK;
                }
                else if (x == 15 && y == 4 || x == 15 && y == 5 || x == 16 && y == 5 || x == 16 && y == 9) {
                    position[x * Board.BOARD_SIZE + y] = Stone.WHITE;
                }
                else {
                    position[x * Board.BOARD_SIZE + y] = Stone.UNSPECIFIED;
                }
            }
        }
        String filename = "/Users/weiqiuyou/Documents/Weiqi/Yunguseng/201810/fallcat-DrWhom.sgf";
        ArrayList<SearchData> list = Search.getMatchingPositions(position, filename);
        System.out.println(list.size());
    }

    public static void shutdown() {
        leelaz.shutdown();
        System.exit(0);
    }

}
