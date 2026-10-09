/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

import icyllis.modernui.core.Context;
import icyllis.modernui.view.Stage;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewRoot;
import icyllis.modernui.view.menu.ContextMenuBuilder;
import icyllis.modernui.view.menu.MenuHelper;
import org.jetbrains.annotations.ApiStatus;

/**
 * @hidden
 */
@ApiStatus.Internal
public class AppViewRoot extends ViewRoot {

    private final Context mContext;

    public AppViewRoot(Context context, Stage stage) {
        super(context, stage);
        mContext = context;
    }

    ContextMenuBuilder mContextMenu;
    MenuHelper mContextMenuHelper;

    @Override
    public boolean showContextMenuForChild(View originalView, float x, float y) {
        if (mContextMenuHelper != null) {
            mContextMenuHelper.dismiss();
            mContextMenuHelper = null;
        }

        if (mContextMenu == null) {
            mContextMenu = new ContextMenuBuilder(mContext);
            //mContextMenu.setCallback(callback);
        } else {
            mContextMenu.clearAll();
        }

        final MenuHelper helper;
        final boolean isPopup = !Float.isNaN(x) && !Float.isNaN(y);
        if (isPopup) {
            helper = mContextMenu.showPopup(mContext, originalView, x, y);
        } else {
            helper = mContextMenu.showPopup(mContext, originalView, 0, 0);
        }

        if (helper != null) {
            //helper.setPresenterCallback(callback);
        }

        mContextMenuHelper = helper;
        return helper != null;
    }
}
