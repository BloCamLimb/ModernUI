import java.util.*;

public class LevP1135 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Map<Integer, String> map = new HashMap<>();
        while (scanner.hasNext()) {
            int n = scanner.nextInt();
            scanner.nextLine();
            while (n-- > 0) {
                String s = scanner.nextLine();
                map.put(Integer.decode(s.substring(s.length() - 4)), s.substring(0, s.length() - 5));
            }
            map.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> System.out.println(e.getKey() + " " + e.getValue()));
            map.clear();
        }
    }
}
