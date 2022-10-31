import java.util.Scanner;

public class LevP1049 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            int a = scanner.nextInt();
            double x = scanner.nextDouble(), y = scanner.nextDouble();
            System.out.printf("%.6f\n", x + a % 3 * (int) (x + y) % 2 / 4);
        }
    }
}
