/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.markdown;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.markdown.core.CorePlugin;

import java.util.*;
import java.util.function.Consumer;

public class MarkdownPluginRegistry implements Plugin.Registry {

    private final LinkedHashSet<Plugin> mGiven;
    private final HashSet<Plugin> mFound;
    private final HashSet<Plugin> mVisited;

    private final ArrayList<Plugin> mResults = new ArrayList<>();

    MarkdownPluginRegistry(LinkedHashSet<Plugin> given) {
        mGiven = given;
        mFound = new LinkedHashSet<>(given.size());
        mVisited = new HashSet<>();
    }

    ArrayList<Plugin> process() {
        for (Plugin plugin : mGiven) {
            visit(plugin);
        }
        return mResults;
    }

    @NonNull
    public <P extends Plugin> P require(@NonNull Class<P> clazz) {
        return get(clazz);
    }

    @Override
    public <P extends Plugin> void require(@NonNull Class<P> clazz, @NonNull Consumer<? super P> action) {
        action.accept(get(clazz));
    }

    private void visit(Plugin plugin) {
        if (!mFound.contains(plugin)) {
            if (!mVisited.add(plugin)) {
                throw new IllegalStateException("Cyclic dependency chain found: " + mVisited);
            }
            plugin.configure(this);
            mVisited.remove(plugin);
            if (mFound.add(plugin)) {
                if (plugin.getClass() == CorePlugin.class) {
                    mResults.add(0, plugin);
                } else {
                    mResults.add(plugin);
                }
            }
        }
    }

    @NonNull
    private <P extends Plugin> P get(@NonNull Class<? extends P> clazz) {
        P plugin = find(mFound, clazz);

        if (plugin == null) {
            plugin = find(mGiven, clazz);
            if (plugin == null) {
                throw new IllegalStateException("Requested plugin is not added: " +
                        "" + clazz.getName() + ", plugins: " + mGiven);
            }
            visit(plugin);
        }
        return plugin;
    }

    @Nullable
    private <P extends Plugin> P find(Set<Plugin> set, @NonNull Class<? extends P> clazz) {
        for (Plugin plugin : set) {
            if (clazz.isAssignableFrom(plugin.getClass())) {
                return (P) plugin;
            }
        }
        return null;
    }
}
