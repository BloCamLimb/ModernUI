/*
 * Modern UI.
 * Copyright (C) 2019-2025 BloCamLimb. All rights reserved.
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

import icyllis.modernui.util.MpmcArrayQueue;
import icyllis.modernui.util.Pools;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.ConcurrentLinkedQueue;

@Fork(1)
@Threads(20)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 5, time = 1)
public class TestRingBuffer {

    public static final int MAX_POOL_SIZE = 50;

    private static final Pools.Pool<Holder> pool = Pools.newSynchronizedPool(MAX_POOL_SIZE);

    private static final MpmcArrayQueue<Holder> queue = new MpmcArrayQueue<>(MAX_POOL_SIZE);
    private static final ConcurrentLinkedQueue<Holder> linked = new ConcurrentLinkedQueue<>();

    private static class Holder {
        int what;
        int arg0;
        int arg1;
        Object obj;
        boolean inUse;
        long when;
        Object target;
        Runnable callback;
        Holder next;
        int flags;
    }

    @Benchmark
    public static void concurrent() {
        Holder o1 = queue.acquire();
        Holder o2 = queue.acquire();
        if (o1 == null) {
            o1 = new Holder();
        } else if (o1.inUse) {
            throw new RuntimeException(o1 + " is in use");
        }
        o1.inUse = true;
        if (o2 == null) {
            o2 = new Holder();
        } else if (o2.inUse) {
            throw new RuntimeException(o2 + " is in use");
        }
        o2.inUse = true;
        Math.random();
        o1.inUse = false;
        o2.inUse = false;
        queue.release(o1);
        queue.release(o2);
    }

    @Benchmark
    public static void blocking() {
        Holder o1 = pool.acquire();
        Holder o2 = pool.acquire();
        if (o1 == null) {
            o1 = new Holder();
        } else if (o1.inUse) {
            throw new RuntimeException(o1 + " is in use");
        }
        o1.inUse = true;
        if (o2 == null) {
            o2 = new Holder();
        } else if (o2.inUse) {
            throw new RuntimeException(o2 + " is in use");
        }
        o2.inUse = true;
        Math.random();
        o1.inUse = false;
        o2.inUse = false;
        pool.release(o1);
        pool.release(o2);
    }

    @Benchmark
    public static void linked() {
        Holder o1 = linked.poll();
        Holder o2 = linked.poll();
        if (o1 == null) {
            o1 = new Holder();
        } else if (o1.inUse) {
            throw new RuntimeException(o1 + " is in use");
        }
        o1.inUse = true;
        if (o2 == null) {
            o2 = new Holder();
        } else if (o2.inUse) {
            throw new RuntimeException(o2 + " is in use");
        }
        o2.inUse = true;
        Math.random();
        o1.inUse = false;
        o2.inUse = false;
        linked.offer(o1);
        linked.offer(o2);
    }

    @Benchmark
    public static void normal() {
        Holder o1;
        Holder o2;
        o1 = new Holder();
        o2 = new Holder();
        if (o1.inUse) {
            throw new RuntimeException(o1 + " is in use");
        }
        o1.inUse = true;
        if (o2.inUse) {
            throw new RuntimeException(o2 + " is in use");
        }
        o2.inUse = true;
        Math.random();
        o1.inUse = false;
        o2.inUse = false;
    }
}
