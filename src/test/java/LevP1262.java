import java.io.PrintWriter;
import java.util.Scanner;

public class LevP1262 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        int n = sc.nextInt();
        while (n-- > 0) {
            String s = sc.next();
            String ns = s.substring(2) + s.charAt(1) + s.charAt(0);
            if (s.equals(ns)) {
                pw.println("NO");
            } else {
                pw.println(ns);
            }
        }
        pw.flush();
    }
}
