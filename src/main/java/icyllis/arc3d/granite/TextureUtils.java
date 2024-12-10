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
import org.jspecify.annotations.Nullable;

public class TextureUtils {

    @Nullable
    public static ObjectIntPair<@SharedPtr ImageViewProxy> makePixmapViewProxy(
            RecordingContext context,
            Pixmap pixmap,
            boolean mipmapped,
            boolean budgeted,
            String label
    ) {
        if (!pixmap.getInfo().isValid()) {
            return null;
        }
        Caps caps = context.getCaps();
        @ColorInfo.ColorType
        int srcCT = pixmap.getColorType();
        @ColorInfo.ColorType
        int dstCT = srcCT;

        if (pixmap.getWidth() <= 1 && pixmap.getHeight() <= 1) {
            mipmapped = false;
        }

        var desc = caps.getDefaultColorImageDesc(
                Engine.ImageType.k2D,
                dstCT,
                pixmap.getWidth(),
                pixmap.getHeight(),
                1,
                ISurface.FLAG_SAMPLED_IMAGE | (mipmapped ? ISurface.FLAG_MIPMAPPED : 0)
        );
        if (desc == null) {
            //TODO see ImageUploadTask and Caps
            return null;
            /*dstCT = ColorInfo.CT_RGBA_8888;
            desc = caps.getDefaultColorImageDesc(
                    Engine.ImageType.k2D,
                    dstCT,
                    pixmap.getWidth(),
                    pixmap.getHeight(),
                    1,
                    ISurface.FLAG_SAMPLED_IMAGE | (mipmapped ? ISurface.FLAG_MIPMAPPED : 0)
            );*/
        }
        assert desc != null;

        short readSwizzle = caps.getReadSwizzle(desc, dstCT);
        // If the color type is alpha-only, propagate the alpha value to the other channels.
        if (ColorInfo.colorTypeIsAlphaOnly(dstCT)) {
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
                srcCT,
                imageInfo.alphaType(),
                imageInfo.colorSpace(),
                dstCT,
                imageInfo.alphaType(),
                imageInfo.colorSpace(), // src == dst: no conversion
                new ImageUploadTask.MipLevel[]{
                        new ImageUploadTask.MipLevel(pixmap)
                },
                new Rect2i(0, 0, pixmap.getWidth(), pixmap.getHeight()),
                ImageUploadTask.uploadOnce()
        );
        if (task == null) {
            context.getLogger().error("ImageUtils.makePixmapViewProxy: Could not create ImageUploadTask");
            return null;
        }

        context.addTask(task); // move

        view.ref();
        return ObjectIntPair.of(view, dstCT);
    }

    @Nullable
    @SharedPtr
    public static GraniteImage makeFromPixmap(
            RecordingContext context,
            Pixmap pixmap,
            boolean mipmapped,
            boolean budgeted,
            String label
    ) {
        var result = makePixmapViewProxy(context, pixmap, mipmapped, budgeted, label);
        if (result == null) {
            return null;
        }

        return new GraniteImage(
                context,
                result.first(),     // move
                result.rightInt(),  // new color type
                pixmap.getAlphaType(),
                pixmap.getColorSpace()
        );
    }
}
