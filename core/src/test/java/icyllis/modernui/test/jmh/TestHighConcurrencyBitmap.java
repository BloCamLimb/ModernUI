/*
 * Modern UI.
 * Copyright (C) 2024-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test.jmh;

import icyllis.modernui.graphics.Bitmap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

@Fork(value = 2, jvmArgsAppend = {"-Xmx128m", "-Dorg.lwjgl.system.allocator=system"})
@Threads(16)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestHighConcurrencyBitmap {
    // you will get seg fault if there is no reachabilityFence() in the Bitmap class

    private static final Bitmap SRC;

    static {
        Bitmap bm = Bitmap.createBitmap(32, 32, Bitmap.Format.RGBA_F32);
        Random r = new Random();
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                bm.setColor4f(i, j, new float[]{r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat()});
            }
        }
        SRC = bm;
    }

    @Benchmark
    public static void testRWPixel(Blackhole blackhole) {
        blackhole.consume(new Object());
        @SuppressWarnings("resource")
        var bm = Bitmap.createBitmap(32, 32, Bitmap.Format.RGBA_8888);
            bm.setPixels(SRC, 0, 0, 0, 0, 32, 32);
        blackhole.consume(new Object());
    }
}
