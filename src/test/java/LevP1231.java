import java.util.Arrays;
import java.util.Scanner;

public class LevP1231 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] counts = new int[26];
        while (scanner.hasNext()) {
            String s = scanner.next();
            Arrays.fill(counts, 0);
            for (int i = 0, len = s.length(); i < len; i++)
                counts[s.charAt(i) - 'a']++;
            for (int i = 0; i < 26; i++)
                if (counts[i] == 1) System.out.print((char) ('a' + i));
            System.out.println();
        }
    }
}
