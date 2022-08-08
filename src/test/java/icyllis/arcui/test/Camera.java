/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.test;

import icyllis.arcui.core.*;

import java.io.PrintWriter;

// deprecated Skia feature
public class Camera {

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
