import java.io.*;
import java.util.Scanner;

public class LevP1166 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        int c = sc.nextInt();
        while (c-- > 0) {
            long m = sc.nextLong(), n = sc.nextLong();
            pw.println(find(Math.min(m, n), Math.max(m, n)) ? "Stan wins" : "Ollie wins");
        }
        pw.flush();
    }

    static boolean find(long a, long b) {
        return a == b || (b >> 1) >= a || !find(b - a, a);
    }

    /*static class Scanner {

        final BufferedReader br;
        StringTokenizer st;

        Scanner(InputStream in) {
            br = new BufferedReader(new InputStreamReader(in));
        }

        String next() {
            while (st == null || !st.hasMoreElements()) {
                try {
                    st = new StringTokenizer(br.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return st.nextToken();
        }

        int nextInt() {
            return Integer.parseInt(next());
        }

        long nextLong() {
            return Long.parseLong(next());
        }
    }*/
}
