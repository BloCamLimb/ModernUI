import java.io.PrintWriter;
import java.util.Scanner;

public class LevP1304 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        while (sc.hasNext()) {
            long a = sc.nextLong(), b = sc.nextLong();
            pw.println(a / gcd(a, b) * b);
        }
        pw.flush();
    }

    static long gcd(long a, long b) {
        if (a == 0) return b;
        if (b == 0) return a;
        int aTwos = Long.numberOfTrailingZeros(a);
        a >>= aTwos;
        int bTwos = Long.numberOfTrailingZeros(b);
        b >>= bTwos;
        while (a != b) {
            long delta = a - b;
            long minDeltaOrZero = delta & (delta >> (Long.SIZE - 1));
            a = delta - minDeltaOrZero - minDeltaOrZero;
            b += minDeltaOrZero;
            a >>= Long.numberOfTrailingZeros(a);
        }
        return a << Math.min(aTwos, bTwos);
    }
}
