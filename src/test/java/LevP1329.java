import java.io.PrintWriter;
import java.util.*;

// date=13 && friday
public class LevP1329 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        Calendar cal = Calendar.getInstance(Locale.ROOT);
        cal.set(Calendar.DATE, 13);
        while (sc.hasNext()) {
            int year = sc.nextInt();
            cal.set(Calendar.YEAR, year);
            int count = 0;
            for (int i = 0; i < 12; i++) {
                cal.set(Calendar.MONTH, i);
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                    count++;
                }
            }
            pw.print(year);
            pw.print(' ');
            pw.println(count);
        }
        pw.flush();
    }
}
