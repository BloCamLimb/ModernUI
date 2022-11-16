import java.io.*;
import java.util.HashSet;
import java.util.StringTokenizer;

// RE in OJ
public class LevP1165 {

    public static void main(String[] args) {
        var sc = new Scanner(System.in);
        int n = sc.nextInt();
        var set = new HashSet<Snowflake>(n, 1.0f);
        var res = false;
        while (n-- > 0) {
            int m1 = sc.nextInt(), m2 = sc.nextInt(), m3 = sc.nextInt(),
                    m4 = sc.nextInt(), m5 = sc.nextInt(), m6 = sc.nextInt();
            res = res || !set.add(new Snowflake(m1, m2, m3, m4, m5, m6));
        }
        System.out.println(res ? "Twin snowflakes found." : "No two snowflakes are alike.");
    }

    record Snowflake(int m1, int m2, int m3, int m4, int m5, int m6) {

        int get(int i) {
            return switch (i) {
                case 0 -> m1;
                case 1 -> m2;
                case 2 -> m3;
                case 3 -> m4;
                case 4 -> m5;
                default -> m6;
            };
        }

        @Override
        public int hashCode() {
            return m1 + m2 + m3 + m4 + m5 + m6;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Snowflake s)
                for (int i = 0; i < 6; i++) {
                    int j;
                    for (j = 0; j < 6; j++)
                        if (get(j) != s.get((i + j) % 6))
                            break;
                    if (j == 6) return true;
                    for (j = 0; j < 6; j++)
                        if (get(j) != s.get((i + 6 - j) % 6))
                            break;
                    if (j == 6) return true;
                }
            return false;
        }
    }

    static class Scanner {

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
    }
}
