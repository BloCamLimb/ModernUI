/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.fragment;

import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatcher for events to {@link FragmentLifecycleCallbacks} instances
 */
final class FragmentLifecycleCallbacksDispatcher {

    private static final class FragmentLifecycleCallbacksHolder {
        @Nonnull
        final FragmentLifecycleCallbacks mCallback;
        final boolean mRecursive;

        FragmentLifecycleCallbacksHolder(@Nonnull FragmentLifecycleCallbacks callback,
                                         boolean recursive) {
            mCallback = callback;
            mRecursive = recursive;
        }
    }

    @Nonnull
    private final CopyOnWriteArrayList<FragmentLifecycleCallbacksHolder>
            mLifecycleCallbacks = new CopyOnWriteArrayList<>();

    @Nonnull
    private final FragmentManager mFragmentManager;

    FragmentLifecycleCallbacksDispatcher(@Nonnull FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    /**
     * Registers a {@link FragmentLifecycleCallbacks} to listen to fragment
     * lifecycle events happening in this FragmentManager. All registered callbacks will be
     * automatically unregistered when this FragmentManager is destroyed.
     *
     * @param cb        Callbacks to register
     * @param recursive true to automatically register this callback for all child FragmentManagers
     */
    public void registerFragmentLifecycleCallbacks(@Nonnull FragmentLifecycleCallbacks cb,
                                                   boolean recursive) {
        mLifecycleCallbacks.add(new FragmentLifecycleCallbacksHolder(cb, recursive));
    }

    /**
     * Unregisters a previously registered {@link FragmentLifecycleCallbacks}.
     * If the callback was not previously registered this call has no effect. All registered
     * callbacks will be automatically unregistered when this FragmentManager is destroyed.
     *
     * @param cb Callbacks to unregister
     */
    public void unregisterFragmentLifecycleCallbacks(@Nonnull FragmentLifecycleCallbacks cb) {
        synchronized (mLifecycleCallbacks) {
            for (int i = 0, count = mLifecycleCallbacks.size(); i < count; i++) {
                if (mLifecycleCallbacks.get(i).mCallback == cb) {
                    mLifecycleCallbacks.remove(i);
                    break;
                }
            }
        }
    }

    void dispatchOnFragmentPreAttached(@Nonnull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentPreAttached(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentPreAttached(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentAttached(@Nonnull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentAttached(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentAttached(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentPreCreated(@Nonnull Fragment f,
                                      @Nullable DataSet savedInstanceState, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentPreCreated(f, savedInstanceState, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentPreCreated(
                        mFragmentManager, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentCreated(@Nonnull Fragment f,
                                   @Nullable DataSet savedInstanceState, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentCreated(f, savedInstanceState, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentCreated(
                        mFragmentManager, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentViewCreated(@Nonnull Fragment f, @Nonnull View v,
                                       @Nullable DataSet savedInstanceState, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentViewCreated(f, v, savedInstanceState, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentViewCreated(
                        mFragmentManager, f, v, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentStarted(@Nonnull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentStarted(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentStarted(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentResumed(@Nonnull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentResumed(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentResumed(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentPaused(@Nonnull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentPaused(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentPaused(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentStopped(@Nonnull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentStopped(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentStopped(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentSaveInstanceState(@Nonnull Fragment f, @Nonnull DataSet outState,
                                             boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentSaveInstanceState(f, outState, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentSaveInstanceState(
                        mFragmentManager, f, outState);
            }
        }
    }

    void dispatchOnFragmentViewDestroyed(@Nonnull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentViewDestroyed(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentViewDestroyed(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentDestroyed(@Nonnull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentDestroyed(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentDestroyed(mFragmentManager, f);
            }
        }
    }

    void dispatchOnFragmentDetached(@Nonnull Fragment f, boolean onlyRecursive) {
        Fragment parent = mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher()
                    .dispatchOnFragmentDetached(f, true);
        }
        for (FragmentLifecycleCallbacksHolder holder : mLifecycleCallbacks) {
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentDetached(mFragmentManager, f);
            }
        }
    }
}
