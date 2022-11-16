import java.util.Scanner;

public class LevP1266 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            int ans = 0;
            for (String s : scanner.next().split("\\+"))
                ans += Integer.parseInt(s);
            System.out.println(ans);
        }
    }
}
