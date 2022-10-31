import java.util.Arrays;
import java.util.Scanner;

// shift array elements
public class LevP1070 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt(), k = scanner.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++)
            a[i] = scanner.nextInt();
        k %= n;
        if (k != 0) {
            int[] t = Arrays.copyOfRange(a, 0, k);
            System.arraycopy(a, k, a, 0, n - k);
            System.arraycopy(t, 0, a, n - k, k);
        }
        for (int i = 0; i < n; i++)
            System.out.println(a[i]);
    }
}
