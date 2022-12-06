import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Scanner;

// A-A∩B
public class LevP1330 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        BitSet A = new BitSet(1001), B = new BitSet(1001);
        while (sc.hasNext()) {
            A.clear();
            B.clear();
            for (int i = sc.nextInt(); i > 0; i--)
                A.set(sc.nextInt());
            for (int i = sc.nextInt(); i > 0; i--)
                B.set(sc.nextInt());
            B.and(A);
            A.andNot(B);
            for (int i = 0; i < 1001; i++)
                if (A.get(i)) {
                    pw.print(i);
                    pw.print(' ');
                }
            pw.println();
        }
        pw.flush();
    }
}
