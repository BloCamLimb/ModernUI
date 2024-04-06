/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.RefCounted;
import icyllis.arc3d.core.SharedPtr;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Interface for operating GPU resources that can be kept in the
 * {@link ResourceCache}. Such resources will have a device memory allocation.
 * Possible implementations:
 * <ul>
 *   <li>GLBuffer</li>
 *   <li>GLTexture</li>
 *   <li>GLRenderbuffer</li>
 *   <li>VkBuffer</li>
 *   <li>VkImage</li>
 * </ul>
 * Specially, cacheable framebuffer objects are also implementations of this class,
 * they can be used as ping-pong buffers.
 * <p>
 * Register resources into the cache to track their GPU memory usage. Since all
 * Java objects will always be strong referenced, an explicit {@link #ref()} and
 * {@link #unref()} is required to determine if they need to be recycled or not.
 * When used as smart pointers, they need to be annotated as {@link SharedPtr},
 * otherwise they tend to be used as raw pointers (no ref/unref calls should be
 * made). A paired {@link UniqueID} object can be used as unique identifiers.
 * <p>
 * Each {@link GpuResource} should be created with immutable GPU memory allocation,
 * one exception is streaming buffers, which allocates a ring buffer and its offset
 * will alter. {@link GpuResource} can be only created/recycled on the render thread.
 * Use {@link ResourceProvider} to obtain {@link GpuResource} objects.
 */
@NotThreadSafe
public sealed interface GpuResource extends RefCounted permits GpuResourceBase, GpuSurface {

    /**
     * @return true if this resource is uniquely referenced by the client pipeline
     */
    boolean unique();

    /**
     * Increases the reference count by 1 on the client pipeline.
     * It's an error to call this method if the reference count has already reached zero.
     */
    void ref();

    /**
     * Decreases the reference count by 1 on the client pipeline.
     * It's an error to call this method if the reference count has already reached zero.
     */
    void unref();

    /**
     * Increases the usage count by 1 on the tracked backend pipeline.
     * <p>
     * This is designed to be used by Resources that need to track when they are in use on
     * backend (usually via a command buffer) separately from tracking if there are any current logical
     * usages in client. This allows for a scratch Resource to be reused for new draw calls even
     * if it is in use on the backend.
     */
    void addCommandBufferUsage();

    /**
     * Decreases the usage count by 1 on the tracked backend pipeline.
     * It's an error to call this method if the usage count has already reached zero.
     */
    void removeCommandBufferUsage();

    /**
     * Checks whether an object has been released or discarded. All objects will
     * be in this state after their creating Context is destroyed or has
     * contextLost called. It's up to the client to test isDestroyed() before
     * attempting to use an object if it holds refs on objects across
     * Context.close(), freeResources with the force flag, or contextLost.
     *
     * @return true if the object has been released or discarded, false otherwise.
     */
    boolean isDestroyed();

    /**
     * Retrieves the context that owns the object. Note that it is possible for
     * this to return null. When objects have been release()ed or discard()ed
     * they no longer have an owning context. Destroying a DirectContext
     * automatically releases all its resources.
     */
    @Nullable
    DirectContext getContext();

    /**
     * Retrieves the amount of GPU memory used by this resource in bytes. It is
     * approximate since we aren't aware of additional padding or copies made
     * by the driver.
     * <p>
     * <b>NOTE: The return value must be constant in this object.</b>
     *
     * @return the amount of GPU memory used in bytes
     */
    long getMemorySize();

    /**
     * Returns the current unique key for the resource. It will be invalid if the resource has no
     * associated unique key.
     */
    @Nullable
    IUniqueKey getUniqueKey();

    /**
     * Gets a tag that is unique for this Resource object. It is static in that it does
     * not change when the content of the Resource object changes. It has identity and
     * never hold a reference to this Resource object, so it can be used to track state
     * changes through '=='.
     */
    @Nonnull
    UniqueID getUniqueID();

    /**
     * @return the label for the resource, or empty
     */
    @Nonnull
    String getLabel();

    /**
     * Sets a label for the resource for debugging purposes.
     *
     * @param label the new label to set, or empty to clear
     */
    void setLabel(String label);

    /**
     * Sets a unique key for the resource. If the resource was previously cached as scratch it will
     * be converted to a uniquely-keyed resource. If the key is invalid then this is equivalent to
     * removeUniqueKey(). If another resource is using the key then its unique key is removed and
     * this resource takes over the key.
     */
    @ApiStatus.Internal
    void setUniqueKey(IUniqueKey key);

    /**
     * Removes the unique key from a resource. If the resource has a scratch key, it may be
     * preserved for recycling as scratch.
     */
    @ApiStatus.Internal
    void removeUniqueKey();

    /**
     * Budgeted: If the resource is uncached make it cached. Has no effect on resources that
     * are wrapped or already cached.
     * <p>
     * Not Budgeted: If the resource is cached make it uncached. Has no effect on resources that
     * are wrapped or already uncached. Furthermore, resources with unique keys cannot be made
     * not budgeted.
     */
    @ApiStatus.Internal
    void makeBudgeted(boolean budgeted);

    /**
     * Get the resource's budget type which indicates whether it counts against the resource cache
     * budget and if not whether it is allowed to be cached.
     */
    @ApiStatus.Internal
    int getBudgetType();

    /**
     * Is the resource object wrapping an externally allocated GPU resource?
     */
    @ApiStatus.Internal
    boolean isWrapped();

    /**
     * If this resource can be used as a scratch resource this returns a valid scratch key.
     * Otherwise, it returns a key for which isNullScratch is true. The resource may currently be
     * used as a uniquely keyed resource rather than scratch. Check isScratch().
     */
    @ApiStatus.Internal
    @Nullable
    IScratchKey getScratchKey();

    /**
     * If the resource has a scratch key, the key will be removed. Since scratch keys are installed
     * at resource creation time, this means the resource will never again be used as scratch.
     */
    @ApiStatus.Internal
    void removeScratchKey();

    @ApiStatus.Internal
    boolean isFree();

    @ApiStatus.Internal
    boolean hasRefOrCommandBufferUsage();

    /**
     * An object with identity.
     */
    final class UniqueID {

        @Nonnull
        String mLabel = "";

        public UniqueID() {
        }

        @Nonnull
        @Override
        public String toString() {
            return "GpuResource.UniqueID@" + Integer.toHexString(hashCode()) + "{" + mLabel + "}";
        }
    }
}
