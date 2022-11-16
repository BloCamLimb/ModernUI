import java.io.PrintWriter;
import java.util.Scanner;

// ****A*BC*DEF*G******** -> ****ABCDEFG
public class LevP1258 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        while (sc.hasNext()) {
            String s = sc.next();
            int i = 0, e = s.length();
            for (; i < e; i++)
                if (s.charAt(i) != '*')
                    break;
            pw.print(s.substring(0, i));
            for (int j = i; j < e; j++)
                if (s.charAt(j) != '*')
                    pw.print(s.charAt(j));
            pw.println();
        }
        pw.flush();
    }
}
