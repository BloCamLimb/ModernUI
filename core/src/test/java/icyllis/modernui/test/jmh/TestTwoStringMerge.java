/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.ThreadLocalRandom;

@Fork(value = 2)
@Threads(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestTwoStringMerge {

    //Benchmark                   (src)   Mode  Cnt         Score         Error  Units
    //TestTwoStringMerge.method1      1  thrpt   10   6292315.734 ±  625506.255  ops/s
    //TestTwoStringMerge.method1      2  thrpt   10   8711248.738 ± 1194624.207  ops/s
    //TestTwoStringMerge.method2      1  thrpt   10  11467579.456 ± 2908541.791  ops/s
    //TestTwoStringMerge.method2      2  thrpt   10  10779466.438 ± 1148551.335  ops/s
    //TestTwoStringMerge.method3      1  thrpt   10   4087318.635 ±  984021.501  ops/s
    //TestTwoStringMerge.method3      2  thrpt   10   5460852.102 ±  882497.490  ops/s
    //TestTwoStringMerge.method4      1  thrpt   10  11089283.204 ±  286009.193  ops/s
    //TestTwoStringMerge.method4      2  thrpt   10   9617890.768 ±  157849.299  ops/s
    //TestTwoStringMerge.method5      1  thrpt   10  10846157.581 ±  744899.435  ops/s
    //TestTwoStringMerge.method5      2  thrpt   10   9490837.982 ±  223790.841  ops/s

    public static String src1 = "A comprehensive solution for building cross-platform, highly interactive modern desktop applications. Leveraging a robust graphics engine and rich UI components, it empowers developers to swiftly craft professional-grade enterprise software and scalable applications tailored to diverse workflows. The framework's adaptable architecture and visual development tools streamline implementation across industries, from productivity platforms to mission-critical systems.";
    public static String src2 = "A comprehensive solution for building cross-platform, highly interactive modern desktop applications: 构建跨平台、高度交互的现代桌面应用程序的全面解决方案。它利用强大的图形引擎和丰富的 UI 组件，使开发人员能够快速构建专业级企业软件和可扩展的应用程序，以满足各种工作流程的需求。该框架的适应性架构和可视化开发工具简化了从生产力平台到关键任务系统等各行各业的实施。";
    public static final ThreadLocal<char[]> TLS = ThreadLocal.withInitial(() -> new char[2000]);

    @Param({"1","2"})
    public int src;
    private char[] buffer;
    private int start1, end1;
    private int start2, end2;

    @Setup
    public void setup() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        buffer = (src == 1 ? src1 : src2).toCharArray();
        start1 = 0;
        end1 = buffer.length / 2 - rnd.nextInt(5, 15);
        start2 = buffer.length / 2 + rnd.nextInt(5, 15);
        end2 = buffer.length;
    }

    @Benchmark
    public String method1() {
        int len1 = end1 - start1;
        int len2 = end2 - start2;
        char[] buf = new char[len1 + len2];
        System.arraycopy(buffer, start1, buf, 0, len1);
        System.arraycopy(buffer, start2, buf, len1, len2);
        return new String(buf);
    }

    @Benchmark
    public String method2() {
        int len1 = end1 - start1;
        int len2 = end2 - start2;
        char[] buf = TLS.get();
        System.arraycopy(buffer, start1, buf, 0, len1);
        System.arraycopy(buffer, start2, buf, len1, len2);
        return new String(buf, 0, len1 + len2);
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    @Benchmark
    public String method3() {
        int len1 = end1 - start1;
        int len2 = end2 - start2;
        StringBuilder sb = new StringBuilder(len1 + len2);
        sb.append(buffer, start1, len1)
                .append(buffer, start2, len2);
        return sb.toString();
    }

    @Benchmark
    public String method4() {
        int len1 = end1 - start1;
        int len2 = end2 - start2;
        return new String(buffer, start1, len1) + new String(buffer, start2, len2);
    }

    @Benchmark
    public String method5() {
        int len1 = end1 - start1;
        int len2 = end2 - start2;
        return new String(buffer, start1, len1).concat(new String(buffer, start2, len2));
    }
}
