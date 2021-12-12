/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.ModernUI;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Fork(1)
@Threads(2)
@Warmup(iterations = 1, time = 3)
@Measurement(iterations = 3, time = 3)
public class TestBenchmark {

    public static void main(String[] args) throws RunnerException {
        /*new Runner(new OptionsBuilder()
                .include(TestBenchmark.class.getSimpleName())
                .shouldFailOnError(true).shouldDoGC(true)
                .jvmArgs("-server").build()).run();*/
        Int2ObjectMap<String> map = new Int2ObjectOpenHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        int n = 0;
        while (map.containsKey(++n));
        ModernUI.LOGGER.info(n);
    }

    public static void testReentrantLock() {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        Lock readLock = lock.readLock();

        Int2ObjectOpenHashMap<Object> map = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < 33; i++) {
            map.put(i, new Object());
        }

        for (int i = 0; i < 1000000; i++) {
            readLock.lock();
            try {
                map.get(1);
            } finally {
                readLock.unlock();
            }
        }
    }

    static volatile ConcurrentHashMap<Integer, Object> conMap = new ConcurrentHashMap<>();

    static volatile Int2ObjectMap<Object> map = new Int2ObjectOpenHashMap<>();
    static volatile Int2ObjectMap<Object> syncMap = new Int2ObjectOpenHashMap<>();
    static final Object lock = new Object();

    static {
        for (int i = 0; i < 33; i++) {
            conMap.put(i, new Object());
        }
        for (int i = 0; i < 33; i++) {
            map.put(i, new Object());
        }
        for (int i = 0; i < 33; i++) {
            syncMap.put(i, new Object());
        }
    }

    @Benchmark
    public static void testConcurrentMap() {
        var m = conMap;
        for (int i = 0; i < 1000000; i++) {
            m.get(13);
        }
    }

    @Benchmark
    public static void testSynchronizedMap() {
        var m = syncMap;
        for (int i = 0; i < 1000000; i++) {
            synchronized (m) {
                m.get(13);
            }
        }
    }

    @Benchmark
    public static void testMap() {
        var m = map;
        for (int i = 0; i < 1000000; i++) {
            m.get(13);
        }
    }
}
