import java.util.Scanner;

public class LevP1212 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            int a = scanner.nextInt(), b = scanner.nextInt(), n = scanner.nextInt();
            System.out.println((int) Math.pow(a, b) % n);
        }
    }
}
