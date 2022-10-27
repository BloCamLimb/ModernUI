import java.util.Arrays;
import java.util.Scanner;

public class CF1749C {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int t = scanner.nextInt();
        int[] elements = new int[128], temp = new int[128];
        while (t-- > 0) {
            int n = scanner.nextInt();
            for (int i = 0; i < n; i++) elements[i] = scanner.nextInt();
            System.out.println(find(elements, temp, n));
        }
    }

    public static int find(int[] elements, int[] temp, int n) {
        Arrays.sort(elements, 0, n);
        int low = 0, high = n;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            System.arraycopy(elements, 0, temp, 0, n);
            int k = 0, pos = n - 1;
            for (int i = 0; i <= mid; i++) {
                for (; pos >= 0; pos--) {
                    if (temp[pos] <= mid - i) {
                        k++;
                        temp[pos] = Integer.MAX_VALUE;
                        break;
                    }
                }
                if (pos <= i) break;
                temp[i] = Integer.MAX_VALUE;
            }
            if (k < mid) high = mid - 1;
            else low = mid + 1;
        }
        return high;
    }
}
