package featurecat.omega.analysis;

import featurecat.omega.Omega;
import featurecat.omega.rules.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

public class SGFParser {
    private static final SimpleDateFormat SGF_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final String[] listProps =
            new String[] {"LB", "CR", "SQ", "MA", "TR", "AB", "AW", "AE"};
    private static final String[] markupProps = new String[] {"LB", "CR", "SQ", "MA", "TR"};

    public static boolean load(String filename, Board... possibleBoard) throws IOException {
        // Clear the board
        Board board = possibleBoard.length > 0 ? possibleBoard[0] : Omega.board;
        while (board.previousMove()) ;

        File file = new File(filename);
        if (!file.exists() || !file.canRead()) {
            return false;
        }

        FileInputStream fp = new FileInputStream(file);
        InputStreamReader reader = new InputStreamReader(fp);
        StringBuilder builder = new StringBuilder();
        while (reader.ready()) {
            builder.append((char) reader.read());
        }
        reader.close();
        fp.close();
        String value = builder.toString();
        if (value.isEmpty()) {
            return false;
        }

        boolean returnValue = parse(value);
        return returnValue;
    }

    public static int[] convertSgfPosToCoord(String pos) {
        if (pos.equals("tt") || pos.isEmpty()) return null;
        int[] ret = new int[2];
        ret[0] = (int) pos.charAt(0) - 'a';
        ret[1] = (int) pos.charAt(1) - 'a';
        return ret;
    }

    private static boolean parse(String value) {
        // Drop anything outside "(;...)"
        final Pattern SGF_PATTERN = Pattern.compile("(?s).*?(\\(\\s*;.*\\)).*?");
        Matcher sgfMatcher = SGF_PATTERN.matcher(value);
        if (sgfMatcher.matches()) {
            value = sgfMatcher.group(1);
        } else {
            return false;
        }

        // dont want to have to support sz property. wouldnt be that hard to, though
//        // Determine the SZ property
//        Pattern szPattern = Pattern.compile("(?s).*?SZ\\[(\\d+)\\](?s).*");
//        Matcher szMatcher = szPattern.matcher(value);
//        if (szMatcher.matches()) {
//            Lizzie.board.reopen(Integer.parseInt(szMatcher.group(1)));
//        } else {
//            Lizzie.board.reopen(19);
//        }

        int subTreeDepth = 0;
        // Save the variation step count
        Map<Integer, Integer> subTreeStepMap = new HashMap<Integer, Integer>();
        // Comment of the game head
        String headComment = "";
        // Game properties
        Map<String, String> gameProperties = new HashMap<String, String>();
        Map<String, String> pendingProps = new HashMap<String, String>();
        boolean inTag = false,
                isMultiGo = false,
                escaping = false,
                moveStart = false,
                addPassForMove = true;
        boolean inProp = false;
        String tag = "";
        StringBuilder tagBuilder = new StringBuilder();
        StringBuilder tagContentBuilder = new StringBuilder();
        // MultiGo 's branch: (Main Branch (Main Branch) (Branch) )
        // Other 's branch: (Main Branch (Branch) Main Branch)
        if (value.matches("(?s).*\\)\\s*\\)")) {
            isMultiGo = true;
        }

        String blackPlayer = "", whitePlayer = "";

        // Support unicode characters (UTF-8)
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                // Any char following "\" is inserted verbatim
                // (ref) "3.2. Text" in https://www.red-bean.com/sgf/sgf4.html
                tagContentBuilder.append(c);
                escaping = false;
                continue;
            }
            switch (c) {
                case '(':
                    if (!inTag) {
                        subTreeDepth += 1;
                        // Initialize the step count
                        subTreeStepMap.put(subTreeDepth, 0);
                        addPassForMove = true;
                        pendingProps = new HashMap<String, String>();
                    } else {
                        if (i > 0) {
                            // Allow the comment tag includes '('
                            tagContentBuilder.append(c);
                        }
                    }
                    break;
                case ')':
                    if (!inTag) {
                        if (isMultiGo) {
                            // Restore to the variation node
                            int varStep = subTreeStepMap.get(subTreeDepth);
                            for (int s = 0; s < varStep; s++) {
                                Omega.board.previousMove();
                            }
                        }
                        subTreeDepth -= 1;
                    } else {
                        // Allow the comment tag includes '('
                        tagContentBuilder.append(c);
                    }
                    break;
                case '[':
                    if (!inProp) {
                        inProp = true;
                        if (subTreeDepth > 1 && !isMultiGo) {
                            break;
                        }
                        inTag = true;
                        String tagTemp = tagBuilder.toString();
                        if (!tagTemp.isEmpty()) {
                            // Ignore small letters in tags for the long format Smart-Go file.
                            // (ex) "PlayerBlack" ==> "PB"
                            // It is the default format of mgt, an old SGF tool.
                            // (Mgt is still supported in Debian and Ubuntu.)
                            tag = tagTemp.replaceAll("[a-z]", "");
                        }
                        tagContentBuilder = new StringBuilder();
                    } else {
                        tagContentBuilder.append(c);
                    }
                    break;
                case ']':
                    if (subTreeDepth > 1 && !isMultiGo) {
                        break;
                    }
                    inTag = false;
                    inProp = false;
                    tagBuilder = new StringBuilder();
                    String tagContent = tagContentBuilder.toString();
                    // We got tag, we can parse this tag now.
                    if (tag.equals("B") || tag.equals("W")) {
                        moveStart = true;
                        addPassForMove = true;
                        int[] move = convertSgfPosToCoord(tagContent);
                        // Save the step count
                        subTreeStepMap.put(subTreeDepth, subTreeStepMap.get(subTreeDepth) + 1);
                        Stone color = tag.equals("B") ? Stone.BLACK : Stone.WHITE;
                        boolean newBranch = (subTreeStepMap.get(subTreeDepth) == 1);
                        if (move == null) {
                            Omega.board.pass(color);
                        } else {
                            Omega.board.place(move[0], move[1], color);
                        }
                        if (newBranch) {
                            processPendingPros(pendingProps);
                        }
                    } else if (tag.equals("C")) {
                        // Support comment
                        if (!moveStart) {
                            headComment = tagContent;
                        } else {
//                            Lizzie.board.comment(tagContent);
                        }
                    } else if (tag.equals("AB") || tag.equals("AW")) {
                        int[] move = convertSgfPosToCoord(tagContent);
                        Stone color = tag.equals("AB") ? Stone.BLACK : Stone.WHITE;
                        if (moveStart) {
                            // add to node properties
//                            Lizzie.board.addNodeProperty(tag, tagContent);
                            if (addPassForMove) {
                                // Save the step count
                                subTreeStepMap.put(subTreeDepth, subTreeStepMap.get(subTreeDepth) + 1);
                                boolean newBranch = (subTreeStepMap.get(subTreeDepth) == 1);
                                Omega.board.pass(color);
                                if (newBranch) {
                                    processPendingPros(pendingProps);
                                }
                                addPassForMove = false;
                            }
//                            Lizzie.board.addNodeProperty(tag, tagContent);
                            if (move != null) {
                                Omega.board.place(move[0], move[1], color);
                            }
                        } else {
                            if (move == null) {
                                Omega.board.pass(color);
                            } else {
                                Omega.board.place(move[0], move[1], color);
                            }
//                            Omega.board.flatten();
                        }
                    } else if (tag.equals("PB")) {
                        blackPlayer = tagContent;
                    } else if (tag.equals("PW")) {
                        whitePlayer = tagContent;
                    } else if (tag.equals("KM")) {
                        try {
                            if (tagContent.trim().isEmpty()) {
                                tagContent = "0.0";
                            }
//                            Omega.board.getHistory().getGameInfo().setKomi(Double.parseDouble(tagContent));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (moveStart) {
                            // Other SGF node properties
                            if ("AE".equals(tag)) {
                                // remove a stone
                                if (addPassForMove) {
                                    // Save the step count
                                    subTreeStepMap.put(subTreeDepth, subTreeStepMap.get(subTreeDepth) + 1);
                                    Stone color =
                                            Omega.board.getHistory().getLastMoveColor() == Stone.WHITE
                                                    ? Stone.BLACK
                                                    : Stone.WHITE;
                                    boolean newBranch = (subTreeStepMap.get(subTreeDepth) == 1);
                                    Omega.board.pass(color);
                                    if (newBranch) {
                                        processPendingPros(pendingProps);
                                    }
                                    addPassForMove = false;
                                }
//                                Lizzie.board.addNodeProperty(tag, tagContent);
                                int[] move = convertSgfPosToCoord(tagContent);
                                if (move != null) {
//                                    Lizzie.board.removeStone(
//                                            move[0], move[1], tag.equals("AB") ? Stone.BLACK : Stone.WHITE);
                                }
                            } else {
                                boolean firstProp = (subTreeStepMap.get(subTreeDepth) == 0);
                                if (firstProp) {
                                    addProperty(pendingProps, tag, tagContent);
                                } else {
//                                    Lizzie.board.addNodeProperty(tag, tagContent);
                                }
                            }
                        } else {
                            addProperty(gameProperties, tag, tagContent);
                        }
                    }
                    break;
                case ';':
                    break;
                default:
                    if (subTreeDepth > 1 && !isMultiGo) {
                        break;
                    }
                    if (inTag) {
                        if (c == '\\') {
                            escaping = true;
                            continue;
                        }
                        tagContentBuilder.append(c);
                    } else {
                        if (c != '\n' && c != '\r' && c != '\t' && c != ' ') {
                            tagBuilder.append(c);
                        }
                    }
            }
        }

//        Lizzie.frame.setPlayers(whitePlayer, blackPlayer);

        // Rewind to game start
        while (Omega.board.previousMove()) ;

        // Set AW/AB Comment
//        if (!headComment.isEmpty()) {
//            Lizzie.board.comment(headComment);
//        }
//        if (gameProperties.size() > 0) {
//            Lizzie.board.addNodeProperties(gameProperties);
//        }

        return true;
    }

    public static String saveToString() throws IOException {
        try (StringWriter writer = new StringWriter()) {
            saveToStream(Omega.board, writer);
            return writer.toString();
        }
    }

    public static void save(Board board, String filename) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filename))) {
            saveToStream(board, writer);
        }
    }

    private static void saveToStream(Board board, Writer writer) throws IOException {
        // collect game info
        BoardHistoryList history = board.getHistory(); // (.getShallowCopy())

        // add SGF header
        StringBuilder builder = new StringBuilder("(;");
        StringBuilder generalProps = new StringBuilder("");
        generalProps.append(
                String.format(
                        "KM[%s]PW[%s]PB[%s]DT[%s]AP[Influencie: %s]",
                        "7.5", "White", "Black", "", Omega.version));

        // To append the winrate to the comment of sgf we might need to update the Winrate
//        if (Lizzie.config.appendWinrateToComment) {
//            Lizzie.board.updateWinrate();
//        }

        // move to the first move
        history.toStart();

        // Game properties
        //history.getData().addProperties(generalProps.toString());
        builder.append(generalProps.toString());
//        builder.append(history.getData().propertiesString());

        // add handicap stones to SGF
        {
            // Process the AW/AB stone
            Stone[] stones = history.getStones();
            StringBuilder abStone = new StringBuilder();
            StringBuilder awStone = new StringBuilder();
            for (int i = 0; i < stones.length; i++) {
                Stone stone = stones[i];
                if (stone.isBlack() || stone.isWhite()) {
                    // i = x * Board.BOARD_SIZE + y;
                    int corY = i % Board.BOARD_SIZE;
                    int corX = (i - corY) / Board.BOARD_SIZE;

                    char x = (char) (corX + 'a');
                    char y = (char) (corY + 'a');

                    if (stone.isBlack()) {
                        abStone.append(String.format("[%c%c]", x, y));
                    } else {
                        awStone.append(String.format("[%c%c]", x, y));
                    }
                }
            }
            if (abStone.length() > 0) {
                builder.append("AB").append(abStone);
            }
            if (awStone.length() > 0) {
                builder.append("AW").append(awStone);
            }
        }

        // The AW/AB Comment
//        if (!history.getData().comment.isEmpty()) {
//            builder.append(String.format("C[%s]", Escaping(history.getData().comment)));
//        }

        // replay moves, and convert them to tags.
        // *  format: ";B[xy]" or ";W[xy]"
        // *  with 'xy' = coordinates ; or 'tt' for pass.

        // Write variation tree
        builder.append(generateNode(board, history.getCurrentHistoryNode()));

        // close file
        builder.append(')');
        writer.append(builder.toString());
    }

    /** Generate node with variations */
    private static String generateNode(Board board, BoardHistoryNode node) throws IOException {
        StringBuilder builder = new StringBuilder("");

        if (node != null) {

            BoardData data = node.getData();
            String stone = "";
            if (Stone.BLACK.equals(data.lastMoveColor) || Stone.WHITE.equals(data.lastMoveColor)) {

                if (Stone.BLACK.equals(data.lastMoveColor)) stone = "B";
                else if (Stone.WHITE.equals(data.lastMoveColor)) stone = "W";

                builder.append(";");
                if (true) {
                    char x = data.lastMove != null ? (char) (data.lastMove[0] + 'a') : 't';
                    char y = data.lastMove != null ? (char) (data.lastMove[1] + 'a') : 't';
                    builder.append(String.format("%s[%c%c]", stone, x, y));
                }

                // Node properties
//                builder.append(data.propertiesString());

//                if (Lizzie.config.appendWinrateToComment) {
//                     Append the winrate to the comment of sgf
//                    data.comment = formatComment(node);
//                }

                // Write the comment
//                if (!data.comment.isEmpty()) {
//                    builder.append(String.format("C[%s]", Escaping(data.comment)));
//                }
            }

//            if (node.numberOfChildren() > 1) {
                // Variation
//                for (BoardHistoryNode sub : node.getVariations()) {
//                    builder.append("(");
//                    builder.append(generateNode(board, sub));
//                    builder.append(")");
//                }
            if (node.hasNext()) {
                builder.append(generateNode(board, node.next()));
            } else {
                return builder.toString();
            }
        }

        return builder.toString();
    }

    public static boolean isListProperty(String key) {
        return asList(listProps).contains(key);
    }

    public static boolean isMarkupProperty(String key) {
        return asList(markupProps).contains(key);
    }

    /**
     * Get a value with key, or the default if there is no such key
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getOrDefault(Map<String, String> props, String key, String defaultValue) {
        return props.getOrDefault(key, defaultValue);
    }

    /**
     * Add a key and value to the props
     *
     * @param key
     * @param value
     */
    public static void addProperty(Map<String, String> props, String key, String value) {
        if (SGFParser.isListProperty(key)) {
            // Label and add/remove stones
            props.merge(key, value, (old, val) -> old + "," + val);
        } else {
            props.put(key, value);
        }
    }

    /**
     * Add the properties by mutating the props
     *
     * @return
     */
    public static void addProperties(Map<String, String> props, Map<String, String> addProps) {
        addProps.forEach((key, value) -> addProperty(props, key, value));
    }

    /**
     * Add the properties from string
     *
     * @return
     */
    public static void addProperties(Map<String, String> props, String propsStr) {
        boolean inTag = false, escaping = false;
        String tag = "";
        StringBuilder tagBuilder = new StringBuilder();
        StringBuilder tagContentBuilder = new StringBuilder();

        for (int i = 0; i < propsStr.length(); i++) {
            char c = propsStr.charAt(i);
            if (escaping) {
                tagContentBuilder.append(c);
                escaping = false;
                continue;
            }
            switch (c) {
                case '(':
                    if (inTag) {
                        if (i > 0) {
                            tagContentBuilder.append(c);
                        }
                    }
                    break;
                case ')':
                    if (inTag) {
                        tagContentBuilder.append(c);
                    }
                    break;
                case '[':
                    inTag = true;
                    String tagTemp = tagBuilder.toString();
                    if (!tagTemp.isEmpty()) {
                        tag = tagTemp.replaceAll("[a-z]", "");
                    }
                    tagContentBuilder = new StringBuilder();
                    break;
                case ']':
                    inTag = false;
                    tagBuilder = new StringBuilder();
                    addProperty(props, tag, tagContentBuilder.toString());
                    break;
                case ';':
                    break;
                default:
                    if (inTag) {
                        if (c == '\\') {
                            escaping = true;
                            continue;
                        }
                        tagContentBuilder.append(c);
                    } else {
                        if (c != '\n' && c != '\r' && c != '\t' && c != ' ') {
                            tagBuilder.append(c);
                        }
                    }
            }
        }
    }

    /**
     * Get properties string by the props
     *
     * @return
     */
    public static String propertiesString(Map<String, String> props) {
        StringBuilder sb = new StringBuilder();
        props.forEach((key, value) -> sb.append(nodeString(key, value)));
        return sb.toString();
    }

    /**
     * Get node string by the key and value
     *
     * @param key
     * @param value
     * @return
     */
    public static String nodeString(String key, String value) {
        StringBuilder sb = new StringBuilder();
        if (SGFParser.isListProperty(key)) {
            // Label and add/remove stones
            sb.append(key);
            String[] vals = value.split(",");
            for (String val : vals) {
                sb.append("[").append(val).append("]");
            }
        } else {
            sb.append(key).append("[").append(value).append("]");
        }
        return sb.toString();
    }

    private static void processPendingPros(Map<String, String> props) {
//        props.forEach((key, value) -> Omega.board.addNodeProperty(key, value));
//        props = new HashMap<String, String>();
    }

    public static String Escaping(String in) {
        String out = in.replaceAll("\\\\", "\\\\\\\\");
        return out.replaceAll("\\]", "\\\\]");
    }
}