/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.task.ImageUploadTask;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;

import javax.annotation.Nullable;

public class ImageUtils {

    public static ObjectIntPair<@SharedPtr ImageViewProxy> makePixmapView(
            RecordingContext context,
            Pixmap pixmap,
            boolean mipmapped,
            boolean budgeted,
            String label
    ) {
        Caps caps = context.getCaps();
        int ct = pixmap.getColorType();

        if (pixmap.getWidth() == 1 && pixmap.getHeight() == 1) {
            mipmapped = false;
        }

        var desc = caps.getDefaultColorImageDesc(
                Engine.ImageType.k2D,
                ct,
                pixmap.getWidth(),
                pixmap.getHeight(),
                1,
                ISurface.FLAG_SAMPLED_IMAGE | (mipmapped ? ISurface.FLAG_MIPMAPPED : 0)
        );
        if (desc == null) {
            return null;
            //TODO pixel conversion
            /*ct = ColorInfo.CT_RGBA_8888;
            desc = caps.getDefaultColorImageDesc(
                    Engine.ImageType.k2D,
                    ct,
                    pixmap.getWidth(),
                    pixmap.getHeight(),
                    1,
                    ISurface.FLAG_SAMPLED_IMAGE | (mipmapped ? ISurface.FLAG_MIPMAPPED : 0)
            );*/
        }
        assert desc != null;

        if (!pixmap.getInfo().isValid()) {
            return null;
        }

        short readSwizzle = caps.getReadSwizzle(desc, ct);
        if (ColorInfo.colorTypeIsAlphaOnly(ct)) {
            readSwizzle = Swizzle.concat(readSwizzle, Swizzle.AAAA);
        }

        @SharedPtr
        ImageViewProxy view = ImageViewProxy.make(
                context,
                desc,
                Engine.SurfaceOrigin.kUpperLeft,
                readSwizzle,
                budgeted,
                label
        );

        if (view == null) {
            return null;
        }

        var imageInfo = pixmap.getInfo();
        @SharedPtr
        var task = ImageUploadTask.make(
                context,
                view,   // move
                ct,
                imageInfo.alphaType(),
                imageInfo.colorSpace(),
                ct,
                imageInfo.alphaType(),
                imageInfo.colorSpace(), // src == dst: no conversion
                new ImageUploadTask.MipLevel[]{
                        new ImageUploadTask.MipLevel(pixmap)
                },
                new Rect2i(0, 0, pixmap.getWidth(), pixmap.getHeight()),
                ImageUploadTask.uploadOnce()
        );
        if (task == null) {
            context.getLogger().error("ImageUtils.makePixmapView: Could not create ImageUploadTask");
            return null;
        }

        context.addTask(task); // move

        view.ref();
        return ObjectIntPair.of(view, ct);
    }

    @Nullable
    @SharedPtr
    public static Image_Engine makeFromPixmap(
            RecordingContext context,
            Pixmap pixmap,
            boolean mipmapped,
            boolean budgeted,
            String label
    ) {
        var result = makePixmapView(context, pixmap, mipmapped, budgeted, label);
        if (result == null) {
            return null;
        }

        return new Image_Engine(
                context,
                result.first(),     // move
                result.rightInt(),  // new color type
                pixmap.getAlphaType(),
                pixmap.getColorSpace()
        );
    }
}
