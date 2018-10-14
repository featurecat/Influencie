package featurecat.omega.rules;

import java.util.ArrayList;

public class SearchData {
    public Stone[] symmetricPosition;
    public BoardHistoryList boardHistoryList;
    public int moveNumber;

    public SearchData (Stone[] symmetricPosition, BoardHistoryList boardHistoryList, int moveNumber) {
        this.symmetricPosition = symmetricPosition;
        this.boardHistoryList = boardHistoryList;
        this.moveNumber = moveNumber;
    }
}
