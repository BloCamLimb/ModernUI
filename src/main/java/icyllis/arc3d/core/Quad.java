/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

//TODO
public class Quad {

    public static final int kAxisAligned = 0;
    public static final int kRectilinear = 1;
    public static final int kGeneral = 2;
    public static final int kPerspective = 3;

    private float x0;
    private float x1;
    private float x2;
    private float x3;
    private float y0;
    private float y1;
    private float y2;
    private float y3;
    private float w0;
    private float w1;
    private float w2;
    private float w3;

    private int type;

    public Quad(Rect2fc rect) {
        x0 = x1 = rect.left();
        y0 = y2 = rect.top();
        x2 = x3 = rect.right();
        y1 = y3 = rect.bottom();
        w0 = w1 = w2 = w3 = 1.f;
        type = kAxisAligned;
    }

    public Quad(Rect2fc rect, Matrixc m) {
        int mask = m.getType();
        if (mask <= (Matrix.kScale_Mask | Matrix.kTranslate_Mask)) {
            float r0 = rect.left();
            float r1 = rect.top();
            float r2 = rect.right();
            float r3 = rect.bottom();
            if (mask > Matrix.kIdentity_Mask) {
                if (mask > Matrix.kTranslate_Mask) {
                    r0 *= m.m11();
                    r1 *= m.m22();
                    r2 *= m.m11();
                    r3 *= m.m22();
                }
                r0 += m.m41();
                r1 += m.m42();
                r2 += m.m41();
                r3 += m.m42();
            }
            x0 = x1 = r0;
            y0 = y2 = r1;
            x2 = x3 = r2;
            y1 = y3 = r3;
            w0 = w1 = w2 = w3 = 1.f;
            type = kAxisAligned;
        } else {
            float rx0, rx1, rx2, rx3;
            float ry0, ry1, ry2, ry3;
            rx0 = rx1 = rect.left();
            ry0 = ry2 = rect.top();
            rx2 = rx3 = rect.right();
            ry1 = ry3 = rect.bottom();
            x0 = m.m11() * rx0 + (m.m21() * ry0 + m.m41());
            x1 = m.m11() * rx1 + (m.m21() * ry1 + m.m41());
            x2 = m.m11() * rx2 + (m.m21() * ry2 + m.m41());
            x3 = m.m11() * rx3 + (m.m21() * ry3 + m.m41());
            y0 = m.m22() * rx0 + (m.m12() * ry0 + m.m42());
            y1 = m.m22() * rx1 + (m.m12() * ry1 + m.m42());
            y2 = m.m22() * rx2 + (m.m12() * ry2 + m.m42());
            y3 = m.m22() * rx3 + (m.m12() * ry3 + m.m42());
            if (m.hasPerspective()) {
                w0 = m.m14() * rx0 + (m.m24() * ry0 + m.m44());
                w1 = m.m14() * rx1 + (m.m24() * ry1 + m.m44());
                w2 = m.m14() * rx2 + (m.m24() * ry2 + m.m44());
                w3 = m.m14() * rx3 + (m.m24() * ry3 + m.m44());
            } else {
                w0 = w1 = w2 = w3 = 1.f;
            }
            if (m.isAxisAligned()) {
                type = kAxisAligned;
            } else if (m.preservesRightAngles()) {
                type = kRectilinear;
            } else if (m.hasPerspective()) {
                type = kPerspective;
            } else {
                type = kGeneral;
            }
        }
    }

    public float x0() {
        return x0;
    }

    public float x1() {
        return x1;
    }

    public float x2() {
        return x2;
    }

    public float x3() {
        return x3;
    }

    public float y0() {
        return y0;
    }

    public float y1() {
        return y1;
    }

    public float y2() {
        return y2;
    }

    public float y3() {
        return y3;
    }

    public float w0() {
        return w0;
    }

    public float w1() {
        return w1;
    }

    public float w2() {
        return w2;
    }

    public float w3() {
        return w3;
    }

    public float x(int i) {
        return switch (i) {
            case 0 -> x0;
            case 1 -> x1;
            case 2 -> x2;
            case 3 -> x3;
            default -> throw new IndexOutOfBoundsException(i);
        };
    }

    public float y(int i) {
        return switch (i) {
            case 0 -> y0;
            case 1 -> y1;
            case 2 -> y2;
            case 3 -> y3;
            default -> throw new IndexOutOfBoundsException(i);
        };
    }

    public float w(int i) {
        return switch (i) {
            case 0 -> w0;
            case 1 -> w1;
            case 2 -> w2;
            case 3 -> w3;
            default -> throw new IndexOutOfBoundsException(i);
        };
    }

    public void point(int i, float[] p) {
        if (type == kPerspective) {
            p[0] = x(i) / w(i);
            p[1] = y(i) / w(i);
        } else {
            p[0] = x(i);
            p[1] = y(i);
        }
    }
}
