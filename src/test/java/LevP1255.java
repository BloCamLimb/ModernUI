import java.io.*;
import java.util.StringTokenizer;

public class LevP1255 {

    public static void main(String[] args) {
        PrintWriter pw = new PrintWriter(System.out);
        int t = nextInt();
        while (t-- > 0) {
            int n = nextInt(), v = nextInt();
            // Kadane’s Algorithm
            int cur = v, max = v, p = 0, st = 0, en = 0;
            for (int i = 1; i < n; i++) {
                v = nextInt();
                if (cur < 0) {
                    cur = v;
                    p = i;
                } else {
                    cur += v;
                }
                if (cur > max) {
                    max = cur;
                    st = p;
                    en = i;
                }
            }
            pw.print(max);pw.print(' ');pw.print(st + 1);pw.print(' ');pw.print(en + 1);pw.println();
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
