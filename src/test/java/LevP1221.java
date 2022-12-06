import java.util.Scanner;

// disjoint set, union-find
public class LevP1221 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] parent = new int[30000], rank = new int[30000];
        while (scanner.hasNext()) {
            int n = scanner.nextInt(), m = scanner.nextInt();
            if ((n | m) == 0) break;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                rank[i] = 1;
            }
            while (m-- > 0) {
                int k = scanner.nextInt();
                int first = scanner.nextInt();
                for (int i = 1; i < k; i++)
                    union(parent, rank, first, scanner.nextInt());
            }
            int ans = 1, victim = find(parent, 0);
            for (int i = 1; i < n; i++)
                if (find(parent, i) == victim) ans++;
            System.out.println(ans);
        }
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
}
