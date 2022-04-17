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

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A key used for scratch resources. There are three important rules about scratch keys:
 * <ul>
 * <li> Multiple resources can share the same scratch key. Therefore resources assigned the same
 * scratch key should be interchangeable with respect to the code that uses them.</li>
 * <li> A resource can have at most one scratch key and it is set at resource creation by the
 * resource itself.</li>
 * <li> When a scratch resource is referenced it will not be returned from the
 * cache for a subsequent cache request until all refs are released. This facilitates using
 * a scratch key for multiple render-to-texture scenarios. An example is a separable blur:</li>
 * </ul>
 * <pre>{@code
 *  public void makeBlurredImage() {
 *      var texture0 = get_scratch_texture(scratchKey);
 *      // texture 0 is already owned so we will get a different one for texture1
 *      var texture1 = get_scratch_texture(scratchKey);
 *      // draws path mask to texture 0
 *      draw_mask(texture0, path);
 *      // blurs texture 0 in y and stores result in texture 1
 *      blur_x(texture0, texture1);
 *      // blurs texture 1 in y and stores result in texture 0
 *      blur_y(texture1, texture0);
 *      // texture 1 can now be recycled for the next request with scratchKey
 *      texture1.unref();
 *      // consume the blurred texture, such as download to CPU
 *      consume_blur(texture0);
 *      // texture 0 can now be recycled for the next request with scratchKey
 *      texture0.unref();
 *  }
 * }</pre>
 */
public final class ScratchKey extends ResourceKey {

    // 0 is invalid
    private static final AtomicInteger sNextDomain = new AtomicInteger(1);

    /**
     * @return the next scratch key domain
     */
    public static int createDomain() {
        return sNextDomain.getAndIncrement();
    }

    private static final ScratchKey[] sPool = new ScratchKey[4];
    private static int sPoolSize;

    /**
     * Creates an invalid scratch key. It must be initialized before use.
     */
    public ScratchKey() {
    }

    public ScratchKey(ScratchKey key) {
        super(key);
    }

    /**
     * Internal use, returns a pooled key, may not be used for hash map as keys,
     * only used with lookup. Init it before lookup, recycle it after lookup.
     * <p>
     * Note: Unique keys cannot be pooled.
     *
     * @return a pooled, state-undefined key
     */
    @Nonnull
    public static ScratchKey obtain() {
        ScratchKey key;
        synchronized (sPool) {
            if (sPoolSize == 0)
                return new ScratchKey();
            key = sPool[--sPoolSize];
            // still hold strong reference
        }
        key.reset();
        return key;
    }

    public void set(ScratchKey key) {
        super.set(key);
    }

    /**
     * Internal use, recycle this to pool.
     */
    public void recycle() {
        synchronized (sPool) {
            if (sPoolSize == sPool.length)
                return;
            sPool[sPoolSize++] = this;
        }
    }

    /**
     * @return a copy of this key, used as hash map's static keys (state-immutable)
     */
    @Nonnull
    public ScratchKey copy() {
        return new ScratchKey(this);
    }
}
