import java.io.*;

public class LevP1284 {

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter pw = new PrintWriter(System.out);
        String s;
        while ((s = br.readLine()) != null) {
            pw.print('-');
            for (int i = 0, e = s.length(); i < e; i++) {
                int st = '9' - s.charAt(i);
                pw.print("-----.....----".substring(st, st + 5));
                pw.print('-');
            }
            pw.println();
        }
        pw.flush();
    }
}
