import java.util.Locale;
import java.util.Scanner;

public class LevP1207 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            int a = scanner.nextInt();
            long n = scanner.nextLong(a);
            int b = scanner.nextInt();
            System.out.println(Long.toString(n, b).toUpperCase(Locale.ROOT));
        }
    }
}
