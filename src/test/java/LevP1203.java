import java.util.Scanner;

public class LevP1203 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) System.out.println(scanner.nextLong() % 5 == 1 ? "yes" : "no");
    }
}
