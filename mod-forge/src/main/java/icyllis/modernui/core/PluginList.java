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

import icyllis.modernui.ModernUI;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginList {

    private static PluginList sInstance;

    private final List<Plugin> mPlugins = new ArrayList<>();

    private PluginList() {

    }

    private void scanPlugins() {
        final Map<String, Plugin> plugins = new HashMap<>();
        final Type target = Type.getType(DefinePlugin.class);
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData data : scanData.getAnnotations()) {
                if (data.getAnnotationType().equals(target)) {
                    try {
                        String pid = (String) data.getAnnotationData().get("value");
                        Plugin v = plugins.putIfAbsent(pid,
                                Class.forName(data.getMemberName()).asSubclass(Plugin.class)
                                        .getDeclaredConstructor().newInstance());
                        if (v != null) {
                            ModernUI.LOGGER.error(ModernUI.MARKER, "{} is annotated with the same plugin id {} as {}", data.getMemberName(), pid, v);
                        }
                    } catch (Throwable throwable) {
                        ModernUI.LOGGER.error(ModernUI.MARKER, "Failed to load plugin: {}", data.getMemberName(), throwable);
                    }
                }
            }
        }
        mPlugins.addAll(plugins.values());
    }

    @Nonnull
    public static PluginList get() {
        if (sInstance == null)
            synchronized (PluginList.class) {
                if (sInstance == null)
                    sInstance = new PluginList();
            }
        return sInstance;
    }

    public int size() {
        return mPlugins.size();
    }
}
