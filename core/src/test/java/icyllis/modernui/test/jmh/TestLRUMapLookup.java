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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.LinkedHashMap;

@Fork(2)
@Threads(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestLRUMapLookup {

    private static final LinkedHashMap<String, Object> linkedhashmap = new LinkedHashMap<>(16, 0.5f, true);
    private static final CustomCache custom = new CustomCache();
    /*private static final Cache<String, Object> caffeine = Caffeine.newBuilder()
            .maximumSize(5000)
            .build();*/

    static class CustomCache {
        private final HashMap<Object, Node> mMap = new HashMap<>();

        private Node mHead;
        private Node mTail;

        private volatile int lockState = 0;

        private static final VarHandle STATE;

        static {
            try {
                STATE = MethodHandles.lookup()
                        .findVarHandle(CustomCache.class, "lockState", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        public void lock() {
            if (STATE.compareAndSet(this, 0, 1)) {
                return;
            }

            int spinCount = 0;
            while (true) {
                while ((int) STATE.getAcquire(this) == 1) {
                    if (++spinCount < 16) {
                        Thread.onSpinWait();
                    } else {
                        Thread.yield();
                        spinCount = 0;
                    }
                }
                if ((int) STATE.compareAndExchangeAcquire(this, 0, 1) == 0) {
                    return;
                }
            }
            /*while (true) {
                if ((int) STATE.compareAndExchangeAcquire(this, 0, 1) == 0) {
                    return;
                }
                if ((int) STATE.getAcquire(this) != 0) {
                    if (++spinCount < 32) {
                        Thread.onSpinWait();
                    } else {
                        Thread.yield();
                        spinCount = 0;
                    }
                }
            }*/
        }

        @Nullable
        public Object get(@NonNull String key) {
            lock();
            try {
                Node entry = mMap.get(key);
                if (entry != null) {
                    moveToHead(entry);
                }
                return entry;
            } finally {
                STATE.setRelease(this, 0);
            }
        }

        public void put(@NonNull String key, Node entry) {
            lock();
            try {
                mMap.put(key, entry);
                addToHead(entry);
            } finally {
                STATE.setRelease(this, 0);
            }
        }

        private void moveToHead(@NonNull Node entry) {
            assert mHead != null && mTail != null;
            if (mHead == entry) {
                return;
            }

            var prev = entry.mPrev;
            var next = entry.mNext;

            if (prev != null) {
                prev.mNext = next;
            } else {
                mHead = next;
            }
            if (next != null) {
                next.mPrev = prev;
            } else {
                mTail = prev;
            }

            entry.mPrev = null;
            entry.mNext = mHead;
            if (mHead != null) {
                mHead.mPrev = entry;
            }
            mHead = entry;
            if (mTail == null) {
                mTail = entry;
            }
        }

        private void addToHead(@NonNull Node entry) {
            entry.mPrev = null;
            entry.mNext = mHead;
            if (mHead != null) {
                mHead.mPrev = entry;
            }
            mHead = entry;
            if (mTail == null) {
                mTail = entry;
            }
        }

        static class Node {
            Node mPrev, mNext;
        }
    }

    static {
        long val = 7;
        for (int i = 0; i < 1500; i++) {
            String key = Long.toString(val);
            linkedhashmap.put(key, new Object());
            custom.put(key, new CustomCache.Node());
            //caffeine.put(key, new Object());
            val *= 13;
        }
        for (int i = 3500; i < 4500; i += 2) {
            String key = Integer.toString(i);
            linkedhashmap.put(key, new Object());
            custom.put(key, new CustomCache.Node());
            //caffeine.put(key, new Object());
        }
    }

    @State(Scope.Thread)
    public static class InputState {
        String[] inputs;
        int index = 0;

        @Setup
        public void setUp() {
            inputs = new String[512];
            for (int i = 0; i < 512; i++) {
                inputs[i] = Integer.toString(i + 3200);
            }
        }

        public String get() {
            return inputs[index++ & 511];
        }
    }

    /*@Benchmark
    public void linkedhashmap(InputState input, Blackhole bh) {
        synchronized (linkedhashmap) {
            bh.consume(linkedhashmap.get(input.get()));
        }
    }*/

    @Benchmark
    public void custom(InputState input, Blackhole bh) {
        bh.consume(custom.get(input.get()));
    }

    /*@Benchmark
    public void caffeine(InputState input, Blackhole bh) {
        bh.consume(caffeine.getIfPresent(input.get()));
    }*/
}
