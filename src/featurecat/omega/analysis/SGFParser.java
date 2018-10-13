package featurecat.omega.analysis;

import featurecat.omega.Omega;
import featurecat.omega.rules.BoardData;
import featurecat.omega.rules.BoardHistoryList;
import featurecat.omega.rules.Stone;

import java.io.*;

public class SGFParser {
    public static boolean load(String filename) throws IOException {
        // Clear the board
        while (Omega.board.previousMove()) ;

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
        String value = builder.toString();
        if (value.isEmpty()) {
            return false;
        }
        reader.close();
        fp.close();
        return parse(value);
    }

    private static boolean parse(String value) {
        value = value.trim();
        if (value.charAt(0) != '(') {
            return false;
        } else if (value.charAt(value.length() - 1) != ')') {
            return false;
        }
        boolean isTag = false, isInTag = false, inSubTree = false, inTree = false;
        String tag = null;
        char last = '(';
        StringBuilder tagBuffer = new StringBuilder();
        StringBuilder tagContentBuffer = new StringBuilder();
        char[] charList = value.toCharArray();
        for (char c : charList) {
            if (c == '(') {
                if (!inTree) {
                    inTree = true;
                } else {
                    inSubTree = true;
                }
                continue;
            }
            if (inSubTree && !isInTag && c == ')') {
                inSubTree = false;
                continue;
            }
            if (c == ';') {
                isTag = true;
                continue;
            }
            if (c == '[') {
                isTag = false;
                tag = tagBuffer.toString();
                tagBuffer = new StringBuilder();
                isInTag = true;
                continue;
            }
            if (c == ']') {
                isInTag = false;
                String tagContent = tagContentBuffer.toString();
                tagContentBuffer = new StringBuilder();
                if (tag == null) {
                    return false;
                } else if (tag.equals("B")) {
                    if (tagContent.isEmpty() || tagContent.equals("tt")) {
                        Omega.board.pass(Stone.WHITE);
                    } else {
                        int x = tagContent.charAt(0) - 'a';
                        int y = tagContent.charAt(1) - 'a';
                        Omega.board.place(x, y, Stone.BLACK);
                    }
                } else if (tag.equals("W")) {
                    if (tagContent.isEmpty() || tagContent.equals("tt")) {
                        Omega.board.pass(Stone.WHITE);
                    } else {
                        int x = tagContent.charAt(0) - 'a';
                        int y = tagContent.charAt(1) - 'a';
                        Omega.board.place(x, y, Stone.WHITE);
                    }
                }
                continue;
            }
            // TODO is there a bug here? this is handled in a previous branch.
            if (last == ']' && c != '(') {
                isTag = true;
            }
            if (inSubTree) {
                continue;
            }
            if (isTag) {
                tagBuffer.append(c);
                continue;
            }
            if (isInTag) {
                tagContentBuffer.append(c);
            }
        }
        return true;
    }
}
