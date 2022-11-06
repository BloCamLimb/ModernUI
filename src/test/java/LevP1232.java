import java.util.Scanner;

public class LevP1232 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] a = new int[1000];
        while (scanner.hasNext()) {
            int n = scanner.nextInt();
            for (int i = 0; i < n; i++)
                a[i] = scanner.nextInt();
            int ans = 0; // count bubble sort
            for (int i = 0; i < n - 1; i++)
                for (int j = 0; j < n - i - 1; j++)
                    if (a[j] > a[j + 1]) {
                        int temp = a[j];
                        a[j] = a[j + 1];
                        a[j + 1] = temp;
                        ans++;
                    }
            System.out.println(ans);
        }
    }
}
