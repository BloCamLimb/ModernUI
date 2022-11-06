import java.io.PrintWriter;
import java.util.Scanner;

public class LevP1252 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        while (sc.hasNext()) {
            int n = sc.nextInt();
            for (int i = n; i > 0; i--) {
                for (int j = n; j > 0; j--) {
                    pw.print(Math.min(i, j));
                    pw.print(" ");
                }
                pw.println();
            }
        }
        pw.flush();
    }
}
