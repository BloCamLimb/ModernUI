import java.util.Scanner;

public class LevP1214 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            int n = scanner.nextInt();
            for (int i = 1; i < 10; i++) {
                if ((i << 1) > n) break;
                for (int j = 0; j < 10; j++) {
                    if (((i + j) << 1) > n) break;
                    for (int k = 0; k < 10; k++) {
                        int sum = ((i + j) << 1) + k;
                        if (sum > n) break;
                        if (sum == n) System.out.printf("%c%c%c%c%c\n", i + '0', j + '0', k + '0', j + '0', i + '0');
                    }
                }
            }
            for (int i = 1; i < 10; i++) {
                if ((i << 1) > n) break;
                for (int j = 0; j < 10; j++) {
                    if (((i + j) << 1) > n) break;
                    for (int k = 0; k < 10; k++) {
                        int sum = ((i + j + k) << 1);
                        if (sum > n) break;
                        if (sum == n) System.out.printf("%c%c%c%c%c%c\n", i + '0', j + '0', k + '0', k + '0', j + '0', i + '0');
                    }
                }
            }
            System.out.println();
        }
    }
}
