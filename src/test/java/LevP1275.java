import java.util.Scanner;

public class LevP1275 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            int n = scanner.nextInt(), ans = 1;
            for (int i = 2; i < n; i++)
                if (gcd(i, n) == 1) ans++;
            System.out.println(ans);
        }
    }

    static int gcd(int a, int b) {
        if (a == 0) return b;
        if (b == 0) return a;
        int aTwos = Integer.numberOfTrailingZeros(a);
        a >>= aTwos;
        int bTwos = Integer.numberOfTrailingZeros(b);
        b >>= bTwos;
        while (a != b) {
            int delta = a - b;
            int minDeltaOrZero = delta & (delta >> (Integer.SIZE - 1));
            a = delta - minDeltaOrZero - minDeltaOrZero;
            b += minDeltaOrZero;
            a >>= Integer.numberOfTrailingZeros(a);
        }
        return a << Math.min(aTwos, bTwos);
    }
}
