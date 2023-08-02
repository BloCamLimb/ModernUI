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

package icyllis.modernui.util;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.MathUtil;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

abstract class L0Padding<E> {
    long p000;
    long p001;
    long p002;
    long p003;
    long p004;
    long p005;
    long p006;
    long p007;
    long p008;
    long p009;
    long p010;
    long p011;
    long p012;
    long p013;
    long p014;
    long p015;
}

abstract class HeadPadding<E> extends L0Padding<E> {
    volatile long head;
}

abstract class L1Padding<E> extends HeadPadding<E> {
    long p100;
    long p101;
    long p102;
    long p103;
    long p104;
    long p105;
    long p106;
    long p107;
    long p108;
    long p109;
    long p110;
    long p111;
    long p112;
    long p113;
    long p114;
    long p115;
}

abstract class TailPadding<E> extends L1Padding<E> {
    volatile long tail;
}

abstract class L2Padding<E> extends TailPadding<E> {
    long p200;
    long p201;
    long p202;
    long p203;
    long p204;
    long p205;
    long p206;
    long p207;
    long p208;
    long p209;
    long p210;
    long p211;
    long p212;
    long p213;
    long p214;
    long p215;
}

// Inspired by JCTools and zio
@ThreadSafe
public class MpmcArrayQueue<E> extends L2Padding<E> implements Pools.Pool<E> {
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle SEQ;

    static {
        var lookup = MethodHandles.lookup();
        try {
            HEAD = lookup.findVarHandle(HeadPadding.class, "head", long.class);
            TAIL = lookup.findVarHandle(TailPadding.class, "tail", long.class);
            SEQ = MethodHandles.arrayElementVarHandle(long[].class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final long MAX_SEQ = 1L << 62;

    private final long mask;
    private final E[] buf;
    private final long[] seq;

    @SuppressWarnings("unchecked")
    public MpmcArrayQueue(int capacity) {
        if (capacity <= 0 || capacity > (1 << (Integer.SIZE - 2)))
            throw new IllegalArgumentException();
        int n = MathUtil.ceilPow2(capacity);
        mask = n - 1;
        buf = (E[]) new Object[n];
        seq = new long[n];
        for (int i = 0; i < n; i++) {
            SEQ.setVolatile(seq, i, (long) i);
        }
    }

    public int size() {
        long after = (long) HEAD.getVolatile(this);
        long size;
        for (;;) {
            final long before = after;
            final long currentTail = (long) TAIL.getVolatile(this);
            after = (long) HEAD.getVolatile(this);
            if (before == after) {
                size = safeDiff(currentTail, after);
                break;
            }
        }
        return (int) MathUtil.clamp(size, 0L, 1L << 31);
    }

    public boolean isEmpty() {
        long after = (long) HEAD.getVolatile(this);
        for (;;) {
            final long before = after;
            final long currentTail = (long) TAIL.getVolatile(this);
            after = (long) HEAD.getVolatile(this);
            if (before == after) {
                return currentTail == after;
            }
        }
    }

    public boolean isFull() {
        long after = (long) HEAD.getVolatile(this);
        for (;;) {
            final long before = after;
            final long currentTail = (long) TAIL.getVolatile(this);
            after = (long) HEAD.getVolatile(this);
            if (before == after) {
                return currentTail == safeNext(after + mask);
            }
        }
    }

    @Nullable
    @Override
    public E acquire() {
        final long mask = this.mask;
        final long[] seq = this.seq;
        long curSeq;
        long curHead = (long) HEAD.getVolatile(this);
        long curTail;
        int curPos;
        for (;;) {
            curPos = (int) (curHead & mask);
            curSeq = (long) SEQ.getVolatile(seq, curPos);

            long diff = safeDiff(curSeq, curHead);
            if (diff <= 0) {
                // There may be two distinct cases:
                // 1. curSeq == curHead
                //    This means there is no item available to dequeue. However
                //    there may be in-flight enqueue, and we need to check for
                //    that.
                // 2. curSeq < curHead
                //    This is a tricky case. Polling thread T1 can observe
                //    `curSeq < curHead` if thread T0 started dequeing at
                //    position `curSeq` but got descheduled. Meantime enqueing
                //    threads enqueued another (capacity - 1) elements, and other
                //    dequeueing threads dequeued all of them. So, T1 wrapped
                //    around the buffer and cannot proceed until T0 finishes its
                //    dequeue.
                //
                //    It may sound surprising that a thread get descheduled
                //    during dequeue for `capacity` number of operations, but
                //    it's actually pretty easy to observe such situations even
                //    at queue capacity of 4096 elements.
                //
                //    Anyway, in this case we can report that the queue is empty.

                curTail = (long) TAIL.getVolatile(this);
                if (safeDiff(curHead, curTail) >= 0) {
                    // There is no concurrent enqueue happening. We can report
                    // that that queue is empty.
                    return null;
                }// else {
                    // There is an ongoing enqueue. A producer had reserved the
                    // place, but hasn't published an element just yet. Let's
                    // spin for a little while, as publishing should happen
                    // momentarily.
                //}
            } else if (diff == 1) {
                // We're at the right spot, and can try to reserve the spot
                // for dequeue.
                if (HEAD.compareAndSet(this, curHead, safeNext(curHead))) {
                    // Successfully reserved the spot and can proceed to dequeueing.
                    break;
                } else {
                    // Another concurrent dequeue won. Let's try again at the next location.
                    curHead = safeNext(curHead);
                }
            } else { // curSeq > curHead + 1
                // Either some other thread beat us or this thread got
                // delayed. We need to resynchronize with `head` and try again.
                curHead = (long) HEAD.getVolatile(this);
            }
        }

        // See the comment in offer method about volatile writes and
        // visibility guarantees.
        var instance = buf[curPos];
        buf[curPos] = null;

        SEQ.setRelease(seq, curPos, safeNext(curHead + mask));

        return instance;
    }

    @Override
    public boolean release(@NonNull E instance) {
        Objects.requireNonNull(instance);
        final long mask = this.mask;
        final long[] seq = this.seq;
        long curSeq;
        long curHead;
        long curTail = (long) TAIL.getVolatile(this);
        int curPos;
        for (;;) {
            curPos = (int) (curTail & mask);
            curSeq = (long) SEQ.getVolatile(seq, curPos);

            long diff = safeDiff(curSeq, curTail);
            if (diff < 0) {
                // This means we're about to wrap around the buffer, i.e. the
                // queue is likely full. But there may be a dequeuing
                // happening at the moment, so we need to check for this.
                curHead = (long) HEAD.getVolatile(this);
                if (safeDiff(curTail, safeNext(curHead + mask)) >= 0) {
                    // This case implies that there is no in-progress dequeue,
                    // we can just report that the queue is full.
                    return false;
                }// else {
                    // This means that the consumer moved the head of the queue
                    // (i.e. reserved a place to dequeue from), but hasn't yet
                    // loaded an element from `buf` and hasn't updated the
                    // `seq`. However, this should happen momentarily, so we can
                    // just spin for a little while.
                //}
            } else if (diff == 0) {
                // We're at the right spot. At this point we can try to
                // reserve the place for enqueue by doing CAS on tail.
                if (TAIL.compareAndSet(this, curTail, safeNext(curTail))) {
                    // We successfully reserved a place to enqueue.
                    break;
                } else {
                    // There was a concurrent offer that won CAS. We need to try again at the next location.
                    curTail = safeNext(curTail);
                }
            } else { // curSeq > curTail
                // Either some other thread beat us enqueued an right element
                // or this thread got delayed. We need to resynchronize with
                // `tail` and try again.
                curTail = (long) TAIL.getVolatile(this);
            }
        }

        // To add an element into the queue we do
        // 1. plain store into `buf`,
        // 2. volatile write of a `seq` value.
        // Following volatile read of `curTail + 1` guarantees
        // that plain store will be visible as it happens before in
        // program order.
        //
        // The volatile write can actually be relaxed to ordered store
        // (`lazySet`).  See Doug Lea's response in
        // [[http://cs.oswego.edu/pipermail/concurrency-interest/2011-October/008296.html]].
        buf[curPos] = instance;
        SEQ.setRelease(seq, curPos, safeNext(curTail));
        return true;
    }

    // the seq will wrap one day, handle it
    private static long safeNext(long v) {
        return (v + 1) & (MAX_SEQ - 1);
    }

    // (a - b) wrap at midpoint
    private static long safeDiff(long a, long b) {
        long d = a - b;
        if (d >= (MAX_SEQ >> 1)) {
            return d - MAX_SEQ;
        } else if (d <= -(MAX_SEQ >> 1)) {
            return d + MAX_SEQ;
        } else {
            return d;
        }
    }
}
