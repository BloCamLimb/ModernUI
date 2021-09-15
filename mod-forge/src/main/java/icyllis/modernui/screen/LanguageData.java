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

package icyllis.modernui.screen;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@Deprecated
public class LanguageData {

    private static LanguageData sInstance;

    private Object2ObjectMap<String, String> mData;

    private boolean mDefaultRTL;

    public LanguageData() {
        LazyOptional<Object> lazyOptional = LazyOptional.empty();
        //noinspection ConstantConditions
        Object o = lazyOptional.orElse(null);
        if (o != null) {

        }
    }

    public LanguageData(Map<String, String> data, boolean defaultRTL) {
        mData = new Object2ObjectAVLTreeMap<>(data);
        mDefaultRTL = defaultRTL;
    }

    @Nonnull
    public static LanguageData getInstance() {
        return sInstance;
    }

    public static void setInstance(@Nonnull LanguageData instance) {
        sInstance = instance;
    }

    @Nullable
    public String get(@Nonnull String key) {
        return mData.get(key);
    }

    @Nonnull
    public String getOrDefault(@Nonnull String key) {
        String ret = mData.get(key);
        return ret == null ? key : ret;
    }

    public boolean isDefaultRTL() {
        return mDefaultRTL;
    }
}
