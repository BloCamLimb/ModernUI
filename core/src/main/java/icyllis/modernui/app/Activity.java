/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.app;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.ContextWrapper;
import icyllis.modernui.view.View;
import icyllis.modernui.view.WindowManager;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.ToastManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

/**
 * Reserved for future use.
 */
@ApiStatus.Experimental
public class Activity extends ContextWrapper {

    //private icyllis.modernui.app.MainThread mMainThread;

    private WindowStage mWindowStage;
    private ToastManager mToastManager;

    private View mDecor;
    private boolean mWindowAdded = false;

    boolean mCalled;

    public Activity() {
        super(null);
    }

    final void attach(Context context, WindowStage windowStage) {
        attachBaseContext(context);
        mWindowStage = windowStage;
        mToastManager = new ToastManager(context, mWindowStage);
    }

    public void setDecorView(@NonNull View view) {
        if (mDecor == view) {
            return;
        }
        mDecor = view;
        if (mWindowAdded) {
            //TODO ensure all popup windows are removed, then re-add app window
            throw new IllegalStateException("Window is already added");
        }
    }

    @NonNull
    public final View getDecorView() {
        if (mDecor == null) {
            mDecor = new FrameLayout(this);
        }
        return mDecor;
    }

    @UiThread
    @MustBeInvokedByOverriders
    protected void onCreate() {
        mCalled = true;
    }

    @UiThread
    @MustBeInvokedByOverriders
    protected void onStart() {
        mCalled = true;
    }

    final void makeVisible() {
        if (!mWindowAdded) {
            var decor = getDecorView();
            mWindowStage.addWindow(AppViewRoot::new,
                    decor, new WindowManager.LayoutParams());
            mWindowStage.show();
            mWindowAdded = true;
        }
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        if (WINDOW_SERVICE.equals(name)) {
            return mWindowStage;
        }
        if (TOAST_SERVICE.equals(name)) {
            return mToastManager;
        }
        return super.getSystemService(name);
    }

    final void performCreate() {
        mCalled = false;
        onCreate();
        if (!mCalled) {
            throw new IllegalStateException("Activity " + this +
                    " did not call through to super.onCreate()");
        }
    }

    final void performStart() {
        mCalled = false;
        onStart();
        if (!mCalled) {
            throw new IllegalStateException("Activity " + this +
                    " did not call through to super.onStart()");
        }
    }
}
