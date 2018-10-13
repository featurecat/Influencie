package featurecat.omega.rules;

import featurecat.omega.analysis.SGFParser;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

public class Search {

    /**
     * Get all matching positions in a file
     *
     * @param position the position to match
     * @param filename the file to search
     * @return arraylist of all matching positions
     */
    public ArrayList<SearchData> getMatchingPositions(Stone[] position, String filename) {
        Board fileBoard = new Board();
        try {
            System.out.println(filename);
            SGFParser.load(filename, fileBoard);
        } catch (IOException err) {
            JOptionPane.showConfirmDialog(null, "Failed to open the SGF file.", "Error", JOptionPane.ERROR);
        }
        BoardHistoryList boardHistoryList = fileBoard.getHistory();
        ArrayList<SearchData> positionList = new ArrayList<SearchData>();
        while (true) {
            BoardData boardData = boardHistoryList.next();
            if (boardData == null) {
                break;
            }
            Stone[] symmetricPosition = compareBoardPositions(position, boardData.stones, boardData.lastMove);
            if (symmetricPosition != null) {
                positionList.add(new SearchData(symmetricPosition, boardHistoryList));
            }
        }
        return positionList;
    }

    /**
     * Check if the position is in the file
     *
     * @param position the position to examine
     * @param fileStones the file to search the position
     * @param lastMove last move, must be in the position
     * @return the symmetric position that the fileStones match
     */
    private Stone[] compareBoardPositions(Stone[] position, Stone[] fileStones, int[] lastMove) {
        for (int mode = 0; mode < 8; mode ++) {
            Stone[] symmetricPosition = getSymmetricStones(position, mode);
            for (int i = 0; i < Board.BOARD_SIZE * Board.BOARD_SIZE; i++) {
                if ((!symmetricPosition[i].equals(Stone.UNSPECIFIED) ||
                        (i == lastMove[0] * Board.BOARD_SIZE + lastMove[1])) &&
                        !symmetricPosition[i].equals(fileStones[i])) {
                    break;
                }
                else if (i == Board.BOARD_SIZE * Board.BOARD_SIZE - 1) {
                    return symmetricPosition;
                }
            }
        }
        return null;
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
