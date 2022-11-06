import java.util.Arrays;
import java.util.Scanner;

// RE in OJ, use C++ version
public class LevP1256 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] a = new int[1000], b = new int[1000];
        while (scanner.hasNext()) {
            int m = scanner.nextInt(), n = scanner.nextInt(), c = scanner.nextInt();
            for (int i = 0; i < m; i++)
                a[i] = scanner.nextInt();
            for (int i = 0; i < n; i++)
                b[i] = scanner.nextInt();
            Arrays.sort(a, 0, m);
            Arrays.sort(b, 0, n);
            int i = 0, j = 0, ans = 0;
            while (i < m && j < n) {
                if (a[i] - b[j] > c) j++;
                else if (b[j] - a[i] > c) i++;
                else {
                    i++;
                    j++;
                    ans++;
                }
            }
            System.out.println(ans);
        }
    }
}
