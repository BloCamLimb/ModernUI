import java.util.Scanner;

public class LevP1271 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            long ans = 0;
            for (String s : scanner.next().split("\\+"))
                ans += Long.parseLong(s);
            System.out.println(ans);
        }
    }
}
