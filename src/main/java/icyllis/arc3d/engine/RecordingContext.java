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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.ApiStatus;

/**
 * This class is a public API, except where noted.
 */
public final class RecordingContext extends Context {

    private final ImageProxyCache mImageProxyCache;
    private RenderTaskManager mRenderTaskManager;
    private DynamicBufferManager mDynamicBufferManager;
    private UploadBufferManager mUploadBufferManager;

    private final TaskList mRootTaskList;

    private final AtlasProvider mAtlasProvider;

    private final DrawAtlas.AtlasTokenTracker mAtlasTokenTracker;
    private final GlyphStrikeCache mGlyphStrikeCache;

    protected RecordingContext(Device device) {
        super(device);
        mImageProxyCache = new ImageProxyCache(this);

        mRootTaskList = new TaskList();

        mAtlasProvider = new AtlasProvider(this);

        mAtlasTokenTracker = new DrawAtlas.AtlasTokenTracker();
        mGlyphStrikeCache = new GlyphStrikeCache();
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
     * Can a {@link icyllis.arc3d.core.Image} be created with the given color type.
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
    public final DynamicBufferManager getDynamicBufferManager() {
        return mDynamicBufferManager;
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

    public void addTask(@SharedPtr Task task) {
        mRootTaskList.appendTask(task);
    }

    public RootTask snap() {
        if (mDynamicBufferManager.hasMappingFailed() ||
                mRootTaskList.prepare(this) == Task.RESULT_FAILURE) {
            mRootTaskList.clear();
            return null;
        }

        var extraResourceRefs = new ObjectArrayList<@SharedPtr Resource>();
        var finalTaskList = new TaskList();
        mDynamicBufferManager.flush(finalTaskList, extraResourceRefs);
        mUploadBufferManager.flush(extraResourceRefs);
        finalTaskList.appendTasks(mRootTaskList);
        var recording = new RootTask(finalTaskList, extraResourceRefs);
        mRootTaskList.clear();
        return recording;
    }

    @Override
    public boolean init() {
        if (!super.init()) {
            return false;
        }
        if (mRenderTaskManager != null) {
            mRenderTaskManager.destroy();
        }
        mRenderTaskManager = new RenderTaskManager(this);
        mDynamicBufferManager = new DynamicBufferManager(getCaps(), getResourceProvider());
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
