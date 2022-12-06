import java.io.*;
import java.util.Arrays;
import java.util.StringTokenizer;

public class LevP1328 {

    public static void main(String[] args) {
        PrintWriter pw = new PrintWriter(System.out);
        int[] a = new int[20];
        int n = nextInt();
        while (n-- > 0) {
            int sum = nextInt() + nextInt() + nextInt();
            int v, m = 0;
            while ((v = nextInt()) != -1)
                a[m++] = -v;
            Arrays.sort(a, 0, m);
            pw.println(sum - a[0] - a[1] - a[2]);
        }
        pw.flush();
    }

    static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    static StringTokenizer st;

    static String next() {
        while (st == null || !st.hasMoreElements()) {
            try {
                st = new StringTokenizer(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return st.nextToken();
    }

    static int nextInt() {
        return Integer.parseInt(next());
    }
}
