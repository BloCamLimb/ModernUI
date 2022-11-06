import java.util.Scanner;

public class LevP1236 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] dp = new int[1001];
        while (scanner.hasNext()) {
            int m = scanner.nextInt(), n = scanner.nextInt();
            for (int i = 1, e = Math.max(m, n); i <= e; i++)
                dp[i] = 0;
            while (m-- > 0) for (int i = 1; i <= n; i++)
                dp[i] = Math.max(dp[i], dp[i - 1]) + scanner.nextInt();
            System.out.println(dp[n]);
        }
    }
}
