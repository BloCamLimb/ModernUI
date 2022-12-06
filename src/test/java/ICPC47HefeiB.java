import java.io.PrintWriter;
import java.util.Random;

/**
 * You're right, but "Genshin Impact" is a new open-world adventure game developed independently by Mihuyo. The game
 * takes place in a fantasy world called "Teyvat", where those chosen by the gods will be granted the "Vision" to
 * channel the power of the elements. You will play a mysterious character named "traveler", in the freedom of travel
 * to meet different personalities, unique ability of companions, and together to defeat strong enemies, to find lost
 * relatives. At the same time, gradually discover the truth of the "Genshin Impact".
 * <p>
 * Z will control Amber to adventure in the game. Amber has a spell that can be cast every y seconds which has a 1/p
 * probability to make the target burn for x seconds (if the target is already burning, the burning duration will be
 * reset to x seconds). Now Z wants to know the expected value of how much of the total duration is covered by the
 * burning duration if he keeps casting the spell continuously on an enemy.
 */
public class ICPC47HefeiB {

    public static void main(String[] args) {
        PrintWriter pw = new PrintWriter(System.out);
        pw.println("BruteForce vs Analytical");
        double bruteForce = bruteForce(19, 5, 6, 1_000_000_000);
        double analytical = solve(19, 5, 6);
        pw.println(bruteForce);
        pw.println(analytical);
        pw.println(Math.abs(analytical - bruteForce));
        pw.flush();
    }

    public static double bruteForce(int x, int y, int p, int limit) {
        Random rand = new Random();
        int cur = 0, sum = 0;
        for (int i = 0, e = limit / y; i <= e; i++) {
            int diff = Math.min(y, cur);
            sum += diff;
            if (rand.nextInt(p) == 0) {
                cur = x;
            } else {
                cur -= diff;
            }
        }
        return (double) sum / limit;
    }

    public static double solve(int x, int y, int p) {
        if (x <= y) {
            return (double) x / y / p;
        }
        int div = x / y, rem = x % y;
        double p1 = 1 - 1. / p;
        return (rem * (1 - Math.pow(p1, div + 1)) + (y - rem) * (1 - Math.pow(p1, div))) / y;
    }
}
