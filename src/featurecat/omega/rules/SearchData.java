package featurecat.omega.rules;

import java.util.ArrayList;

public class SearchData {
    public Stone[] symmetricPosition;
    public BoardHistoryList boardHistoryList;

    public SearchData (Stone[] symmetricPosition, BoardHistoryList boardHistoryList) {
        this.symmetricPosition = symmetricPosition;
        this.boardHistoryList = boardHistoryList;
    }
}
