package featurecat.omega.analysis;

import featurecat.omega.Omega;
import featurecat.omega.rules.Stone;
import featurecat.omega.ui.BoardRenderer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * an interface with leelaz.exe go engine. Can be adapted for GTP, but is specifically designed for GCP's Leela Zero.
 * leelaz is modified to output information as it ponders
 * see www.github.com/gcp/leela-zero
 */
public class Leelaz {
    public LeelazData heatmap = null;

    private Process process;

    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;

    private boolean isParsingHeatmap = false;
    private List<String> heatmapStrings;
    private Consumer<LeelazData> heatmapPromise;

    private boolean isLoaded = false;

    /**
     * Initializes the leelaz process and starts reading output
     *
     * @throws IOException
     */
    public Leelaz() throws IOException {
        // list of commands for the leelaz process
        List<String> commands = new ArrayList<>();
        commands.add("./leelaz"); // windows, linux, mac all understand this
        commands.add("-g");
        commands.add("-w" + Omega.NETWORK_STRING);
        commands.add("--noponder");

        // run leelaz
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.directory(new File("."));
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();

        initializeStreams();

        // start a thread to continuously read Leelaz output
        new Thread(this::read).start();

        sendCommand("name");
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
            {
                if (!isLoaded && line.startsWith("=")) {
                    isLoaded = true;
                    return;
                }
//                System.out.print(line);
                if (isLoaded) {
                    if (isParsingHeatmap) {
                        if (line.startsWith("=")) {
                            if (heatmapStrings.size() >= 21) {
                                isParsingHeatmap = false;
                                heatmapPromise.accept(new LeelazData(heatmapStrings));
                            } // otherwise it must be a different command. wow we need a more robust system but this should work.
                        } else if (!(line = line.trim()).isEmpty() && Character.isDigit(line.charAt(0)) || line.matches("(pass: |winrate: ).+")) {
                            heatmapStrings.add(line);
                        }
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
    private void sendCommand(String command) {
        try {
            outputStream.write((command + "\n").getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param heatmapPromise this function will be called when the heatmap data is ready.
     */
    public void heatmap(Consumer<LeelazData> heatmapPromise) {
        if (isParsingHeatmap) {
            System.out.println("Leelaz is already parsing a heatmap. Abort");
            return;
        }
        isParsingHeatmap = true;
        heatmapStrings = new ArrayList<>();
        this.heatmapPromise = heatmapPromise;
        sendCommand("heatmap");
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
                    return; // Do nothing if the stone color is empty TODO this probably means deleting stone, so we need to figure out what to do here.
            }

            sendCommand("play " + colorString + " " + move);
            refreshHeatmap();
        }
    }

    private void refreshHeatmap() {
        this.heatmap = null;
        heatmap((LeelazData data) -> {
            this.heatmap = data;
            Omega.frame.repaint();
        });
    }

    public void undo() {
        synchronized (this) {
            sendCommand("undo");
            refreshHeatmap();
        }
    }

    /**
     * End the process
     */
    public void shutdown() {
        process.destroy();
    }
}
