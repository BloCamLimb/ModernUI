import java.util.Scanner;

public class LevP1219 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++)
            a[i] = scanner.nextInt();
        System.out.println(n - lengthOfLIS(a, n));
    }

    // longest non-decreasing subsequence
    static int lengthOfLIS(int[] a, int n) {
        if (n <= 1) return n;
        int[] tail = new int[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            int v = a[i], idx = upperBound(tail, length, v);
            if (idx == length) tail[length++] = v;
            else tail[idx] = v;
        }
        return length;
    }

    static int upperBound(int[] a, int end, int key) {
        int low = 0, high = end - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] > key) high = mid - 1;
            else low = mid + 1;
        }
        return low;
    }
}
