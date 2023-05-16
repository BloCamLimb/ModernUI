import icyllis.modernui.graphics.MathUtil;

// this class does nothing
public class Main {

    public static void main(String[] args) {
        System.out.println(MathUtil.ceilLog2(Float.NEGATIVE_INFINITY));
        System.out.println(MathUtil.ceilLog2(0));
        System.out.println(MathUtil.ceilLog2(1));
        System.out.println(MathUtil.ceilLog2(2));
        System.out.println(MathUtil.ceilLog2(3));
        System.out.println(MathUtil.ceilLog2(7));
        System.out.println(MathUtil.ceilLog2(8));
        System.out.println(MathUtil.ceilLog2(Float.POSITIVE_INFINITY));
        System.out.println(MathUtil.ceilLog2(Float.NaN));
    }
}
