package M2;
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
           T t = arr[i];
           if (t instanceof String){
            String x = t + " ";
           }
           else if (t instanceof Integer) {
            String x = t + "";
            int newVal = -1;
            try{
                newVal = Integer.parseInt(x);
            }
            catch (Exception e){

            }
            if (newVal < 0) {
                newVal = newVal * -1;
            }
            output[i]=newVal + "";
        }
        else if (t instanceof Integer) {
            int x = (int) t;
            output[i] = Math.abs(x);

        }else {
            double x =(double) t;
            output[i] = Math.abs(x);
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