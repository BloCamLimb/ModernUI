import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Scanner;

public class LevP1263 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        int n = sc.nextInt();
        while (n-- > 0)
            pw.println(new BigInteger(sc.next()).multiply(new BigInteger(sc.next())));
        pw.flush();
    }
}
