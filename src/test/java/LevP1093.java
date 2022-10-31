import java.util.Scanner;

public class LevP1093 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            int t = scanner.nextInt();
            while (t-- > 0) {
                String op = scanner.next();
                int a = scanner.nextInt(), b = scanner.nextInt();
                switch (op) {
                    case "+" : System.out.println(a + b); break;
                    case "-" : System.out.println(a - b); break;
                    case "*" : System.out.println(a * b); break;
                    case "/" : {
                        if (a % b == 0) System.out.println(a / b);
                        else System.out.printf("%.2f\n", (float) a / b);
                    }
                }
            }
        }
    }
}
