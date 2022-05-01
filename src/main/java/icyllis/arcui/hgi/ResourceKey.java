/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.hgi;

import it.unimi.dsi.fastutil.longs.LongHash;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Base class for all Resource cache keys.
 */
@Deprecated
public abstract class ResourceKey implements Cloneable {

    ResourceKey() {
    }

    /**
     * Resets to an invalid key.
     */
    public abstract void invalidate();

    /**
     * @return true if valid, that is, initialized
     */
    public abstract boolean isValid();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public ResourceKey clone() {
        try {
            return (ResourceKey) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public static boolean isValid(ResourceKey key) {
        return key != null && key.isValid();
    }

    public static boolean isInvalid(ResourceKey key) {
        return key == null || !key.isValid();
    }

    // DEPRECATED BELOW

    static long make(long key, int domain, int data0) {
        assert domain > 0 && domain <= 0xFFFF;
        int domainSize = domain | (12 << 16);
        int hash = domainSize;
        hash = 31 * hash + data0;
        if (key == NULL || size(key) < 12) {
            key = nmemReallocChecked(key, 12);
        }
        memPutInt(key, hash);
        memPutInt(key + 4, domainSize);
        memPutInt(key + 8, data0);
        return key;
    }

    static long make(long key, int domain, int data0, int data1) {
        assert domain > 0 && domain <= 0xFFFF;
        int domainSize = domain | (16 << 16);
        int hash = domainSize;
        hash = 31 * hash + data0;
        hash = 31 * hash + data1;
        if (key == NULL || size(key) < 16) {
            key = nmemReallocChecked(key, 16);
        }
        memPutInt(key, hash);
        memPutInt(key + 4, domainSize);
        memPutInt(key + 8, data0);
        memPutInt(key + 12, data1);
        return key;
    }

    static long make(long key, int domain, int data0, int data1, int data2) {
        assert domain > 0 && domain <= 0xFFFF;
        int domainSize = domain | (20 << 16);
        int hash = domainSize;
        hash = 31 * hash + data0;
        hash = 31 * hash + data1;
        hash = 31 * hash + data2;
        if (key == NULL || size(key) < 20) {
            key = nmemReallocChecked(key, 20);
        }
        memPutInt(key, hash);
        memPutInt(key + 4, domainSize);
        memPutInt(key + 8, data0);
        memPutInt(key + 12, data1);
        memPutInt(key + 16, data2);
        return key;
    }

    static long make(long key, int domain, int data0, int data1, int data2, int data3) {
        assert domain > 0 && domain <= 0xFFFF;
        int domainSize = domain | (24 << 16);
        int hash = domainSize;
        hash = 31 * hash + data0;
        hash = 31 * hash + data1;
        hash = 31 * hash + data2;
        hash = 31 * hash + data3;
        if (key == NULL || size(key) < 24) {
            key = nmemReallocChecked(key, 24);
        }
        memPutInt(key, hash);
        memPutInt(key + 4, domainSize);
        memPutInt(key + 8, data0);
        memPutInt(key + 12, data1);
        memPutInt(key + 16, data2);
        memPutInt(key + 20, data3);
        return key;
    }

    static long make(long key, int domain, int data0, int data1, int data2, int data3, int data4) {
        assert domain > 0 && domain <= 0xFFFF;
        int domainSize = domain | (28 << 16);
        int hash = domainSize;
        hash = 31 * hash + data0;
        hash = 31 * hash + data1;
        hash = 31 * hash + data2;
        hash = 31 * hash + data3;
        hash = 31 * hash + data4;
        if (key == NULL || size(key) < 28) {
            key = nmemReallocChecked(key, 28);
        }
        memPutInt(key, hash);
        memPutInt(key + 4, domainSize);
        memPutInt(key + 8, data0);
        memPutInt(key + 12, data1);
        memPutInt(key + 16, data2);
        memPutInt(key + 20, data3);
        memPutInt(key + 24, data4);
        return key;
    }

    // make the key on stack, cannot be resized
    static long make(int domain, int data0, MemoryStack stack) {
        assert domain > 0 && domain <= 0xFFFF;
        int domainSize = domain | (12 << 16);
        int hash = domainSize;
        hash = 31 * hash + data0;
        long key = stack.nmalloc(12);
        memPutInt(key, hash);
        memPutInt(key + 4, domainSize);
        memPutInt(key + 8, data0);
        return key;
    }

    static long make(int domain, int data0, int data1, MemoryStack stack) {
        assert domain > 0 && domain <= 0xFFFF;
        int domainSize = domain | (16 << 16);
        int hash = domainSize;
        hash = 31 * hash + data0;
        hash = 31 * hash + data1;
        long key = stack.nmalloc(16);
        memPutInt(key, hash);
        memPutInt(key + 4, domainSize);
        memPutInt(key + 8, data0);
        memPutInt(key + 12, data1);
        return key;
    }

    static long make(int domain, int data0, int data1, int data2, MemoryStack stack) {
        assert domain > 0 && domain <= 0xFFFF;
        int domainSize = domain | (20 << 16);
        int hash = domainSize;
        hash = 31 * hash + data0;
        hash = 31 * hash + data1;
        hash = 31 * hash + data2;
        long key = stack.nmalloc(20);
        memPutInt(key, hash);
        memPutInt(key + 4, domainSize);
        memPutInt(key + 8, data0);
        memPutInt(key + 12, data1);
        memPutInt(key + 16, data2);
        return key;
    }

    static long make(int domain, int data0, int data1, int data2, int data3, MemoryStack stack) {
        assert domain > 0 && domain <= 0xFFFF;
        int domainSize = domain | (24 << 16);
        int hash = domainSize;
        hash = 31 * hash + data0;
        hash = 31 * hash + data1;
        hash = 31 * hash + data2;
        hash = 31 * hash + data3;
        long key = stack.nmalloc(24);
        memPutInt(key, hash);
        memPutInt(key + 4, domainSize);
        memPutInt(key + 8, data0);
        memPutInt(key + 12, data1);
        memPutInt(key + 16, data2);
        memPutInt(key + 20, data3);
        return key;
    }

    static long make(int domain, int data0, int data1, int data2, int data3, int data4, MemoryStack stack) {
        assert domain > 0 && domain <= 0xFFFF;
        int domainSize = domain | (28 << 16);
        int hash = domainSize;
        hash = 31 * hash + data0;
        hash = 31 * hash + data1;
        hash = 31 * hash + data2;
        hash = 31 * hash + data3;
        hash = 31 * hash + data4;
        long key = stack.nmalloc(28);
        memPutInt(key, hash);
        memPutInt(key + 4, domainSize);
        memPutInt(key + 8, data0);
        memPutInt(key + 12, data1);
        memPutInt(key + 16, data2);
        memPutInt(key + 20, data3);
        memPutInt(key + 24, data4);
        return key;
    }

    LongHash.Strategy HASH_STRATEGY = new LongHash.Strategy() {
        @Override
        public int hashCode(long e) {
            return memGetInt(e);
        }

        @Override
        public boolean equals(long a, long b) {
            int domainSizeA = memGetInt(a + 4);
            int domainSizeB = memGetInt(b + 4);
            // for invalid keys, these are equal, then go default branch
            if (domainSizeA != domainSizeB) {
                return false;
            }
            // for small size block, UNSAFE will be faster
            return switch (domainSizeA >>> 16) {
                case 12 -> memGetInt(a + 8) == memGetInt(b + 8);
                case 16 -> memGetLong(a + 8) == memGetLong(b + 8);
                case 20 -> memGetLong(a + 8) == memGetLong(b + 8) &&
                        memGetInt(a + 16) == memGetInt(b + 16);
                case 24 -> memGetLong(a + 8) == memGetLong(b + 8) &&
                        memGetLong(a + 16) == memGetLong(b + 16);
                case 28 -> memGetLong(a + 8) == memGetLong(b + 8) &&
                        memGetLong(a + 16) == memGetLong(b + 16) &&
                        memGetInt(a + 24) == memGetInt(b + 24);
                default -> true;
            };
        }
    };

    // may be zero, if invalid
    static int domain(long key) {
        return memGetInt(key + 4) & 0xFFFF;
    }

    // not the key size, zero for invalid
    static int size(long key) {
        return memGetInt(key + 4) >>> 16;
    }

    static int data(long key, int index) {
        return memGetInt(key + ((index + 2L) << 2));
    }

    static boolean isValid(long key) {
        // check domain_and_size if not zero
        return key != NULL && memGetInt(key + 4) != 0;
    }

    // reset to invalid
    static long reset(long key) {
        if (key != NULL) {
            // clear hash and domain_and_size to zero
            memPutLong(key, 0);
        }
        return key;
    }

    // copy src to dst, return dst
    static long set(long dst, long src) {
        int size;
        if (src == NULL || (size = size(src)) == 0) {
            return reset(dst);
        }
        if (dst == NULL) {
            dst = nmemAllocChecked(size);
        } else if (size(dst) < size) {
            // we don't care if dst invalid, just realloc
            dst = nmemReallocChecked(dst, size);
        }
        // copy metadata
        memPutLong(dst, memGetLong(src));
        // copy data
        switch (size) {
            case 12 -> memPutInt(dst + 8, memGetInt(src + 8));
            case 16 -> memPutLong(dst + 8, memGetLong(src + 8));
            case 20 -> {
                memPutLong(dst + 8, memGetLong(src + 8));
                memPutInt(dst + 16, memGetInt(src + 16));
            }
            case 24 -> {
                memPutLong(dst + 8, memGetLong(src + 8));
                memPutLong(dst + 16, memGetLong(src + 16));
            }
            case 28 -> {
                memPutLong(dst + 8, memGetLong(src + 8));
                memPutLong(dst + 16, memGetLong(src + 16));
                memPutInt(dst + 24, memGetInt(src + 24));
            }
        }
        return dst;
    }
}
