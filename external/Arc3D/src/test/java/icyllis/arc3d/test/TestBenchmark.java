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
import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.sketch.Paint;
import icyllis.arc3d.sketch.Path;
import icyllis.arc3d.sketch.StrokeRec;
import icyllis.arc3d.sketch.j2d.J2DUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.awt.BasicStroke;

@Fork(1)
@Threads(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestBenchmark {

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(TestBenchmark.class.getSimpleName())
                .shouldFailOnError(true).shouldDoGC(true)
                .jvmArgs("-XX:+UseFMA")
                .build())
                .run();
    }

    /*private final Matrix4 mMatrix = new Matrix4();
    {
        mMatrix.preRotate(MathUtil.PI_O_3, MathUtil.PI_O_6, MathUtil.PI_O_4);
    }
    private final long mData = MemoryUtil.nmemAlignedAllocChecked(8, 64);*/

    /*public static final Pixmap SRC_PIXMAP;
    public static final Pixmap DST_PIXMAP;*/
    public static final Path SRC_PATH;

    static {
        /*int[] x = {0}, y = {0}, channels = {0};
        var imgData = STBImage.stbi_load(
                "F:/123459857_p0.png",
                x, y, channels, 4
        );
        assert imgData != null;
        SRC_PIXMAP = new Pixmap(
                ImageInfo.make(x[0], y[0], ColorInfo.CT_RGBA_8888, ColorInfo.AT_UNPREMUL, null),
                null,
                MemoryUtil.memAddress(imgData),
                4 * x[0]
        );
        var newInfo = ImageInfo.make(x[0], y[0], ColorInfo.CT_BGR_565, ColorInfo.AT_UNPREMUL, null);
        long newPixels = MemoryUtil.nmemAlloc(newInfo.computeMinByteSize());
        assert newPixels != 0;
        DST_PIXMAP = new Pixmap(
                newInfo, null, newPixels, newInfo.minRowBytes()
        );*/
        Path src = new Path();
        src.moveTo(100, 120);
        src.lineTo(130, 160);
        src.lineTo(100, 200);
        src.lineTo(180, 140);
        src.lineTo(170, 130);
        src.lineTo(170, 120);
        src.cubicTo(160, 130, 120, 100, 190, 60);
        SRC_PATH = src;
    }

    /*@Benchmark
    public void uploadMethod1() {
        boolean res = SRC_PIXMAP.readPixels(DST_PIXMAP, 0, 0);
        assert res;
    }*/

    @Benchmark
    public void arcStroker(Blackhole bh) {
        Path dst = new Path();
        StrokeRec strokeRec = new StrokeRec();
        strokeRec.setWidth(10);
        strokeRec.setStrokeParams(Paint.CAP_ROUND, Paint.JOIN_ROUND, Paint.ALIGN_CENTER, 4);
        strokeRec.applyToPath(SRC_PATH, dst);
        bh.consume(dst);
    }

    @Benchmark
    public void marlinStroker(Blackhole bh) {
        BasicStroke strokeRec = new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 4);
        var shape = strokeRec.createStrokedShape(SRC_PATH);
        bh.consume(shape);
    }

    @Benchmark
    public void marlinStrokerConvert(Blackhole bh) {
        BasicStroke strokeRec = new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 4);
        var shape = strokeRec.createStrokedShape(SRC_PATH);
        Path dst = J2DUtils.toPath(shape.getPathIterator(null), null);
        bh.consume(dst);
    }
}
