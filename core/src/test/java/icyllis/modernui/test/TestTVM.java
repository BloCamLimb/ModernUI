/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.test;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.PrintWriter;
import java.util.*;

public class TestTVM {

    public static final String input =
            "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";

    public static void main(String[] args) {
        final int[] g;
        {
            var h = new IntArrayList();
            a(h, 0, 43, 0);
            h.addElements(h.size(), new int[]{62, 0, 62, 0, 63});
            a(h, 51, 10, 1);
            a(h, 0, 8, 0);
            a(h, 0, 25, 1);
            h.addElements(h.size(), new int[]{0, 0, 0, 0, 63, 0});
            a(h, 25, 26, 1);
            g = h.toIntArray();
        }

        int[] ops = b(input, g);

        System.out.println(Arrays.toString(g));

        PrintWriter pw = new PrintWriter(System.out);
        for (int i = 0; i < ops.length; i++) {
            pw.printf("%d: %d\n", i, ops[i]);
        }
        try {
            process(ops, 2498, pw);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.flush();
    }

    public static void process(int[] ops, int pc, PrintWriter pw) {
        Object[] k = new Object[32];
        k[0] = "Global";
        k[1] = "Keywords";
        k[2] = "c";
        k[3] = "this";
        k[4] = "args";
        k[5] = "self";
        k[6] = "ops";
        Deque<Object> stack = new ArrayDeque<>();
        for (;;) {
            int op = ops[++pc];
            pw.printf("%d: %d --> \n", pc, op);
            switch (op) {
                case 9 -> {
                    int op0 = ops[++pc];
                    int op1 = ops[++pc];
                    k[op0] = "Array(" + op1 + ")";
                    pw.printf("Create Array k[%d] = %s\n", op0, k[op0]);
                }
                case 10 -> {
                    ++pc;
                    pw.println("Stack Push");
                }
                case 38 -> {
                    pw.println("Stack Pop");
                }
                case 26 -> {
                    pc += ops[++pc];
                }
                case 21 -> {
                    int op0 = ops[++pc];
                    int op1 = ops[++pc];
                    int op2 = ops[++pc];
                    int op3 = ops[++pc];
                    int op4 = ops[++pc];
                    int op5 = ops[++pc];
                    pw.printf("assignment k[%d] = %s.%s\n", op0, k[op1], op2);
                    pw.printf("assignment k[%d] = %s.%s\n", op3, k[op4], k[op5]);
                }
                case 32 -> {
                    int op0 = ops[++pc];
                    k[op0] = "";
                }
                case 30 -> {
                    int op0 = ops[++pc];
                    int op1 = ops[++pc];
                    int op2 = ops[++pc];
                    int op3 = ops[++pc];
                    int op4 = ops[++pc];
                    int op5 = ops[++pc];
                    k[op0] = (String) k[op0] + (char) op1;
                    pw.printf("k[%d] push char '%c'\n", op0, (char) op1);
                    k[op2] = (String) k[op2] + (char) op3;
                    pw.printf("k[%d] push char '%c'\n", op2, (char) op3);
                    k[op4] = (String) k[op4] + (char) op5;
                    pw.printf("k[%d] push char '%c'\n", op4, (char) op5);
                }
                case 35 -> {
                    int op0 = ops[++pc];
                    int op1 = ops[++pc];
                    int op2 = ops[++pc];
                    k[op0] = String.format("%s(%s)", k[op1], k[op2]);
                    pw.printf("function call k[%d] = %s(%s)\n", op0, k[op1], k[op2]);
                }
                case 47 -> {
                    int op0 = ops[++pc];
                    int op1 = ops[++pc];
                    int op2 = ops[++pc];
                    pw.printf("assignment %s.%s = %s\n", k[op0], op1, k[op2]);
                }
                case 27 -> {
                    int op0 = ops[++pc];
                    int op1 = ops[++pc];
                    k[op0] = k[op1] + "()";
                    pw.printf("function call k[%d] = %s()\n", op0, k[op1]);
                }
                case 36 -> {
                    int op0 = ops[++pc];
                    int op1 = ops[++pc];
                    k[op0] = (String) k[op0] + (char) op1;
                    pw.printf("k[%d] push char '%c'\n", op0, (char) op1);
                }
                case 17 -> {
                    int op0 = ops[++pc];
                    int op1 = ops[++pc];
                    int op2 = ops[++pc];
                    int op3 = ops[++pc];
                    k[op0] = (String) k[op0] + (char) op1;
                    pw.printf("k[%d] push char '%c'\n", op0, (char) op1);
                    k[op2] = (String) k[op2] + (char) op3;
                    pw.printf("k[%d] push char '%c'\n", op2, (char) op3);
                }
                case 4 -> {
                    int op0 = ops[++pc];
                    k[op0] = "Object";
                    pw.printf("Create Object k[%d]\n", op0);
                }
                case 34 -> {
                    int op0 = ops[++pc];
                    int op1 = ops[++pc];
                    int op2 = ops[++pc];
                    k[op0] = k[op1] + "[" + op2 + ']';
                    pw.printf("Assignment: k[%d] = %s[%s]\n", op0, k[op1], op2);
                }
                case 0 -> {
                    int op0 = ops[++pc];
                    int op1 = ops[++pc];
                    int op2 = ops[++pc];
                    int op3 = ops[++pc];
                    int op4 = ops[++pc];
                    int op5 = ops[++pc];
                    int op6 = ops[++pc];
                    pw.printf("""
                            Create Object k[%d]
                            Assignment: k[%d] = %s[%s]
                            Assignment: k[%d] = %s[%s]
                            """, op0, op1, k[op2], op3, op4, k[op5], op6);
                }
                case 6 -> {
                    pc += 9;
                }
                case 44 -> {
                    pc += 6;
                }
                case 11 -> {
                    pc += 3;
                }
                case 24 -> {
                    pc += 3;
                }
                case 14 -> {
                    pc += 3;
                }
                case 37 -> {
                    pc += 8;
                }
                default -> {
                    throw new IllegalStateException("Unhandled opcode " + op);
                }
            }
        }
    }

    public static void a(IntArrayList h, int off, int num, int inc) {
        for (int i = 0; i < num; i++) {
            h.add(off += inc);
        }
    }

    public static byte[] c(String s, int[] g) {
        var j = s.length();
        int b = 0, a, d = 0, e = 0;
        var h = new ByteArrayList();
        for (; e < j; e++) {
            a = g[s.charAt(e)];
            if (~a != 0) {
                b = (d % 4) != 0 ? 64 * b + a : a;
                if (d++ % 4 != 0) {
                    h.push((byte) (255 & b >> (-2 * d & 6)));
                }
            }
        }
        return h.toByteArray();
    }

    public static int d(int a) {
        return a >> 1 ^ -(1 & a);
    }

    public static int[] b(String s, int[] g0) {
        var f = new IntArrayList();
        byte[] g = c(s, g0);
        var j = g.length;
        var h = 0;
        while (j > h) {
            var a = g[h++];
            var e = 127 & a;
            if (a >= 0) {
                f.push(d(e));
                continue;
            }
            a = g[h++];
            e |= (127 & a) << 7;
            if (a >= 0) {
                f.push(d(e));
                continue;
            }
            a = g[h++];
            e |= (127 & a) << 14;
            if (a >= 0) {
                f.push(d(e));
                continue;
            }
            a = g[h++];
            e |= (127 & a) << 21;
            if (a >= 0) {
                f.push(d(e));
                continue;
            }
            a = g[h++];
            e |= a << 28;
            f.push(d(e));
        }
        return f.toIntArray();
    }

    public static int[] concat(int[] a, int[] b) {
        var c = new int[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
