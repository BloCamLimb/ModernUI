import java.math.BigInteger;
import java.util.Scanner;

public class LevP1132 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BigInteger b10 = BigInteger.valueOf(10), b9 = BigInteger.valueOf(9);
        while (scanner.hasNext()) {
            int n = scanner.nextInt();
            System.out.println(b10.pow(n).subtract(b9.pow(n)));
        }
    }
}
