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

package icyllis.modernui.graphics.texture;

import icyllis.modernui.core.Context;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TextureManager {

    private static final TextureManager INSTANCE = new TextureManager();

    private final Map<Context, Map<Path, Texture2D>> mTextureMap = new HashMap<>();

    private TextureManager() {
    }

    public static TextureManager getINSTANCE() {
        return INSTANCE;
    }

    public void reload() {
        mTextureMap.clear();
    }

    public Texture2D getOrCreate(Context context, Path path) {
        Texture2D texture = mTextureMap.computeIfAbsent(context, l -> new Object2ObjectRBTreeMap<>())
                .get(path);
        if (texture != null) {
            return texture;
        }
        return null;
    }
}
