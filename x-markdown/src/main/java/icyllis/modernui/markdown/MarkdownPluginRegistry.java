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

public class MarkdownPluginRegistry {

    private final LinkedHashSet<MarkdownPlugin> mGiven;
    private final HashSet<MarkdownPlugin> mFound;
    private final HashSet<MarkdownPlugin> mVisited;

    private final ArrayList<MarkdownPlugin> mResults = new ArrayList<>();

    MarkdownPluginRegistry(LinkedHashSet<MarkdownPlugin> given) {
        mGiven = given;
        mFound = new LinkedHashSet<>(given.size());
        mVisited = new HashSet<>();
    }

    ArrayList<MarkdownPlugin> process() {
        for (MarkdownPlugin plugin : mGiven) {
            visit(plugin);
        }
        return mResults;
    }

    @NonNull
    public <P extends MarkdownPlugin> P require(@NonNull Class<? extends P> clazz) {
        return get(clazz);
    }

    private void visit(MarkdownPlugin plugin) {
        if (!mFound.contains(plugin)) {
            if (!mVisited.add(plugin)) {
                throw new IllegalStateException("Cyclic dependency chain found: " + mVisited);
            }
            plugin.register(this);
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
    private <P extends MarkdownPlugin> P get(@NonNull Class<? extends P> clazz) {
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
    private <P extends MarkdownPlugin> P find(Set<MarkdownPlugin> set, @NonNull Class<? extends P> clazz) {
        for (MarkdownPlugin plugin : set) {
            if (clazz.isAssignableFrom(plugin.getClass())) {
                return (P) plugin;
            }
        }
        return null;
    }
}
