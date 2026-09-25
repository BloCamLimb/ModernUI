/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.test.jmh;

import icyllis.modernui.annotation.NonNull;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@Threads(1)
public class TestClassLookup {

    private String className;
    private ClassLoader classLoader;
    private WeakHashMap<ClassLoader, ConcurrentHashMap<String, WeakReference<Class<?>>>> cacheMap;

    public Function<String, ?> factory;
    public Function<String, ?> factory2;

    public static class MyClass {

        public MyClass(String s) {
        }
    }

    private static class NameCache extends HashMap<String, WeakReference<Class<?>>> {
        private final StampedLock lock = new StampedLock();

        final Class<?> compute(@NonNull ClassLoader loader, @NonNull String name) {
            long stamp = lock.tryOptimisticRead();
            WeakReference<Class<?>> value = get(name);
            if (lock.validate(stamp)) {
                if (value != null) return value.get();
            } else {
                stamp = lock.readLock();
                try {
                    value = get(name);
                    if (value != null) return value.get();
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            Class<?> target;
            try {
                target = loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            WeakReference<Class<?>> computed = new WeakReference<>(target);

            long ws = lock.writeLock();
            try {
                WeakReference<Class<?>> existing = putIfAbsent(name, computed);
                if (existing == null) {
                    return computed.get();
                } else {
                    // there's race, just discard the newly created value
                    return existing.get();
                }
            } finally {
                lock.unlockWrite(ws);
            }
        }
    }

    private static final WeakHashMap<ClassLoader, NameCache> sClassCache = new WeakHashMap<>();
    private static final StampedLock sCacheLock = new StampedLock();

    private static NameCache getClassNameCache(@NonNull ClassLoader loader) {
        long stamp = sCacheLock.tryOptimisticRead();
        NameCache value = sClassCache.get(loader);
        if (sCacheLock.validate(stamp)) {
            if (value != null) return value;
        } else {
            stamp = sCacheLock.readLock();
            try {
                value = sClassCache.get(loader);
                if (value != null) return value;
            } finally {
                sCacheLock.unlockRead(stamp);
            }
        }

        final NameCache computed = new NameCache();

        long ws = sCacheLock.writeLock();
        try {
            NameCache existing = sClassCache.putIfAbsent(loader, computed);
            if (existing == null) {
                return computed;
            } else {
                // there's race, just discard the newly created value
                return existing;
            }
        } finally {
            sCacheLock.unlockWrite(ws);
        }
    }

    @Setup(Level.Iteration)
    public void setup() throws Exception {

        className = "java.lang.String";
        classLoader = Thread.currentThread().getContextClassLoader();
        ConcurrentHashMap<String, WeakReference<Class<?>>> map = new ConcurrentHashMap<>();
        map.put(className, new WeakReference<>(classLoader.loadClass(className)));
        cacheMap = new WeakHashMap<>();
        cacheMap.put(classLoader, map);

        factory = MyClass::new;
        var ctor = MethodHandles.publicLookup().findConstructor(MyClass.class,
                        MethodType.methodType(void.class, String.class))
                .asType(MethodType.methodType(Object.class, String.class));
        factory2 = s -> {
            try {
                return ctor.invokeExact(s);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    @State(Scope.Thread)
    public static class Input {

        Class<?>[] clz = {MyClass.class, String.class};
        int i;

        int[] sizes = {2, 6, 9, 7};

        Class<?> next() {
            return clz[i & 1];
        }

        int nextInt() {
            return sizes[i++ & 3];
        }
    }

    //Benchmark            Mode  Cnt    Score   Error  Units
    //TestClassLookup.l    avgt    5    9.344 ± 0.899  ns/op
    //TestClassLookup.mh   avgt    5   14.610 ± 0.344  ns/op

    //TestClassLookup.dc   avgt    5  209.775 ± 7.719  ns/op
    //TestClassLookup.chm  avgt    5   10.249 ± 0.124  ns/op
    //TestClassLookup.map  avgt    5   13.041 ± 0.216  ns/op

    @Benchmark
    public Object l(Blackhole bh, Input input) {
        var clazz = input.next() == MyClass.class;
        var sz = input.nextInt();
        bh.consume(clazz ? new MyClass[sz] : new String[sz]);
        return factory.apply("S");
    }

    @Benchmark
    public Object mh(Blackhole bh, Input input) {
        Object[] a = (Object[]) Array.newInstance(input.next(), input.nextInt());
        bh.consume(a);
        return factory2.apply("S");
    }

    @Benchmark
    public Object dc() throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    @Benchmark
    public Object map() {
        return getClassNameCache(classLoader).compute(classLoader, className);
    }

    @Benchmark
    public Object chm() {
        return cacheMap.get(classLoader).get(className).get();
    }
}
