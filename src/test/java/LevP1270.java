import java.util.Scanner;

public class LevP1270 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int prev = scanner.nextInt();
        int ans = 1;
        while (n-- > 1) {
            int curr = scanner.nextInt();
            if (curr > prev) {
                prev = curr;
                ans++;
            }
        }
        System.out.println(ans);
    }
}
