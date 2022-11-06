import java.util.Scanner;

public class LevP1233 {

    public static final int N = 60;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[][] dp = new int[N + 1][N + 1];
        for (int i = 1; i <= N; i++)
            dp[i][1] = 1;
        while (scanner.hasNext()) {
            int n = scanner.nextInt();
            for (int i = 1; i <= n; i++)
                for (int j = 2; j <= i; j++)
                    dp[i][j] = dp[i - 1][j - 1] + dp[i - j][j];
            int ans = 0;
            for (int i = 2; i <= n; i++)
                ans += dp[n][i];
            System.out.println(ans);
        }
    }
}
