import java.util.Scanner;

public class LevP1234 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            double a = scanner.nextDouble(), b = scanner.nextDouble(), c = scanner.nextDouble(),
                    d = scanner.nextDouble(), e = scanner.nextDouble(), f = scanner.nextDouble();
            double x0 = (scanner.nextDouble() + scanner.nextDouble()) * 0.5, x1, y0, y1;
            do {
                y0 = ((((a * x0 + b) * x0 + c) * x0 + d) * x0 + e) * x0 + f;
                y1 = (((5 * a * x0 + 4 * b) * x0 + 3 * c) * x0 + 2 * d) * x0 + e;
                x1 = x0;
                x0 = x0 - y0 / y1;
            } while (Math.abs(x1 - x0) >= 1e-6);
            System.out.printf("%.4f\n", x1); // not exactly correct, should print x0 but wrong answer
        }
    }
}
