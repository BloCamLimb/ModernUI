import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

// ****A*BC*DEF*G****** -> ABCDEFG******
public class LevP1241 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(new BufferedOutputStream(System.out));
        while (sc.hasNext()) {
            String s = sc.next();
            int i = s.length() - 1;
            for (; i >= 0; i--)
                if (s.charAt(i) != '*')
                    break;
            for (int j = 0; j < i; j++)
                if (s.charAt(j) != '*')
                    pw.print(s.charAt(j));
            pw.println(s.substring(i));
        }
        pw.flush();
    }
}
