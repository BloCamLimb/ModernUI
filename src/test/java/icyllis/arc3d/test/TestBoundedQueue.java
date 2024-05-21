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

import java.util.concurrent.atomic.AtomicInteger;

public class TestBoundedQueue<T> {

    private final AtomicInteger mHead = new AtomicInteger();
    private final AtomicInteger mTail = new AtomicInteger();

    private final T[] mPool;

    @SuppressWarnings("unchecked")
    public TestBoundedQueue(int size) {
        mPool = (T[]) new Object[size];
    }

    public boolean push(T value) {
        int tail = mTail.getOpaque();
        int nextTail = inc(tail, mPool.length);
        if (nextTail == mHead.getAcquire()) {
            return false;
        }
        mPool[tail] = value;
        mTail.setRelease(nextTail);
        return true;
    }

    public T pop() {
        int head = mHead.getOpaque();
        if (head != mTail.getAcquire()) {
            T value = mPool[head];
            if (mHead.compareAndSet(head, inc(head, mPool.length))) {
                return value;
            }
        }
        return null;
    }

    static int inc(int i, int modulus) {
        if (++i >= modulus) i = 0;
        return i;
    }

    private static class Holder {
        boolean inUse;
    }

    public static void main(String[] args) {
        var queue = new TestBoundedQueue<Holder>(3);
        while (queue.push(new Holder()))
            ;
        for (int i = 0; i < 500; i++) {
            new Thread(() -> {
                Holder o1 = queue.pop();
                Holder o2 = queue.pop();
                boolean new1 = false;
                boolean new2 = false;
                if (o1 == null) {
                    o1 = new Holder();
                    new1 = true;
                } else if (o1.inUse) {
                    throw new RuntimeException(o1 + " is in use");
                }
                o1.inUse = true;
                if (o2 == null) {
                    o2 = new Holder();
                    new2 = true;
                } else if (o2.inUse) {
                    throw new RuntimeException(o2 + " is in use");
                }
                o2.inUse = true;
                System.out.printf("%s obtained: o1 %s o2 %s (new1 %b new2 %b)\n", Thread.currentThread(),
                        o1, o2, new1, new2);
                o1.inUse = false;
                o2.inUse = false;
                queue.push(o1);
                queue.push(o2);
                System.out.printf("%s released: o1 %s o2 %s\n", Thread.currentThread(), o1, o2);
            }).start();
        }
    }
}
