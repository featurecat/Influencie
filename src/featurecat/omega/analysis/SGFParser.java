package featurecat.omega.analysis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import featurecat.omega.Omega;
import featurecat.omega.rules.Stone;

import java.io.*;
import java.text.SimpleDateFormat;

public class SGFParser {
    private static final SimpleDateFormat SGF_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static boolean load(String filename) throws IOException {
        // Clear the board
        Omega.board.clear();

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

    public static boolean loadFromString(String sgfString) {
        // Clear the board
        Omega.board.clear();

        return parse(sgfString);
    }

    public static int[] convertSgfPosToCoord(String pos) {
        if (pos.equals("tt") || pos.isEmpty())
            return null;
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
        int subTreeDepth = 0;
        boolean inTag = false, isMultiGo = false, escaping = false;
        String tag = null;
        StringBuilder tagBuilder = new StringBuilder();
        StringBuilder tagContentBuilder = new StringBuilder();
        // MultiGo 's branch: (Main Branch (Main Branch) (Branch) )
        // Other 's branch: (Main Branch (Branch) Main Branch)
        if (value.charAt(value.length() - 2) == ')') {
            isMultiGo = true;
        }

        String blackPlayer = "", whitePlayer = "";

        PARSE_LOOP:
        for (byte b : value.getBytes()) {
            // Check unicode charactors (UTF-8)
            char c = (char) b;
            if (((int) b & 0x80) != 0) {
                continue;
            }
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
                    }
                    break;
                case ')':
                    if (!inTag) {
                        subTreeDepth -= 1;
                        if (isMultiGo) {
                            break PARSE_LOOP;
                        }
                    }
                    break;
                case '[':
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
                    break;
                case ']':
                    if (subTreeDepth > 1 && !isMultiGo) {
                        break;
                    }
                    inTag = false;
                    tagBuilder = new StringBuilder();
                    String tagContent = tagContentBuilder.toString();
                    // We got tag, we can parse this tag now.
                    if (tag.equals("B")) {
                        int[] move = convertSgfPosToCoord(tagContent);
                        if (move == null) {
                            Omega.board.pass(Stone.BLACK);
                        } else {
                            Omega.board.place(move[0], move[1], Stone.BLACK);
                        }
                    } else if (tag.equals("W")) {
                        int[] move = convertSgfPosToCoord(tagContent);
                        if (move == null) {
                            Omega.board.pass(Stone.WHITE);
                        } else {
                            Omega.board.place(move[0], move[1], Stone.WHITE);
                        }
                    } else if (tag.equals("AB")) {
                        int[] move = convertSgfPosToCoord(tagContent);
                        if (move == null) {
                            Omega.board.pass(Stone.BLACK);
                        } else {
                            Omega.board.place(move[0], move[1], Stone.BLACK);
                        }
                        Omega.board.flatten();
                    } else if (tag.equals("AW")) {
                        int[] move = convertSgfPosToCoord(tagContent);
                        if (move == null) {
                            Omega.board.pass(Stone.WHITE);
                        } else {
                            Omega.board.place(move[0], move[1], Stone.WHITE);
                        }
                        Omega.board.flatten();
                    } else if (tag.equals("PB")) {
                        blackPlayer = tagContent;
                    } else if (tag.equals("PW")) {
                        whitePlayer = tagContent;
                    }  else if (tag.equals("KM")) {
                        Omega.board.getHistory().getGameInfo().setKomi(Double.parseDouble(tagContent));
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

        // Rewind to game start
        while (Omega.board.previousMove()) ;

        return true;
    }
}
