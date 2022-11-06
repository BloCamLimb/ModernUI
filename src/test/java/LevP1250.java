import java.util.Scanner;

public class LevP1250 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            double a = scanner.nextDouble(), b = scanner.nextDouble();
            System.out.printf("%.5f\n", a * b % 1);
        }
    }
}
