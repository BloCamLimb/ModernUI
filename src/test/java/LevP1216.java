import java.util.Locale;
import java.util.Scanner;

public class LevP1216 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        while (n-- > 0) System.out.println(Integer.toString(scanner.nextInt(), 17).toUpperCase(Locale.ROOT));
    }
}
