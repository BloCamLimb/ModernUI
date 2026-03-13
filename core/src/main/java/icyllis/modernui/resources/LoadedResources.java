/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.resources;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.resources.ResourceTypes.Res_value;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.function.BiFunction;

/**
 * Represents a read-only view to a ResourceTable.
 *
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public class LoadedResources {

    ResStringPool globalStringPool;

    ResStringPool typeStringPool;
    ResStringPool keyStringPool;

    BiFunction<?, ?, ?>[] factoryPool;

    LoadedPackage[] packages;

    final String[] packageLibs = new String[256];

    public LoadedPackage[] getPackages() {
        return packages;
    }

    /**
     * Returns a namespace that an integer id in this Table represents.
     * Or null if missing.
     *
     * @param id the res id to lookup
     * @return the namespace the id represents
     */
    @Nullable
    public String lookupPackageName(int id) {
        int packageId = id >>> Res_value.PACKAGE_ID_SHIFT;

        return packageLibs[packageId];
    }

    /**
     * Converts packed integer id to string-based resource id.
     * The integer id must come from this table.
     *
     * @param source the source res id that contains the res id to lookup
     * @param id     the res id to lookup, 0xppeeeeee
     * @param typeId type id if the res id is a reference, or 0 if they have the same type
     * @return the resource id the id represents
     */
    @Nullable
    public ResourceId lookupResourceId(ResourceId source, int id, int typeId) {
        String namespace = lookupPackageName(id);
        if (namespace == null) {
            return null;
        }
        String type;
        if (typeId == 0) {
            // 0 means self
            type = source.type();
        } else {
            type = typeStringPool.getStringAt(typeId - 1);
            if (type == null) {
                return null;
            }
        }
        String entry = keyStringPool.getStringAt(id & Res_value.KEY_INDEX_MASK);
        if (entry == null) {
            return null;
        }
        return new ResourceId(namespace, type, entry);
    }

    public ResStringPool getKeyStringPool() {
        return keyStringPool;
    }

    public ResStringPool getGlobalStringPool() {
        return globalStringPool;
    }

    public BiFunction<?,?,?> lookupFactory(int idx) {
        if (idx < 0 || idx >= factoryPool.length) {
            return null;
        }
        return factoryPool[idx];
    }
}
