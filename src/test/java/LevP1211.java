import java.util.Scanner;

public class LevP1211 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int t = scanner.nextInt();
        while (t-- > 0) {
            int n = scanner.nextInt();
            int min = Integer.MAX_VALUE, max = 0;
            while (n-- > 0) {
                int i = scanner.nextInt();
                min = Math.min(min, i);
                max = Math.max(max, i);
            }
            System.out.println((min + max) / 2);
        }
    }
}
