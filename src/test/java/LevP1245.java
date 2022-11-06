import java.io.PrintWriter;
import java.util.Scanner;

// negative base
public class LevP1245 {

    public static final char[] CHAR_MAP = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'
    };

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        char[] buf = new char[17];
        while (sc.hasNext()) {
            int n = sc.nextInt(), base = sc.nextInt();
            pw.print(n);
            pw.print("=");
            int pos = 16;
            while (n != 0) {
                int x = n % base;
                n /= base;
                if (x < 0) {
                    x -= base;
                    n++;
                }
                buf[pos--] = CHAR_MAP[x];
            }
            pw.print(String.valueOf(buf, pos + 1, 16 - pos));
            pw.print("(base");
            pw.print(base);
            pw.println(")");
        }
        pw.flush();
    }
}
