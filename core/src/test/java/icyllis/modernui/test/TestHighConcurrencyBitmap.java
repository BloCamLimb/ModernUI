/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

import icyllis.modernui.graphics.Bitmap;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;

@Fork(2)
@Threads(16)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestHighConcurrencyBitmap {

    public static void main(String[] args) throws RunnerException {
        // you will get seg fault if there is no reachabilityFence() in the Bitmap class
        new Runner(new OptionsBuilder()
                .include(TestHighConcurrencyBitmap.class.getSimpleName())
                .shouldFailOnError(true).shouldDoGC(true)
                .jvmArgs("-Xmx128m", "-Dorg.lwjgl.system.allocator=system")
                .build())
                .run();
    }

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
