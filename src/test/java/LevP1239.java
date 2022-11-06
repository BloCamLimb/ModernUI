import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

public class LevP1239 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(new BufferedOutputStream(System.out));
        while (sc.hasNext())
            pw.println(gcd(sc.nextInt(), sc.nextInt()));
        pw.flush();
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
