package featurecat.omega.analysis;

import featurecat.omega.Omega;
import featurecat.omega.rules.Stone;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * an interface with leelaz.exe go engine. Can be adapted for GTP, but is specifically designed for GCP's Leela Zero.
 * leelaz is modified to output information as it ponders
 * see www.github.com/gcp/leela-zero
 */
public class Leelaz {
    private static final long MINUTE = 60 * 1000; // number of milliseconds in a minute
//    private static final long SECOND = 1000;
    private long maxAnalyzeTimeMillis;//, maxThinkingTimeMillis;

    private Process process;

    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;

    private boolean isReadingPonderOutput;
    private List<MoveData> bestMoves;
    private List<MoveData> bestMovesTemp;

    private boolean isPondering;
    private long startPonderTime;

    // genmove
    public boolean isThinking = false;

    /**
     * Initializes the leelaz process and starts reading output
     *
     * @throws IOException
     */
    public Leelaz() throws IOException {
        isReadingPonderOutput = false;
        bestMoves = new ArrayList<>();
        bestMovesTemp = new ArrayList<>();

        isPondering = false;
        startPonderTime = System.currentTimeMillis();


        // list of commands for the leelaz process
        List<String> commands = new ArrayList<>();
        commands.add("./leelaz"); // windows, linux, mac all understand this
        commands.add("-g");
        commands.add("-w" + Omega.NETWORK_STRING);
        commands.add("-b0");

        // run leelaz
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.directory(new File("."));
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();

        initializeStreams();

        // start a thread to continuously read Leelaz output
        new Thread(this::read).start();
    }

    /**
     * Initializes the input and output streams
     */
    private void initializeStreams() {
        inputStream = new BufferedInputStream(process.getInputStream());
        outputStream = new BufferedOutputStream(process.getOutputStream());
    }

    /**
     * Parse a line of Leelaz output
     *
     * @param line output line
     */
    private void parseLine(String line) {
        synchronized (this) {
            if (line.startsWith("~begin")) {
                if (System.currentTimeMillis() - startPonderTime > maxAnalyzeTimeMillis) {
                    // we have pondered for enough time. pause pondering
                    togglePonder();
                }

                isReadingPonderOutput = true;
                bestMovesTemp = new ArrayList<>();
            } else if (line.startsWith("~end")) {
                isReadingPonderOutput = false;
                bestMoves = bestMovesTemp;

                Omega.frame.repaint();
            } else {

                if (isReadingPonderOutput) {
                    line=line.trim();
                    // ignore passes, and only accept lines that start with a coordinate letter
                    if (Character.isLetter(line.charAt(0)) && !line.startsWith("pass"))
                        bestMovesTemp.add(new MoveData(line));
                } else {
                    System.out.print(line);

                    line = line.trim();
                    if (Omega.frame != null && line.startsWith("=")&&line.length() > 2 && isThinking) {
                        if (Omega.frame.isPlayingAgainstLeelaz) {
                            Omega.board.place(line.substring(2));
                        }
                        isThinking=false;
                    }
                }
            }
        }
    }

    /**
     * Continually reads and processes output from leelaz
     */
    private void read() {
        try {
            int c;
            StringBuilder line = new StringBuilder();
            while ((c = inputStream.read()) != -1) {
                line.append((char) c);
                if ((c == '\n')) {
                    parseLine(line.toString());
                    line = new StringBuilder();
                }
            }
            // this line will be reached when Leelaz shuts down
            System.out.println("Leelaz process ended.");

            shutdown();
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Sends a command for leelaz to execute
     *
     * @param command a GTP command containing no newline characters
     */
    public void sendCommand(String command) {
        System.out.println(command);
        if (command.startsWith("genmove"))
            isThinking = true;
        try {
            outputStream.write((command + "\n").getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param color color of stone to play
     * @param move  coordinate of the coordinate
     */
    public void playMove(Stone color, String move) {
        synchronized (this) {
            String colorString;
            switch (color) {
                case BLACK:
                    colorString = "B";
                    break;
                case WHITE:
                    colorString = "W";
                    break;
                default:
                    throw new IllegalArgumentException("The stone color must be BLACK or WHITE, but was " + color.toString());
            }

            sendCommand("play " + colorString + " " + move);
            bestMoves = new ArrayList<>();

            if (isPondering && !Omega.frame.isPlayingAgainstLeelaz)
                ponder();
        }
    }

    public void undo() {
        synchronized (this) {
            sendCommand("undo");
            bestMoves = new ArrayList<>();
            if (isPondering)
                ponder();
        }
    }

    /**
     * this initializes leelaz's pondering mode at its current position
     */
    private void ponder() {
        isPondering = true;
        startPonderTime = System.currentTimeMillis();
        sendCommand("time_left b 0 0");
    }

    public void togglePonder() {
        isPondering = !isPondering;
        if (isPondering) {
            ponder();
        } else {
            sendCommand("name"); // ends pondering
        }
    }

    /**
     * End the process
     */
    public void shutdown() {
        process.destroy();
    }

    public List<MoveData> getBestMoves() {
        synchronized (this) {
            return bestMoves;
        }
    }

    public boolean isPondering() {
        return isPondering;
    }
}
