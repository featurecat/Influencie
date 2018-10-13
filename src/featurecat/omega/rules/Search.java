package featurecat.omega.rules;

import featurecat.omega.analysis.SGFParser;

import javax.swing.*;
import java.io.IOException;

public class Search {
    public SearchData getMatchedPosition(Stone[] position, String filename) {
        try {
            System.out.println(filename);
            SGFParser.load(filename);
        } catch (IOException err) {
            JOptionPane.showConfirmDialog(null, "Failed to open the SGF file.", "Error", JOptionPane.ERROR);
        }
        return null;
    }

    /**
     * Check if the position is in the file
     *
     * @param position the position to examine
     * @param fileBoard the file to search the position
     * @return true if the position is in the file, false if not
     */
    private boolean compareBoardPositions(Stone[] position, Stone[] fileBoard) {
        for (int mode = 0; mode < 8; mode ++) {
            Stone[] symmetricPosition = getSymmetricStones(position, mode);
            for (int i = 0; i < Board.BOARD_SIZE * Board.BOARD_SIZE; i++) {
                if (!symmetricPosition[i].equals(Stone.UNSPECIFIED) && !symmetricPosition[i].equals(fileBoard[i])) {
                    break;
                }
                else if (i == Board.BOARD_SIZE * Board.BOARD_SIZE - 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get 8 different symmetry stones
     *
     * @param original original board position
     * @param mode symmetry mode, 0-3 is rotating the board clockwise 90 degrees at a time,
     *             4-7 flips the board along y=x first and then rotate.
     * @return transformed board
     */
    private Stone[] getSymmetricStones(Stone[] original, int mode) {
        Stone[] symmetry = new Stone[Board.BOARD_SIZE * Board.BOARD_SIZE];
        if (mode >= 4) {
            for (int x = 0; x < Board.BOARD_SIZE; x++) {
                for (int y = 0; y < Board.BOARD_SIZE; y++) {
                    symmetry[y * Board.BOARD_SIZE + x] = original[x * Board.BOARD_SIZE + y];
                }
            }
            mode -= 4;
        }
        else {
            symmetry = original.clone();
        }
        if (mode % 2 == 1) {
            for (int x = 0; x < Board.BOARD_SIZE / 2; x++) {
                for (int y = 0; y < Board.BOARD_SIZE / 2; y++) {
                    Stone temp = symmetry[x * Board.BOARD_SIZE + y];
                    symmetry[x * Board.BOARD_SIZE + y] = symmetry[y * Board.BOARD_SIZE + (Board.BOARD_SIZE - 1 - x)];
                    symmetry[y * Board.BOARD_SIZE + (Board.BOARD_SIZE - 1 - x)] = symmetry[(Board.BOARD_SIZE - 1 - x) * Board.BOARD_SIZE + (Board.BOARD_SIZE - 1 - y)];
                    symmetry[(Board.BOARD_SIZE - 1 - x) * Board.BOARD_SIZE + (Board.BOARD_SIZE - 1 - y)] = symmetry[(Board.BOARD_SIZE - 1 - y) * Board.BOARD_SIZE + x];
                    symmetry[(Board.BOARD_SIZE - 1 - y) * Board.BOARD_SIZE + x] = temp;
                }
            }
        }
        if (mode == 2) {
            for (int i = 0; i < Board.BOARD_SIZE * Board.BOARD_SIZE / 2; i ++) {
                Stone temp = symmetry[i];
                symmetry[i] = symmetry[Board.BOARD_SIZE * Board.BOARD_SIZE - 1 - i];
                symmetry[Board.BOARD_SIZE * Board.BOARD_SIZE - 1 - i] = temp;
            }
        }
        return symmetry;
    }
}
