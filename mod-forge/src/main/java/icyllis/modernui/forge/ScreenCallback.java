/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.forge;

import icyllis.modernui.annotation.MainThread;
import icyllis.modernui.core.ArchCore;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Callback for handling user interface lifecycle events.
 * <p>
 * Most methods are callbacks that will be invoked on UI thread. To initiate this on
 * the client side, call {@link #startLifecycle()} from client main thread.
 * In this case, this is served as a local interaction model, the server will not
 * intersect with this before. Otherwise, initiate this with a network model.
 *
 * @see OpenMenuEvent#setCallback(ScreenCallback)
 */
@OnlyIn(Dist.CLIENT)
public abstract class ScreenCallback {

    protected ScreenCallback() {
    }

    /**
     * Start the lifecycle of application user interface with this callback and create views.
     * This method must be called from client main thread.
     * <p>
     * This is served as a local interaction model, the server will not intersect with this before.
     * <p>
     * Note that this callback object can be restarted even it was dead. But in this case,
     * you need to pay special attention to the lifecycle, since it is not associated with this object.
     */
    @MainThread
    public final void startLifecycle() {
        ArchCore.checkMainThread();
        UIManager.sInstance.openGui(this);
    }

    protected abstract void onCreate();

    /**
     * Set the content view for the screen, this is the top-level view that users can add.
     * After this method is called, all child views of the root view are removed.
     * <p>
     * Note that the layout params match the root view (a container view), it must be a subclass
     * of FrameLayout, you can use {@link icyllis.modernui.widget.FrameLayout.LayoutParams} safely.
     *
     * @param view   content view
     * @param params layout params of content view
     */
    public void setContentView(@Nonnull View view, @Nonnull ViewGroup.LayoutParams params) {
        UIManager.sInstance.setContentView(view, params);
    }

    protected void onDestroy() {
    }
}
