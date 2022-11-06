import java.util.Scanner;

public class CF1747C {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int t = scanner.nextInt();
        while (t-- > 0) {
            int n = scanner.nextInt(), first = scanner.nextInt();
            boolean b = false;
            for (int i = 1; i < n; i++)
                b |= scanner.nextInt() < first; // determine who gets 0 first
            System.out.println(b ? "Alice" : "Bob");
        }
    }
}
