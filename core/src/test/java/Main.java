import icyllis.modernui.akashi.opengl.GLCore;
import icyllis.modernui.graphics.MathUtil;

import java.io.PrintWriter;
import java.util.Scanner;

// this class does nothing
public class Main {

    public static void main(String[] args) {
        System.out.println(MathUtil.nextLog2(Float.NEGATIVE_INFINITY));
        System.out.println(MathUtil.nextLog2(0));
        System.out.println(MathUtil.nextLog2(1));
        System.out.println(MathUtil.nextLog2(2));
        System.out.println(MathUtil.nextLog2(3));
        System.out.println(MathUtil.nextLog2(7));
        System.out.println(MathUtil.nextLog2(8));
        System.out.println(MathUtil.nextLog2(Float.POSITIVE_INFINITY));
        System.out.println(MathUtil.nextLog2(Float.NaN));
    }
}
