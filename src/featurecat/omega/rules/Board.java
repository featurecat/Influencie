package featurecat.omega.rules;

import com.sun.jdi.DoubleValue;
import featurecat.omega.Omega;
import featurecat.omega.ui.BoardRenderer;
import featurecat.omega.ui.PlaceMode;

import java.util.*;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;

public class Board {
    public static final int BOARD_SIZE = 19;
    private final static String alphabet = "ABCDEFGHJKLMNOPQRST";

    private BoardHistoryList history;

    public Board() {
        Stone[] stones = new Stone[BOARD_SIZE * BOARD_SIZE];
        for (int i = 0; i < stones.length; i++)
            stones[i] = Stone.EMPTY;

        boolean blackToPlay = true;
        int[] lastMove = null;

        history = new BoardHistoryList(new BoardData(stones, lastMove, Stone.EMPTY, blackToPlay, new Zobrist(), 0, new int[BOARD_SIZE * BOARD_SIZE]));
    }

    public Board(BoardHistoryList history) {
        Stone[] stones = new Stone[BOARD_SIZE * BOARD_SIZE];
        for (int i = 0; i < stones.length; i++)
            stones[i] = Stone.EMPTY;

        boolean blackToPlay = true;
        int[] lastMove = null;

        this.history = history;
    }

    /**
     * Calculates the array index of a stone stored at (x, y)
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the array index
     */
    public static int getIndex(int x, int y) {
        return getIndex(x, y, BOARD_SIZE);
    }

    public static int getIndex(int x, int y, int LENGTH) {
        return x * LENGTH + y;
    }

    /**
     * Converts a named coordinate eg C16, T5, K10, etc to an x and y coordinate
     *
     * @param namedCoordinate a capitalized version of the named coordinate. Must be a valid 19x19 Go coordinate, without I
     * @return an array containing x, followed by y
     */
    public static int[] convertNameToCoordinates(String namedCoordinate) {
        namedCoordinate = namedCoordinate.trim();
        // coordinates take the form C16 A19 Q5 K10 etc. I is not used.
        int x = alphabet.indexOf(namedCoordinate.charAt(0));
        int y = Integer.parseInt(namedCoordinate.substring(1)) - 1;
        return new int[]{x, y};
    }

    /**
     * Converts a x and y coordinate to a named coordinate eg C16, T5, K10, etc
     *
     * @param x x coordinate -- must be valid
     * @param y y coordinate -- must be valid
     * @return a string representing the coordinate
     */
    public static String convertCoordinatesToName(int x, int y) {
        // coordinates take the form C16 A19 Q5 K10 etc. I is not used.
        return alphabet.charAt(x) + "" + (y + 1);
    }

    /**
     * Checks if a coordinate is valid
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return whether or not this coordinate is part of the board
     */
    public static boolean isValid(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }

    /**
     * The pass. Thread safe
     *
     * @param color the type of pass
     */
    public void pass(Stone color) {
        synchronized (this) {

            // check to see if this move is being replayed in history
            BoardData next = history.getNext();
            if (next != null && next.lastMove == null) {
                // this is the next move in history. Just increment history so that we don't erase the redo's
                history.next();
                Omega.frame.repaint();
                return;
            }

            Stone[] stones = history.getStones().clone();
            Zobrist zobrist = history.getZobrist();
            int moveNumber = history.getMoveNumber() + 1;
            int[] moveNumberList = history.getMoveNumberList().clone();

            // build the new game state
            BoardData newState = new BoardData(stones, null, color, !history.isBlacksTurn(), zobrist, moveNumber, moveNumberList);


            // update history with pass
            history.add(newState);

            Omega.frame.repaint();
        }
    }

    /**
     * overloaded method for pass(), chooses color in an alternating pattern
     */
    public void pass() {
        pass(history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE);
    }

    /**
     * Places a stone onto the board representation. Thread safe
     *
     * @param x     x coordinate
     * @param y     y coordinate
     * @param color the type of stone to place
     */
    public void place(int x, int y, Stone color) {
        synchronized (this) {
            if (!isValid(x, y) || history.getStones()[getIndex(x, y)] != Stone.EMPTY)
                return;

            // check to see if this coordinate is being replayed in history
            BoardData next = history.getNext();
            if (next != null && next.lastMove != null && next.lastMove[0] == x && next.lastMove[1] == y) {
                // this is the next coordinate in history. Just increment history so that we don't erase the redo's
                history.next();
                Omega.frame.repaint();
                // should be opposite from the bottom case
                return;
            }

            // load a copy of the data at the current node of history
            Stone[] stones = history.getStones().clone();
            Zobrist zobrist = history.getZobrist();
            int[] lastMove = new int[]{x, y}; // keep track of the last played stone
            int moveNumber = history.getMoveNumber() + 1;
            int[] moveNumberList = history.getMoveNumberList().clone();

            moveNumberList[Board.getIndex(x, y)] = moveNumber;

            // set the stone at (x, y) to color
            stones[getIndex(x, y)] = color;
            zobrist.toggleStone(x, y, color);

            // remove enemy stones
            removeDeadChain(x + 1, y, color.opposite(), stones, zobrist);
            removeDeadChain(x, y + 1, color.opposite(), stones, zobrist);
            removeDeadChain(x - 1, y, color.opposite(), stones, zobrist);
            removeDeadChain(x, y - 1, color.opposite(), stones, zobrist);

            // check to see if the player made a suicidal coordinate
            boolean isSuicidal = removeDeadChain(x, y, color, stones, zobrist);

            for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
                if (stones[i].equals(Stone.EMPTY)) {
                    moveNumberList[i] = 0;
                }
            }

            // build the new game state
            BoardData newState = new BoardData(stones, lastMove, color, !history.isBlacksTurn(), zobrist, moveNumber, moveNumberList);

            // don't make this coordinate if it is suicidal or violates superko
            if (isSuicidal || history.violatesSuperko(newState))
                return;

            // update history with this coordinate
            history.add(newState);

            Omega.frame.repaint();
        }
    }

    /**
     * overloaded method for place(), chooses color in an alternating pattern
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void place(int x, int y) {
        if (Omega.placeMode == PlaceMode.REMOVE) {
            deleteStone(x, y);
            return;
        }

        Stone colorToPlay;

        switch (Omega.placeMode) {
            case BLACK:
                colorToPlay = Stone.BLACK;
                break;

            case WHITE:
                colorToPlay = Stone.WHITE;
                break;

            case ALTERNATING: // TODO we need a better way of choosing the color for alternating.
            default:
                colorToPlay = history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE;
                break;
        }
        place(x, y, colorToPlay);
    }

    /**
     * overloaded method for place. To be used by the LeelaZ engine. Color is then assumed to be alternating
     *
     * @param namedCoordinate the coordinate to place a stone,
     */
    public void place(String namedCoordinate) {
        if (namedCoordinate.contains("pass")) {
            pass(history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE);
            return;
        }

        int[] coordinates = convertNameToCoordinates(namedCoordinate);

        place(coordinates[0], coordinates[1]);
    }

    /**
     * Removes a chain if it has no liberties
     *
     * @param x       x coordinate -- needn't be valid
     * @param y       y coordinate -- needn't be valid
     * @param color   the color of the chain to remove
     * @param stones  the stones array to modify
     * @param zobrist the zobrist object to modify
     * @return whether or not stones were removed
     */
    private boolean removeDeadChain(int x, int y, Stone color, Stone[] stones, Zobrist zobrist) {
        if (!isValid(x, y) || stones[getIndex(x, y)] != color)
            return false;

        boolean hasLiberties = hasLibertiesHelper(x, y, color, stones);

        // either remove stones or reset what hasLibertiesHelper does to the board
        cleanupHasLibertiesHelper(x, y, color.recursed(), stones, zobrist, !hasLiberties);

        // if hasLiberties is false, then we removed stones
        return !hasLiberties;
    }

    /**
     * Recursively determines if a chain has liberties. Alters the state of stones, so it must be counteracted
     *
     * @param x      x coordinate -- needn't be valid
     * @param y      y coordinate -- needn't be valid
     * @param color  the color of the chain to be investigated
     * @param stones the stones array to modify
     * @return whether or not this chain has liberties
     */
    private boolean hasLibertiesHelper(int x, int y, Stone color, Stone[] stones) {
        if (!isValid(x, y))
            return false;

        if (stones[getIndex(x, y)] == Stone.EMPTY)
            return true; // a liberty was found
        else if (stones[getIndex(x, y)] != color)
            return false; // we are either neighboring an enemy stone, or one we've already recursed on

        // set this index to be the recursed color to keep track of where we've already searched
        stones[getIndex(x, y)] = color.recursed();

        // set removeDeadChain to true if any recursive calls return true. Recurse in all 4 directions
        boolean hasLiberties = hasLibertiesHelper(x + 1, y, color, stones) ||
                hasLibertiesHelper(x, y + 1, color, stones) ||
                hasLibertiesHelper(x - 1, y, color, stones) ||
                hasLibertiesHelper(x, y - 1, color, stones);

        return hasLiberties;
    }

    /**
     * cleans up what hasLibertyHelper does to the board state
     *
     * @param x            x coordinate -- needn't be valid
     * @param y            y coordinate -- needn't be valid
     * @param color        color to clean up. Must be a recursed stone type
     * @param stones       the stones array to modify
     * @param zobrist      the zobrist object to modify
     * @param removeStones if true, we will remove all these stones. otherwise, we will set them to their unrecursed version
     */
    private void cleanupHasLibertiesHelper(int x, int y, Stone color, Stone[] stones, Zobrist zobrist, boolean removeStones) {
        if (!isValid(x, y) || stones[getIndex(x, y)] != color)
            return;

        if (removeStones) {
            deleteStone(x, y, stones, zobrist);
        } else {
            stones[getIndex(x, y)] = color.unrecursed();
        }

        // use the flood fill algorithm to replace all adjacent recursed stones
        cleanupHasLibertiesHelper(x + 1, y, color, stones, zobrist, removeStones);
        cleanupHasLibertiesHelper(x, y + 1, color, stones, zobrist, removeStones);
        cleanupHasLibertiesHelper(x - 1, y, color, stones, zobrist, removeStones);
        cleanupHasLibertiesHelper(x, y - 1, color, stones, zobrist, removeStones);
    }

    private void deleteStone(int x, int y, Stone[] stones, Zobrist zobrist) {
        Stone color = stones[getIndex(x, y)];
        stones[getIndex(x, y)] = Stone.EMPTY;
        zobrist.toggleStone(x, y, color.unrecursed());
    }

    private void deleteStone(int x, int y) {
        if (getStones()[getIndex(x, y)] == Stone.EMPTY) {
            return;
        }

        // load a copy of the data at the current node of history
        Stone[] stones = history.getStones().clone();
        Zobrist zobrist = history.getZobrist();
        int moveNumber = history.getMoveNumber() + 1;
        int[] moveNumberList = history.getMoveNumberList().clone();

        deleteStone(x, y, stones, zobrist);

        // build the new game state
        BoardData newState = new BoardData(stones, null, Stone.EMPTY, !history.isBlacksTurn(), zobrist, moveNumber, moveNumberList);

        // TODO how to update leelaz? This might be a problem.
        // Omega.leelaz.playMove(color, "pass");

        // update history
        history.add(newState);

        Omega.frame.repaint();
    }

    /**
     * get current board state
     *
     * @return the stones array corresponding to the current board state
     */
    public Stone[] getStones() {
        return history.getStones();
    }

    /**
     * shows where to mark the last coordinate
     *
     * @return the last played stone
     */
    public int[] getLastMove() {
        return history.getLastMove();
    }

    /**
     * get current board move number
     *
     * @return the int array corresponding to the current board move number
     */
    public int[] getMoveNumberList() {
        return history.getMoveNumberList();
    }

    /**
     * Goes to the next coordinate, thread safe
     */
    public boolean nextMove() {
        synchronized (this) {
            if (history.next() != null) {
                // update leelaz board position, before updating to next node
                history.getData(); // todo not needed just kept for paranoia
                Omega.frame.repaint();
                return true;
            }
            return false;
        }
    }

    public BoardData getData() {
        return history.getData();
    }

    public BoardHistoryList getHistory() {
        return history;
    }

    /**
     * Goes to the previous coordinate, thread safe
     */
    public boolean previousMove() {
        synchronized (this) {
            if (history.previous() != null) {
                Omega.frame.repaint();
                return true;
            }
            return false;
        }
    }

    public static int stoneInfluence = 7;

    public double[] getInfluenceHeatmap() {
        double[] heatmap = new double[BOARD_SIZE * BOARD_SIZE];

        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                int index = getIndex(x, y);
                if (getStones()[index] != Stone.EMPTY) {
                    int initial = getStones()[index].isBlack() ? 1 : -1;
                    floodAdd(heatmap, new boolean[BOARD_SIZE * BOARD_SIZE], x, y, x, y, initial, 1.0 / stoneInfluence);
                }
            }
        }

        if (BoardRenderer.useGradient) {
            // prevent stones from being too bright
            for (int x = 0; x < BOARD_SIZE; x++) {
                for (int y = 0; y < BOARD_SIZE; y++) {
                    int index = getIndex(x, y);
                    if (getStones()[index].isBlack()) {
                        heatmap[index] = Math.max(0, Math.min(heatmap[index], 0.5));
                    } else if (getStones()[index].isWhite()) {
                        heatmap[index] = Math.min(0, Math.max(heatmap[index], -0.5));
                    }
                }
            }
        }

        return Arrays.stream(heatmap).map(val -> Math.max(0, Math.min(1, (val + 1) / 2))).toArray();
    }

    private void floodAdd(double[] heatmap, boolean[] seen, int originalX, int originalY, int x, int y, double initial, double degrade) {
        if (isValid(x, y) && !seen[getIndex(x, y)]) { // we havent seen before
            heatmap[getIndex(x, y)] += initial;
            seen[getIndex(x, y)] = true;
            if (initial > 0) {
                // if we run into enemy, we cant repopulate, is the rule.
                if (getStones()[getIndex(x, y)].isWhite()) {
                    return;
                }
                initial = Math.max(0, initial - degrade);
            } else {
                if (getStones()[getIndex(x, y)].isBlack()) {
                    return;
                }
                initial = Math.min(0, initial + degrade);
            }
            if (initial != 0) {
                if (originalX - x >= 0)
                    floodAdd(heatmap, seen, originalX, originalY, x - 1, y, initial, degrade);
                if (originalX - x <= 0)
                    floodAdd(heatmap, seen, originalX, originalY, x + 1, y, initial, degrade);
                if (originalY - y >= 0)
                    floodAdd(heatmap, seen, originalX, originalY, x, y - 1, initial, degrade);
                if (originalY - y <= 0)
                    floodAdd(heatmap, seen, originalX, originalY, x, y + 1, initial, degrade);
            }
        }
    }

}
