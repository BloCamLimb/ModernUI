import java.util.Scanner;

// a+-*/b, 1+2 -> 3
public class LevP1003 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        while (n-- > 0) {
            String input = scanner.next();
            int i;
            if ((i = input.indexOf('+')) != -1) {
                long a = Long.parseLong(input.substring(0, i));
                long b = Long.parseLong(input.substring(i + 1));
                System.out.println(a + b);
            } else if ((i = input.indexOf('-')) != -1) {
                long a = Long.parseLong(input.substring(0, i));
                long b = Long.parseLong(input.substring(i + 1));
                System.out.println(a - b);
            } else if ((i = input.indexOf('*')) != -1) {
                long a = Long.parseLong(input.substring(0, i));
                long b = Long.parseLong(input.substring(i + 1));
                System.out.println(a * b);
            } else if ((i = input.indexOf('/')) != -1) {
                long a = Long.parseLong(input.substring(0, i));
                long b = Long.parseLong(input.substring(i + 1));
                System.out.println(a / b);
            }
        }
    }
}
