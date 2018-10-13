package featurecat.omega.analysis;

import featurecat.omega.Util;

import java.util.List;

public class LeelazData {
    double[] moveProbabilities = new double[19 * 19];
    double passProbability;
    double winrate;

    public LeelazData(List<String> lines) {
        // there will be 19 lines of 19 numbers,
        // 1 line of pass number
        // 1 line of winrate

        int[] moveWeights = new int[19 * 19];
        int passWeight;

        int total = 0;

        for (int i = 0; i < 19; i++) {
            // 18 - i is because my coordinate system and leelaz's coordinate system are flipped.
            String[] weights = lines.get(18 - i).split(" +");
            for (int j = 0; j < 19; j++) {
                total += moveWeights[i * 19 + j] = Integer.parseInt(weights[j]);
            }
        }
        total += passWeight = Integer.parseInt(lines.get(19).substring(6));
        winrate = Double.parseDouble(lines.get(20).substring(9));

        for (int i = 0; i < moveWeights.length; i++) {
            moveProbabilities[i] = 1.0 * moveWeights[i] / total;
        }

        passProbability = 1.0 * passWeight / total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        sb.append(Util.toStringFlatTo2DArray(moveProbabilities, 19));
        sb.append("Pass: " + passProbability + "\n");
        sb.append("Winrate: " + winrate + "\n");
        sb.append("]" + "\n");

        return sb.toString();
    }
}
