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

package icyllis.arc3d.test;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.ClipResult;
import icyllis.arc3d.engine.ClipStack;

public class TestClipStack {

    public static void main(String[] args) {
        var clipStack = new ClipStack(
                new Rect2i(0, 0, 800, 800),
                false
        );
        var viewMatrix = new Matrix();

        clipStack.clipRect(viewMatrix,
                20, 20, 60, 60,
                ClipOp.CLIP_OP_INTERSECT);

        clipStack.save();
        viewMatrix.preScale(0.5f, 0.5f);
        clipStack.clipRect(viewMatrix,
                40, 20, 70, 60,
                ClipOp.CLIP_OP_INTERSECT);
        System.out.println(stateToString(clipStack.currentClipState()));
        clipStack.elements().forEach(System.out::println);
        System.out.println();

        var clipResult = new ClipResult();
        clipResult.init(
                800, 800, 800, 800
        );
        int effect = clipStack.apply(null, false, clipResult,
                new Rect2f(20, 20, 30, 40));
        System.out.println(effect);
        System.out.println(clipResult.hasScissorClip() ? "HasScissorClip" : "NoScissorClip");
        System.out.println(clipResult.hasStencilClip() ? "HasStencilClip" : "NoStencilClip");
        System.out.printf("ScissorRect %d %d %d %d\n",
                clipResult.getScissorX0(), clipResult.getScissorY0(),
                clipResult.getScissorX1(), clipResult.getScissorY1()); // only scissor test
        System.out.println();

        clipStack.restore();
        System.out.println(stateToString(clipStack.currentClipState()));
        clipStack.elements().forEach(System.out::println);
    }

    public static String stateToString(int state) {
        return switch (state) {
            case ClipStack.STATE_EMPTY -> "Empty";
            case ClipStack.STATE_WIDE_OPEN -> "WideOpen";
            case ClipStack.STATE_DEVICE_RECT -> "DeviceRect";
            case ClipStack.STATE_COMPLEX -> "Complex";
            default -> throw new AssertionError(state);
        };
    }
}
