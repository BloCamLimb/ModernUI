import java.util.Scanner;

public class LevP1219 {

    //TODO
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt(), top = 0, kick = 0;
        int[] a = new int[1001], b = new int[1001];
        for (int i = 0; i < n; i++)
            a[i] = scanner.nextInt();
        a[n] = Integer.MAX_VALUE;
        b[0] = n;
        for (int i = n - 1; i >= 0; i--) {
            while (top >= 0 && a[b[top]] < a[i]) top--;
            kick += b[top] - i - 1;
            b[++top] = i;
        }
        System.out.println(kick);
    }
}
