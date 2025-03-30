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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.task.Task;
import icyllis.arc3d.engine.task.TaskList;
import icyllis.arc3d.granite.*;
import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.sketch.Image;
import icyllis.arc3d.sketch.Surface;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jetbrains.annotations.ApiStatus;

/**
 * This class is a public API, except where noted.
 */
//TODO make non-sealed, and add GraniteContext
public final class RecordingContext extends Context {

    private final ImageProxyCache mImageProxyCache;
    private RenderTaskManager mRenderTaskManager;
    private DrawBufferManager mDrawBufferManager;
    private UploadBufferManager mUploadBufferManager;

    private final TaskList mRootTaskList;

    private final AtlasProvider mAtlasProvider;

    private final DrawAtlas.AtlasTokenTracker mAtlasTokenTracker;
    private final GlyphStrikeCache mGlyphStrikeCache;
    private final TextBlobCache mTextBlobCache;

    private final ReferenceArrayList<@SharedPtr GraniteDevice> mTrackedDevices =
            new ReferenceArrayList<>();

    protected RecordingContext(Device device) {
        super(device);
        mImageProxyCache = new ImageProxyCache(this);

        mRootTaskList = new TaskList();

        mAtlasProvider = new AtlasProvider(this);

        mAtlasTokenTracker = new DrawAtlas.AtlasTokenTracker();
        mGlyphStrikeCache = new GlyphStrikeCache();
        mTextBlobCache = new TextBlobCache();
    }

    @Override
    public void freeGpuResources() {
        checkOwnerThread();
        mAtlasProvider.freeGpuResources();
        mResourceProvider.freeGpuResources();
        // This is technically not GPU memory, but there's no other place for the client to tell us to
        // clean this up, and without any cleanup it can grow unbounded.
        mGlyphStrikeCache.clear();
    }

    @Override
    public void performDeferredCleanup(long msNotUsed) {
        checkOwnerThread();
        long timeMillis = System.currentTimeMillis() - msNotUsed;
        mResourceProvider.purgeResourcesNotUsedSince(timeMillis);
    }

    public void clearStrikeCache() {
        mGlyphStrikeCache.clear();
    }

    /**
     * Reports whether the {@link ImmediateContext} associated with this {@link RecordingContext}
     * is discarded. When called on a {@link ImmediateContext} it may actively check whether the
     * underlying 3D API device/context has been disconnected before reporting the status. If so,
     * calling this method will transition the {@link ImmediateContext} to the discarded state.
     */
    public boolean isDiscarded() {
        return mDevice.isDiscarded();
    }

    /**
     * Can a {@link Image} be created with the given color type.
     *
     * @param colorType see {@link ColorInfo}
     */
    public final boolean isImageCompatible(int colorType) {
        return getDefaultBackendFormat(colorType, false) != null;
    }

    /**
     * Can a {@link Surface} be created with the given color type.
     * To check whether MSAA is supported use {@link #getMaxSurfaceSampleCount(int)}.
     *
     * @param colorType see {@link ColorInfo}
     */
    public final boolean isSurfaceCompatible(int colorType) {
        colorType = Engine.colorTypeToPublic(colorType);
        if (ColorInfo.CT_RG_1616 == colorType ||
                ColorInfo.CT_A16_UNORM == colorType ||
                ColorInfo.CT_A16_FLOAT == colorType ||
                ColorInfo.CT_RG_F16 == colorType ||
                ColorInfo.CT_R16G16B16A16_UNORM == colorType ||
                ColorInfo.CT_GRAY_8 == colorType) {
            return false;
        }

        return getMaxSurfaceSampleCount(colorType) > 0;
    }

    @ApiStatus.Internal
    public final ImageProxyCache getSurfaceProvider() {
        return mImageProxyCache;
    }

    @ApiStatus.Internal
    public final RenderTaskManager getRenderTaskManager() {
        return mRenderTaskManager;
    }

    /*@ApiStatus.Internal
    public final ThreadSafeCache getThreadSafeCache() {
        return mDevice.getThreadSafeCache();
    }*/

    @ApiStatus.Internal
    public final DrawBufferManager getDynamicBufferManager() {
        return mDrawBufferManager;
    }

    @ApiStatus.Internal
    public UploadBufferManager getUploadBufferManager() {
        return mUploadBufferManager;
    }

    @ApiStatus.Internal
    public AtlasProvider getAtlasProvider() {
        return mAtlasProvider;
    }

    @ApiStatus.Internal
    public DrawAtlas.AtlasTokenTracker getAtlasTokenTracker() {
        return mAtlasTokenTracker;
    }

    @ApiStatus.Internal
    public GlyphStrikeCache getGlyphStrikeCache() {
        return mGlyphStrikeCache;
    }

    @ApiStatus.Internal
    public TextBlobCache getTextBlobCache() {
        return mTextBlobCache;
    }

    public void trackDevice(@SharedPtr GraniteDevice device) {
        assert isOwnerThread();
        assert device != null;
        // By taking a ref on tracked devices, the Recorder prevents the Device from being deleted on
        // another thread unless the Recorder has been destroyed or the device has abandoned its
        // recorder (e.g. was marked immutable).
        mTrackedDevices.add(device); // move
    }

    public void untrackDevice(@RawPtr GraniteDevice device) {
        assert isOwnerThread();
        int index = mTrackedDevices.indexOf(device);
        if (index >= 0) {
            // Don't modify the list structure of mTrackedDevices within this loop
            RefCnt.move(mTrackedDevices.set(index, null));
        }
    }

    public void addTask(@SharedPtr Task task) {
        mRootTaskList.appendTask(task);
    }

    public RootTask snap() {
        assert isOwnerThread();
        flushTrackedDevices();

        // In both the "task failed" case and the "everything is discarded" case, there's no work that
        // needs to be done in insertRecording(). However, we use nullptr as a failure signal, so
        // kDiscard will return a non-null Recording that has no tasks in it.
        if (mDrawBufferManager.hasMappingFailed() ||
                mRootTaskList.prepare(this) == Task.RESULT_FAILURE) {
            // Leaving 'mTrackedDevices' alone since they were flushed earlier and could still be
            // attached to extant Surfaces.
            mAtlasProvider.invalidateAtlases();
            mRootTaskList.clear();
            return null;
        }

        var extraResourceRefs = new ObjectArrayList<@SharedPtr Resource>();
        var finalTaskList = new TaskList();
        mDrawBufferManager.flush(finalTaskList, extraResourceRefs);
        mUploadBufferManager.flush(extraResourceRefs);
        finalTaskList.appendTasks(mRootTaskList);
        var recording = new RootTask(finalTaskList, extraResourceRefs);
        mRootTaskList.clear();
        return recording;
    }

    @ApiStatus.Internal
    public void flushTrackedDevices() {
        assert isOwnerThread();

        int index = -1;
        while (index < mTrackedDevices.size() - 1) {
            index++;
            // Entries may be set to null from a call to untrackDevice(), which will be cleaned up
            // along with any immutable or uniquely held Devices once everything is flushed.
            @RawPtr
            GraniteDevice device = mTrackedDevices.get(index);
            if (device != null) {
                device.flushPendingWork();
            }
        }

        // Issue next upload flush token. This is only used by the atlasing code which
        // always uses this method. Calling in Device.flushPendingWork() may
        // miss parent device flushes, increment too often, and lead to atlas corruption.
        mAtlasTokenTracker.issueFlushToken();

        var it = mTrackedDevices.listIterator();
        while (it.hasNext()) {
            @RawPtr
            GraniteDevice device = it.next();
            if (device == null || device.getRecordingContext() == null || device.unique()) {
                if (device != null) {
                    device.discardRC();
                    device.unref();
                }
                it.remove();
            }
        }
    }

    public boolean init(RecordingContextOptions options) {
        if (!super.init(options)) {
            return false;
        }
        if (mRenderTaskManager != null) {
            mRenderTaskManager.destroy();
        }
        mRenderTaskManager = new RenderTaskManager(this);
        mDrawBufferManager = new DrawBufferManager(getCaps(), getResourceProvider());
        mUploadBufferManager = new UploadBufferManager(getResourceProvider());
        return true;
    }

    protected void discard() {
        if (mDevice.discard() && mRenderTaskManager != null) {
            throw new AssertionError();
        }
        if (mRenderTaskManager != null) {
            mRenderTaskManager.destroy();
        }
        mRenderTaskManager = null;
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        if (mRenderTaskManager != null) {
            mRenderTaskManager.destroy();
        }
        mRenderTaskManager = null;

        mAtlasProvider.close();

        mRootTaskList.close();
    }
}
