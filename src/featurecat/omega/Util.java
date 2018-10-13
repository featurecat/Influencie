package featurecat.omega;

public class Util {
    public static String toStringFlatTo2DArray(double[] array, int rows) {
        StringBuilder sb = new StringBuilder();
        int rowCounter = 0;
        for (double d : array) {
            sb.append(String.format("%4.1f ", d*100));
            if (++rowCounter >= rows) {
                rowCounter = 0;
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
