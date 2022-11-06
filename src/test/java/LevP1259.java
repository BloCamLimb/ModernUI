import java.util.Scanner;

// 39 -> 3+9=12 -> 1+2=3
public class LevP1259 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        long n;
        while ((n = scanner.nextLong()) != 0) find(n);
    }

    static void find(long n) {
        int res = 0;
        do {
            res += n % 10;
            n /= 10;
        } while (n != 0);
        if (res < 10) System.out.println(res);
        else find(res);
    }
}
