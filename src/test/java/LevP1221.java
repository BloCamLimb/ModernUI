import java.util.Scanner;

// disjoint set, union-find
public class LevP1221 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] root = new int[30000], rank = new int[30000];
        while (scanner.hasNext()) {
            int n = scanner.nextInt(), m = scanner.nextInt();
            if ((n | m) == 0) break;
            for (int i = 0; i < n; i++) {
                root[i] = i;
                rank[i] = 1;
            }
            while (m-- > 0) {
                int k = scanner.nextInt();
                int first = scanner.nextInt();
                for (int i = 1; i < k; i++)
                    union(root, rank, first, scanner.nextInt());
            }
            int ans = 1, victim = find(root, 0);
            for (int i = 1; i < n; i++)
                if (find(root, i) == victim) ans++;
            System.out.println(ans);
        }
    }

    static int find(int[] root, int i) {
        return root[i] != i ? root[i] = find(root, root[i]) : i;
    }

    static void union(int[] root, int[] rank, int x, int y) {
        int rootX = find(root, x), rootY = find(root, y);
        if (rootX == rootY) return; // connected
        int rankX = rank[rootX], rankY = rank[rootY];
        if (rankX > rankY) root[rootY] = rootX;
        else {
            root[rootX] = rootY;
            if (rankX == rankY) rank[rootY]++;
        }
    }
}
