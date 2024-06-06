/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine.task;

import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.core.Rect2ic;
import icyllis.arc3d.engine.*;

import javax.annotation.Nullable;

public class ImageUploadTask extends Task {

    public interface UploadCondition {

        boolean PRESERVE = true;
        boolean DISCARD = false;

        boolean shouldUpload(ImmediateContext context);

        default boolean onUploadSubmitted() {
            return PRESERVE;
        }
    }

    public static class OneTimeUploadCondition implements UploadCondition {

        public static final UploadCondition INSTANCE = new OneTimeUploadCondition();

        @Override
        public boolean shouldUpload(ImmediateContext context) {
            return true;
        }

        @Override
        public boolean onUploadSubmitted() {
            return DISCARD;
        }
    }

    @Nullable
    public static ImageUploadTask make(RecordingContext context,
                                       ImageViewProxy imageViewProxy,
                                       int srcColorType,
                                       int srcAlphaType,
                                       ColorSpace srcColorSpace,
                                       int dstColorType,
                                       int dstAlphaType,
                                       ColorSpace dstColorSpace,
                                       Rect2ic dstRect,
                                       UploadCondition condition) {

        if (dstRect.isEmpty()) {
            return null;
        }








        return null;
    }

    @Override
    public int prepare(ResourceProvider resourceProvider) {
        return RESULT_FAILURE;
    }

    @Override
    public int execute(ImmediateContext context, CommandBuffer cmdBuffer) {
        return RESULT_FAILURE;
    }
}
