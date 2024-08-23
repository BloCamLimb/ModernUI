/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test;

import icyllis.arc3d.core.*;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// deprecated Skia feature
public class Camera {

    public static void main(String[] args) {
        PrintWriter pw = new PrintWriter(System.out, true, StandardCharsets.UTF_8);

        Matrix4 mat = new Matrix4();
        mat.m34 = 1 / 576f;
        //mat.preTranslateZ(-20f);
        mat.preRotateY(MathUtil.PI_O_3);
        float[] p1 = new float[]{-25, -15};
        float[] p2 = new float[]{25, -15};
        float[] p3 = new float[]{25, 15};
        float[] p4 = new float[]{-25, 15};
        pw.println(mat);
        mat.mapPoint(p1);
        mat.mapPoint(p2);
        mat.mapPoint(p3);
        mat.mapPoint(p4);
        pw.println(Arrays.toString(p1));
        pw.println(Arrays.toString(p2));
        pw.println(Arrays.toString(p3));
        pw.println(Arrays.toString(p4));


        Camera3D camera3D = new Camera3D();
        Matrix4 transformMat = new Matrix4();
        transformMat.preRotateY(MathUtil.PI_O_3);
        Matrix3 outMatrix = new Matrix3();
        camera3D.getMatrix(transformMat, outMatrix, pw);
        pw.println("Orien: " + camera3D.mOrientation);
        pw.println(outMatrix);
        p1 = new float[]{-25, -15};
        p2 = new float[]{25, -15};
        p3 = new float[]{25, 15};
        p4 = new float[]{-25, 15};
        mapPoint(outMatrix, p1);
        mapPoint(outMatrix, p2);
        mapPoint(outMatrix, p3);
        mapPoint(outMatrix, p4);
        pw.println(Arrays.toString(p1));
        pw.println(Arrays.toString(p2));
        pw.println(Arrays.toString(p3));
        pw.println(Arrays.toString(p4));
    }

    public static void mapPoint(Matrix3 m, float[] p) {
        float x1 = m.m11 * p[0] + m.m21 * p[1] + m.m31;
        float y1 = m.m12 * p[0] + m.m22 * p[1] + m.m32;
        // project
        float w = 1.0f / (m.m13 * p[0] + m.m23 * p[1] + m.m33);
        x1 *= w;
        y1 *= w;
        p[0] = x1;
        p[1] = y1;
    }

    public static class Patch3D {

        public final Vector3 mU = new Vector3();
        public final Vector3 mV = new Vector3();
        public final Vector3 mOrigin = new Vector3();

        public Patch3D() {
            mU.x = 1;
            mV.y = -1;
        }

        public void transform(Matrix4 m) {
            transformVector(m, mU);
            transformVector(m, mV);
            transformPoint(m, mOrigin);
        }

        // w = 0
        public void transformVector(Matrix4 m, Vector3 v) {
            final float x = m.m11 * v.x + m.m21 * v.y + m.m31 * v.z;
            final float y = m.m12 * v.x + m.m22 * v.y + m.m32 * v.z;
            final float z = m.m13 * v.x + m.m23 * v.y + m.m33 * v.z;
            v.x = x;
            v.y = y;
            v.z = z;
        }

        // w = 1
        public void transformPoint(Matrix4 m, Vector3 v) {
            final float x = m.m11 * v.x + m.m21 * v.y + m.m31 * v.z + m.m41;
            final float y = m.m12 * v.x + m.m22 * v.y + m.m32 * v.z + m.m42;
            final float z = m.m13 * v.x + m.m23 * v.y + m.m33 * v.z + m.m43;
            v.x = x;
            v.y = y;
            v.z = z;
        }
    }

    public static class Camera3D {

        public final Vector3 mLocation = new Vector3();
        public final Vector3 mAxis = new Vector3();
        public final Vector3 mZenith = new Vector3();
        public final Vector3 mObserver = new Vector3();

        public final Matrix3 mOrientation = new Matrix3();

        public Camera3D() {
            mLocation.z = -576;
            mAxis.z = 1;
            mZenith.y = -1;
            mObserver.z = -576;

            update();
        }

        public void getMatrix(Matrix4 mat, Matrix3 matrix, PrintWriter pw) {
            Patch3D patch3D = new Patch3D();
            patch3D.transform(mat);

            pw.println("Patch U: " + patch3D.mU);
            pw.println("Patch V: " + patch3D.mV);

            Matrix3 o = mOrientation;

            Vector3 diff = patch3D.mOrigin.copy();
            diff.subtract(mLocation);
            float dot = diff.dot(o.m13, o.m23, o.m33);

            matrix.m11 = (patch3D.mU.dot(o.m11, o.m21, o.m31) / dot);
            matrix.m12 = (patch3D.mU.dot(o.m12, o.m22, o.m32) / dot);
            matrix.m13 = (patch3D.mU.dot(o.m13, o.m23, o.m33) / dot);

            matrix.m21 = (patch3D.mV.dot(o.m11, o.m21, o.m31) / dot);
            matrix.m22 = (patch3D.mV.dot(o.m12, o.m22, o.m32) / dot);
            matrix.m23 = (patch3D.mV.dot(o.m13, o.m23, o.m33) / dot);

            matrix.m31 = diff.dot(o.m11, o.m21, o.m31) / dot;
            matrix.m32 = diff.dot(o.m12, o.m22, o.m32) / dot;
            matrix.m33 = 1;
        }

        public void update() {
            Vector3 axis, zenith, cross;

            axis = mAxis;

            zenith = mZenith;

            cross = axis.copy();
            cross.cross(zenith);

            {
                Matrix3 o = mOrientation;
                float x = mObserver.x;
                float y = mObserver.y;
                float z = mObserver.z;

                o.m11 = (x * axis.x - z * cross.x);
                o.m21 = (x * axis.y - z * cross.y);
                o.m31 = (x * axis.z - z * cross.z);
                o.m12 = (y * axis.x - z * zenith.x);
                o.m22 = (y * axis.y - z * zenith.y);
                o.m32 = (y * axis.z - z * zenith.z);
                o.m13 = (axis.x);
                o.m23 = (axis.y);
                o.m33 = (axis.z);
            }
        }
    }
}
