import java.util.Scanner;

// LevP1029
public class LuoguP1434 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[][] map = new int[105][105], dist = new int[105][105];
        for (int i = 0, r = scanner.nextInt(); i < r; i++)
            for (int j = 0, c = scanner.nextInt(); j < c; j++)
                map[i][j] = scanner.nextInt();
    }
}
