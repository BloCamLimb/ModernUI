import java.io.*;
import java.util.StringTokenizer;

public class LevP1790 {

    static final int N = 100;

    public static void main(String[] args) {
        int[] parent = new int[N + 1], rank = new int[N + 1];
        int n = nextInt(), m = nextInt();
        for (int i = 1; i <= n; i++) {
            parent[i] = i;
            rank[i] = 1;
        }
        while (m-- > 0)
            union(parent, rank, nextInt(), nextInt());
        int comp = 0;
        for (int i = 1; comp < 2 && i <= n; i++)
            if (find(parent, i) == i) comp++;
        System.out.println(comp == 1 ? "Yes" : "No");
    }

    static int find(int[] parent, int i) {
        return parent[i] != i ? parent[i] = find(parent, parent[i]) : i;
    }

    static void union(int[] parent, int[] rank, int x, int y) {
        x = find(parent, x);
        y = find(parent, y);
        if (x == y) return; // connected
        int rx = rank[x], ry = rank[y];
        if (rx > ry) parent[y] = x;
        else {
            parent[x] = y;
            if (rx == ry) rank[y]++;
        }
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
