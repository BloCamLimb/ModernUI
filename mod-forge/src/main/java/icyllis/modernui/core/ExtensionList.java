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

package icyllis.modernui.core;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ExtensionList {

    private static ExtensionList sInstance;

    private final List<Extension> mExtensions = new ArrayList<>();

    private ExtensionList() {

    }

    @Nonnull
    public static ExtensionList get() {
        if (sInstance == null)
            synchronized (ExtensionList.class) {
                if (sInstance == null)
                    sInstance = new ExtensionList();
            }
        return sInstance;
    }

    public int size() {
        return mExtensions.size();
    }
}
