import java.io.*;
import java.math.BigInteger;
import java.util.StringTokenizer;

public class LevP1610 {

    public static void main(String[] args) {
        BigInteger mod = BigInteger.valueOf(10000);
        PrintWriter pw = new PrintWriter(System.out);
        int t = nextInt();
        while (t-- > 0) {
            int a = nextInt(), b = nextInt();
            pw.printf("%04d\n", BigInteger.valueOf(a).modPow(BigInteger.valueOf(b), mod));
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
