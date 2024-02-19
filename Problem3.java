import java.util.Arrays;
import java.lang.Math;

public class Problem3 {
    public static void main(String[] args) {
        Integer[] a1 = new Integer[]{-1, -2, -3, -4, -5, -6, -7, -8, -9, -10};
        Integer[] a2 = new Integer[]{-1, 1, -2, 2, 3, -3, -4, 5};
        Double[] a3 = new Double[]{-0.01, -0.0001, -0.15};
        String[] a4 = new String[]{"-1", "2", "-3", "4", "-5", "5", "-6", "6", "-7", "7"};

        bePositive(a1);
        bePositive(a2);
        bePositive(a3);
        bePositive(a4);
    }

    static <T> void bePositive(T[] arr) {
        System.out.println("Processing Array:" + Arrays.toString(arr));

        // pd438 2/5/2024 Create an array to store the positive values
        Object[] output = new Object[arr.length];

        for (int i = 0; i < arr.length; i++) {
            if (arr[i] instanceof Number) {
                if (arr[i] instanceof Integer) {
                    output[i] = Math.abs((Integer) arr[i]);
                } else if (arr[i] instanceof Double) {
                    output[i] = Math.abs((Double) arr[i]);
                }
            } else if (arr[i] instanceof String) {
                    output[i] = Math.abs(Integer.parseInt((String) arr[i]));
                    output[i] = arr[i];
            } else {
                output[i] = arr[i];
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Object i : output) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(String.format("%s (%s)", i, i.getClass().getSimpleName().substring(0, 1)));
        }
        System.out.println("Result: " + sb.toString());
    }
}