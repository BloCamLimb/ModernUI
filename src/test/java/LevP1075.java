import java.util.Scanner;

public class LevP1075 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] a = new int[7];
        for (int i = 0; i < 7; i++)
            a[i] = scanner.nextInt() + scanner.nextInt();
        int ans = 0, max = 0;
        for (int i = 0; i < 7; i++)
            if (a[i] > 8 && a[i] > max) {
                ans = i + 1;
                max = a[i];
            }
        System.out.println(ans);
    }
}
