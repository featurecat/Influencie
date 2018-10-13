package featurecat.omega.rules;

public class SearchData {
    private Board board;
    private BoardHistoryNode boardHistoryNode;

    public SearchData (Board board, BoardHistoryNode boardHistoryNode) {
        this.board = board;
        this.boardHistoryNode = boardHistoryNode;
    }

    public Board getBoard() {
        return board;
    }

    public BoardHistoryNode getBoardHistoryNode() {
        return boardHistoryNode;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public void setBoardHistoryNode(BoardHistoryNode boardHistoryNode) {
        this.boardHistoryNode = boardHistoryNode;
    }
}
