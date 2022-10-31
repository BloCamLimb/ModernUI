import java.util.Scanner;

public class LevP1201 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int k = scanner.nextInt();
        CYCLE:
        while (k-- > 0) {
            int n = scanner.nextInt(), m = scanner.nextInt();
            for (int i = 1; i < m; i++)
                if (n % i == 0 && n % (m - i) == 0) {
                    System.out.println(i);
                    continue CYCLE;
                }
            System.out.println("-1");
        }
    }
}
