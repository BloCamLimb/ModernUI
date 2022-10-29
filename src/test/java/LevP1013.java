import java.util.Scanner;

// find max element
public class LevP1013 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int ans = 0, n = scanner.nextInt();
        while (n-- > 0)
            ans = Math.max(ans, scanner.nextInt());
        System.out.println(ans);
    }
}
