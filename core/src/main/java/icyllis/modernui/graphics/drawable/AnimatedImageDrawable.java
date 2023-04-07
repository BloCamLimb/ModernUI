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

package icyllis.modernui.graphics.drawable;

import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.*;

/**
 * {@link Drawable} for drawing animated images (like GIF).
 *
 * <p>The framework handles decoding subsequent frames in another thread and
 * updating when necessary. The drawable will only animate while it is being
 * displayed.</p>
 */
public class AnimatedImageDrawable {

    private static final ExecutorService ANIMATED_IMAGE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Animated-Image-Thread");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY + 1);
        return t;
    });

    @ApiStatus.Internal
    public static Executor getAnimatedImageExecutor() {
        //TODO use CompletableFuture.supplyAsync() to decode next frame, and with a lock
        return ANIMATED_IMAGE_EXECUTOR;
    }
}
