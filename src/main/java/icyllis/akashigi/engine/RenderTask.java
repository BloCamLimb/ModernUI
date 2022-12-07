/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine;

import icyllis.akashigi.core.RefCnt;
import icyllis.akashigi.core.SharedPtr;
import icyllis.akashigi.engine.ops.OpsTask;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class abstracts a task that targets a single {@link SurfaceProxy}, participates in the
 * {@link DrawingManager}'s DAG, and implements the {@link #execute(OpFlushState)} method to
 * modify its target proxy's contents. (e.g., an {@link OpsTask} that executes a command buffer,
 * a {@link TextureResolveTask} that regenerates mipmaps, etc.)
 */
public abstract class RenderTask extends RefCnt {

    private static final AtomicInteger sNextID = new AtomicInteger(1);

    /**
     * Generates a unique ID from the task pool. 0 is reserved.
     */
    private static int createUniqueID() {
        for (;;) {
            final int value = sNextID.get();
            final int newValue = value == -1 ? 1 : value + 1; // 0 is reserved
            if (sNextID.weakCompareAndSetVolatile(value, newValue)) {
                return value;
            }
        }
    }

    /**
     * Indicates "resolutions" that need to be done on a surface before its pixels can be accessed.
     * If both types of resolve are requested, the MSAA resolve will happen first.
     */
    public static final int
            RESOLVE_FLAG_MSAA = 0x1,    // Blit and resolve an internal MSAA render buffer into the texture.
            RESOLVE_FLAG_MIPMAPS = 0x2; // Regenerate all mipmap levels.

    protected static final int
            CLOSED_FLAG = 0x01,     // This task can't accept any more dependencies.
            DETACHED_FLAG = 0x02,   // This task is detached from its creating DrawingManager.
            SKIPPABLE_FLAG = 0x04,  // This task can be skipped.
            ATLAS_FLAG = 0x08,      // This task is texture atlas.
            IN_RESULT_FLAG = 0x10,  // Flag for topological sorting
            TEMP_MARK_FLAG = 0x20;  // Flag for topological sorting

    static final TopologicalSort.Adapter<RenderTask> SORT_ADAPTER = new TopologicalSort.Adapter<>() {
        @Override
        public void setIndex(@Nonnull RenderTask node, int index) {
            node.setIndex(index);
        }

        @Override
        public int getIndex(@Nonnull RenderTask node) {
            return node.getIndex();
        }

        @Override
        public void setTempMarked(@Nonnull RenderTask node, boolean marked) {
            if (marked) {
                node.mFlags |= TEMP_MARK_FLAG;
            } else {
                node.mFlags &= ~TEMP_MARK_FLAG;
            }
        }

        @Override
        public boolean isTempMarked(@Nonnull RenderTask node) {
            return (node.mFlags & TEMP_MARK_FLAG) != 0;
        }

        @Override
        public List<RenderTask> getIncomingEdges(@Nonnull RenderTask node) {
            return node.mDependencies;
        }
    };

    private final int mUniqueID;
    private int mFlags;

    // 'this' RenderTask relies on the output of the RenderTasks in 'mDependencies'
    final List<RenderTask> mDependencies = new ArrayList<>(1);
    // 'this' RenderTask's output is relied on by the RenderTasks in 'mDependents'
    final List<RenderTask> mDependents = new ArrayList<>(1);
    // for performance reasons, we reuse one single resolve task for each render task
    private TextureResolveTask mTextureResolveTask;

    // multiple targets for texture resolve task
    @SharedPtr
    protected final List<SurfaceProxy> mTargets = new ArrayList<>(1);
    protected DrawingManager mDrawingMgr;

    /**
     * @param drawingMgr the creating drawing manager
     */
    protected RenderTask(@Nonnull DrawingManager drawingMgr) {
        mDrawingMgr = drawingMgr;
        mUniqueID = createUniqueID();
    }

    public final int getUniqueID() {
        return mUniqueID;
    }

    public final int getNumTargets() {
        return mTargets.size();
    }

    public final SurfaceProxy getTarget(int index) {
        return mTargets.get(index);
    }

    public final SurfaceProxy getTarget() {
        assert getNumTargets() == 1;
        return mTargets.get(0);
    }

    private void setIndex(int index) {
        assert (mFlags & IN_RESULT_FLAG) == 0;
        assert (index >= 0 && index < (1 << 26));
        mFlags |= (index << 6) | IN_RESULT_FLAG;
    }

    private int getIndex() {
        if ((mFlags & IN_RESULT_FLAG) != 0) {
            return mFlags >>> 6;
        }
        return -1;
    }

    protected final void addTarget(@SharedPtr SurfaceProxy proxy) {
        assert (mDrawingMgr.getContext().isOwnerThread());
        assert (!isClosed());
        mDrawingMgr.setLastRenderTask(proxy, this);
        proxy.isUsedAsTaskTarget();
        mTargets.add(proxy);
    }

    @Override
    protected void dispose() {
        mTargets.forEach(RefCnt::unref);
        mTargets.clear();
        assert (mFlags & DETACHED_FLAG) != 0;
    }

    public void gatherProxyIntervals(ResourceAllocator alloc) {
        // Default implementation is no proxies
    }

    /**
     * This method will be invoked at record-time to create pipeline information
     * and pre-bake input data on a background thread.
     */
    public void prePrepare(RecordingContext context) {
        // OpsTask and DeferredListTask override this
    }

    /**
     * This method will be invoked at flush-time to create pipeline information
     * and build input buffers.
     */
    public void prepare(OpFlushState flushState) {
        // OpsTask and DeferredListTask override this
    }

    /**
     * This method will be invoked at flush-time to execute commands.
     */
    public abstract boolean execute(OpFlushState flushState);

    public final void makeClosed(RecordingContext context) {
        if (isClosed()) {
            return;
        }
        assert (mDrawingMgr.getContext().isOwnerThread());

        onMakeClosed(context);

        if (mTextureResolveTask != null) {
            addDependency(mTextureResolveTask);
            mTextureResolveTask.makeClosed(context);
            mTextureResolveTask = null;
        }

        mFlags |= CLOSED_FLAG;
    }

    protected void onMakeClosed(RecordingContext context) {
    }

    public final boolean isClosed() {
        return (mFlags & CLOSED_FLAG) != 0;
    }

    /**
     * This method "detaches" all the SurfaceProxies this RenderTask modifies. In
     * practice this just means telling the drawing manager to forget the relevant
     * mappings from surface proxy to last modifying render task.
     */
    public final void detach(DrawingManager drawingMgr) {
        assert (isClosed());
        assert (mDrawingMgr == drawingMgr);
        if ((mFlags & DETACHED_FLAG) != 0) {
            return;
        }
        assert (drawingMgr.getContext().isOwnerThread());
        mDrawingMgr = null;
        mFlags |= DETACHED_FLAG;

        for (SurfaceProxy target : mTargets) {
            if (drawingMgr.getLastRenderTask(target) == this) {
                drawingMgr.setLastRenderTask(target, null);
            }
        }
    }

    /**
     * Make this task skippable. This must be used purely for optimization purposes
     * at this point as not all tasks will actually skip their work. It would be better if we could
     * detect tasks that can be skipped automatically. We'd need to support minimal flushes (i.e.,
     * only flush that which is required for SkSurfaces/SkImages) and the ability to detect
     * "orphaned tasks" and clean them out from the DAG so they don't indefinitely accumulate.
     * Finally, we'd probably have to track whether a proxy's backing store was imported or ever
     * exported to the client in case the client is doing direct reads outside of Skia and thus
     * may require tasks targeting the proxy to execute even if our DAG contains no reads.
     */
    public final void makeSkippable() {
        assert (isClosed());
        if (!isSkippable()) {
            assert (mDrawingMgr.getContext().isOwnerThread());
            mFlags |= SKIPPABLE_FLAG;
            onMakeSkippable();
        }
    }

    protected void onMakeSkippable() {
    }

    public final boolean isSkippable() {
        return (mFlags & SKIPPABLE_FLAG) != 0;
    }

    public final void addDependency(TextureProxy dependency, int samplerState) {
        assert (mDrawingMgr.getContext().isOwnerThread());
        assert (!isClosed());

        RenderTask dependencyTask = mDrawingMgr.getLastRenderTask(dependency);

        if (dependencyTask == this) {
            // no self dependency
            return;
        }

        if (dependencyTask != null) {
            if (dependsOn(dependencyTask) || mTextureResolveTask == dependencyTask) {
                // don't add duplicate dependencies
                return;
            }

            if ((dependencyTask.mFlags & ATLAS_FLAG) == 0) {
                // We are closing 'dependencyTask' here bc the current contents of it are what 'this'
                // renderTask depends on. We need a break in 'dependencyTask' so that the usage of
                // that state has a chance to execute.
                dependencyTask.makeClosed(mDrawingMgr.getContext());
            }
        }

        int resolveFlags = 0;

        if (dependency.isManualMSAAResolve() && dependency.isMSAADirty()) {
            resolveFlags |= RESOLVE_FLAG_MSAA;
        }

        if (SamplerState.isMipmapped(samplerState) &&
                dependency.isMipmapped() && dependency.isMipmapsDirty()) {
            resolveFlags |= RESOLVE_FLAG_MIPMAPS;
        }

        if (resolveFlags != 0) {
            if (mTextureResolveTask == null) {
                mTextureResolveTask = new TextureResolveTask(mDrawingMgr);
            }
            mTextureResolveTask.addProxy(RefCnt.create(dependency), resolveFlags);

            // addProxy() should have closed the texture proxy's previous task.
            assert (dependencyTask == null ||
                    dependencyTask.isClosed());
            assert (mDrawingMgr.getLastRenderTask(dependency) == mTextureResolveTask);

            assert (dependencyTask == null ||
                    mTextureResolveTask.dependsOn(dependencyTask));

            assert (!dependency.isManualMSAAResolve() ||
                    !dependency.isMSAADirty());
            assert (!dependency.isMipmapped() ||
                    !dependency.isMipmapsDirty());
            return;
        }

        if (dependencyTask != null) {
            addDependency(dependencyTask);
        }
    }

    public final boolean dependsOn(RenderTask dependency) {
        for (RenderTask task : mDependencies) {
            if (task == dependency) {
                return true;
            }
        }
        return false;
    }

    public final boolean isInstantiated() {
        for (SurfaceProxy proxy : mTargets) {
            if (!proxy.isInstantiated()) {
                return false;
            }
            Texture texture = proxy.peekTexture();
            if (texture != null && texture.isDestroyed()) {
                return false;
            }
        }
        return true;
    }

    void addDependency(RenderTask dependency) {
        assert (!dependency.dependsOn(this));  // loops are bad
        assert (!this.dependsOn(dependency));  // caller should weed out duplicates

        mDependencies.add(dependency);
        dependency.addDependent(this);
    }

    void addDependent(RenderTask dependent) {
        mDependents.add(dependent);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        int numTargets = getNumTargets();
        if (numTargets > 0) {
            out.append("Targets: \n");
            for (int i = 0; i < numTargets; i++) {
                out.append(getTarget(i));
                out.append("\n");
            }
        }
        out.append("Dependencies (").append(mDependencies.size()).append("): ");
        for (RenderTask task : mDependencies) {
            out.append(task.mUniqueID).append(", ");
        }
        out.append("\n");
        out.append("Dependents (").append(mDependents.size()).append("): ");
        for (RenderTask task : mDependents) {
            out.append(task.mUniqueID).append(", ");
        }
        out.append("\n");
        return out.toString();
    }
}
