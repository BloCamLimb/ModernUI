import java.io.PrintWriter;
import java.util.Scanner;

public class LevP1281 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        while (sc.hasNext()) {
            int a = sc.nextInt(), b = sc.nextInt();
            if ((a >> 1) >= b && (a & 1) == 0) {
                pw.println("Yes");
            } else {
                pw.println("No");
            }
        }
        pw.flush();
    }
}
