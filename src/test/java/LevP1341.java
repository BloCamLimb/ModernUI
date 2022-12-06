import java.io.PrintWriter;
import java.util.Scanner;
import java.util.regex.Pattern;

// evaluate 1+1=2
public class LevP1341 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        Pattern p = Pattern.compile("[+=]");
        while (sc.hasNext()) {
            int n = sc.nextInt();
            boolean result = true;
            while (n-- > 0) {
                String expr = sc.next();
                if (result) {
                    String[] codes = p.split(expr);
                    result = Integer.parseInt(codes[0]) + Integer.parseInt(codes[1]) == Integer.parseInt(codes[2]);
                }
            }
            pw.println(result ? "Accepted" : "Wrong Answer");
        }
        pw.flush();
    }
}
