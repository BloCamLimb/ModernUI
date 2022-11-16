import java.util.Scanner;

public class LevP1288 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            int n = scanner.nextInt(), c = 0;
            for (int i = 0; i < n; i++) if (gcd(n, scanner.nextInt()) == 1) c++;
            System.out.println(c);
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
