import java.util.Arrays;
import java.util.Scanner;

public class LevP1267 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] a = new int[3];
        while (scanner.hasNext()) {
            for (int i = 0; i < 3; i++)
                a[i] = scanner.nextInt();
            Arrays.sort(a);
            if (a[0] + a[1] > a[2]) {
                if (a[0] == a[1] && a[1] == a[2]) {
                    System.out.println("regular triangle");
                } else if (a[0] == a[1] || a[1] == a[2]) {
                    System.out.println("isosceles triangle");
                } else if (a[0] * a[0] + a[1] * a[1] == a[2] * a[2]) {
                    System.out.println("right triangle");
                } else {
                    System.out.println("triangle");
                }
            } else {
                System.out.println("not a triangle");
            }
        }
    }
}
