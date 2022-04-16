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

package icyllis.arcui.graphics;

import it.unimi.dsi.fastutil.longs.LongHash;
import org.lwjgl.system.MemoryStack;

import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Base class for all gpu Resource cache keys. There are two types of cache keys. Refer to the
 * comments for each key type below. <b>WARNING: UNSAFE OPERATIONS</b>
 * <h3>Scratch Key</h3>
 * A key used for scratch resources. There are three important rules about scratch keys:
 * <ul>
 * <li> Multiple resources can share the same scratch key. Therefore resources assigned the same
 * scratch key should be interchangeable with respect to the code that uses them.</li>
 * <li> A resource can have at most one scratch key and it is set at resource creation by the
 * resource itself.</li>
 * <li> When a scratch resource is ref'ed it will not be returned from the
 * cache for a subsequent cache request until all refs are released. This facilitates using
 * a scratch key for multiple render-to-texture scenarios. An example is a separable blur:</li>
 * </ul>
 * <pre>{@code
 * GrTexture* texture[2];
 * texture[0] = get_scratch_texture(scratchKey);
 * texture[1] = get_scratch_texture(scratchKey); // texture[0] is already owned so we will get a
 * // different one for texture[1]
 * draw_mask(texture[0], path);        // draws path mask to texture[0]
 * blur_x(texture[0], texture[1]);     // blurs texture[0] in y and stores result in texture[1]
 * blur_y(texture[1], texture[0]);     // blurs texture[1] in y and stores result in texture[0]
 * texture[1]->unref();  // texture 1 can now be recycled for the next request with scratchKey
 * consume_blur(texture[0]);
 * texture[0]->unref();  // texture 0 can now be recycled for the next request with scratchKey
 * }</pre>
 * <h3>Unique Key</h3>
 * A key that allows for exclusive use of a resource for a use case (AKA "domain"). There are three
 * rules governing the use of unique keys:
 * <ul>
 * <li> Only one resource can have a given unique key at a time. Hence, "unique".</li>
 * <li> A resource can have at most one unique key at a time.</li>
 * <li> Unlike scratch keys, multiple requests for a unique key will return the same
 * resource even if the resource already has refs.</li>
 * </ul>
 * This key type allows a code path to create cached resources for which it is the exclusive user.
 * The code path creates a domain which it sets on its keys. This guarantees that there are no
 * cross-domain collisions.
 * <p>
 * Unique keys preempt scratch keys. While a resource has a unique key it is inaccessible via its
 * scratch key. It can become scratch again if the unique key is removed.
 */
public final class ResourceKey {

    private static final AtomicInteger sNextScratchDomain = new AtomicInteger(1);
    private static final AtomicInteger sNextUniqueDomain = new AtomicInteger(1);

    /**
     * Memory layout: uint32[2...7]
     * <p>
     * Fixed metadata, HASH and DOMAIN_AND_SIZE, with remaining data.
     * <p>
     * The allocation only increases but not decreases, default is NULL.
     * <p>
     * Scratch key and Unique key share the same layout, but domain
     * are different, the two cannot be mixed.
     */
    private ResourceKey() {
    }

    public static int createScratchDomain() {
        return sNextScratchDomain.getAndIncrement();
    }

    public static int createUniqueDomain() {
        return sNextUniqueDomain.getAndIncrement();
    }

    public static long make(long key, int domain, int data0) {
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

    public static long make(long key, int domain, int data0, int data1) {
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

    public static long make(long key, int domain, int data0, int data1, int data2) {
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

    public static long make(long key, int domain, int data0, int data1, int data2, int data3) {
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

    public static long make(long key, int domain, int data0, int data1, int data2, int data3, int data4) {
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
    public static long make(int domain, int data0, MemoryStack stack) {
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

    public static long make(int domain, int data0, int data1, MemoryStack stack) {
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

    public static long make(int domain, int data0, int data1, int data2, MemoryStack stack) {
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

    public static long make(int domain, int data0, int data1, int data2, int data3, MemoryStack stack) {
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

    public static long make(int domain, int data0, int data1, int data2, int data3, int data4, MemoryStack stack) {
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

    public static final LongHash.Strategy HASH_STRATEGY = new LongHash.Strategy() {
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
    public static int domain(long key) {
        return memGetInt(key + 4) & 0xFFFF;
    }

    // not the key size, zero for invalid
    public static int size(long key) {
        return memGetInt(key + 4) >>> 16;
    }

    public static int data(long key, int index) {
        return memGetInt(key + ((index + 2L) << 2));
    }

    public static boolean isValid(long key) {
        // check domain_and_size if not zero
        return key != NULL && memGetInt(key + 4) != 0;
    }

    // reset to invalid
    public static long reset(long key) {
        if (key != NULL) {
            // clear hash and domain_and_size to zero
            memPutLong(key, 0);
        }
        return key;
    }

    // copy src to dst, return dst
    public static long set(long dst, long src) {
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
