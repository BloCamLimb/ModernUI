import java.util.Arrays;
import java.util.Scanner;

public class LevP1072 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int m = scanner.nextInt(), n = scanner.nextInt();
        int[] a = new int[m + n];
        for (int i = 0; i < m + n; i++)
            a[i] = scanner.nextInt();
        Arrays.sort(a);
        for (int i = 0; i < m + n; i++)
            System.out.print(a[i] + ((i < m + n - 1) ? " " : ""));
    }
}
