package flute.jdtparser.statistics.ps;

public class TestCase {
    private String filePath;
    private int line, col;
    private int argPos;

    public TestCase(String filePath, int line, int col, int argPos) {
        this.filePath = filePath;
        this.line = line;
        this.col = col;
        this.argPos = argPos;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getArgPos() {
        return argPos;
    }

    public void setArgPos(int argPos) {
        this.argPos = argPos;
    }
}
