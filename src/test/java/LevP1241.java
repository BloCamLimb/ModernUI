import java.util.Scanner;

// ****A*BC*DEF*G****** -> ABCDEFG******
public class LevP1241 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String s = scanner.next();
            int i = s.length() - 1;
            for (; i >= 0; i--)
                if (s.charAt(i) != '*')
                    break;
            for (int j = 0; j < i; j++)
                if (s.charAt(j) != '*')
                    System.out.print(s.charAt(j));
            System.out.println(s.substring(i));
        }
    }
}
