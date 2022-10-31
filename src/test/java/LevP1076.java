import java.util.Scanner;

public class LevP1076 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int t = scanner.nextInt();
        while (t-- > 0) {
            int a = scanner.nextInt(), b = scanner.nextInt();
            System.out.printf("%d %d\n", a / b, a % b);
        }
    }
}
