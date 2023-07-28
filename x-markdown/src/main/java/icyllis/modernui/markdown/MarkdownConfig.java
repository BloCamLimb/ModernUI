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

import com.vladsch.flexmark.util.ast.Node;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.util.DataSet;

import java.util.*;

/**
 * Controlling the styled attributes for rendering Markdown.
 */
public final class MarkdownConfig {

    final MarkdownTheme mTheme;
    final Map<Class<? extends Node>, NodeVisitor<Node>> mVisitors;
    final BlockHandler mBlockHandler;

    private final Map<Class<? extends Node>, SpanFactory<Node>> mSpanFactories;

    private MarkdownConfig(MarkdownTheme theme, Map<Class<? extends Node>, NodeVisitor<Node>> visitors,
                           BlockHandler blockHandler, Map<Class<? extends Node>, SpanFactory<Node>> spanFactories) {
        mTheme = theme;
        mVisitors = new HashMap<>(visitors);
        mBlockHandler = blockHandler;
        mSpanFactories = new HashMap<>(spanFactories);
    }

    public MarkdownTheme theme() {
        return mTheme;
    }

    SpanFactory<Node> getSpanFactory(Class<? extends Node> clazz) {
        return mSpanFactories.get(clazz);
    }

    public static final class Builder {

        private final HashMap<Class<? extends Node>, NodeVisitor<Node>> mVisitors =
                new HashMap<>();

        private final Map<Class<? extends Node>, SpanFactory<Node>> mSpanFactories =
                new HashMap<>();

        private BlockHandler mBlockHandler;

        @SuppressWarnings("unchecked")
        public <N extends Node> Builder addVisitor(@NonNull Class<N> clazz,
                                                   @NonNull NodeVisitor<N> visitor) {
            mVisitors.put(clazz, (NodeVisitor<Node>) visitor);
            return this;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public <N extends Node> Builder appendSpanFactory(@NonNull Class<? extends N> clazz,
                                                          @Nullable SpanFactory<N> factory) {
            SpanFactory<Node> oldFactory = mSpanFactories.get(clazz);
            SpanFactory<Node> newFactory = (SpanFactory<Node>) factory;
            if (oldFactory != null) {
                if (oldFactory instanceof CompositeSpanFactory<Node> list) {
                    list.add(newFactory);
                } else {
                    mSpanFactories.put(clazz, new CompositeSpanFactory<>(oldFactory, newFactory));
                }
            } else {
                mSpanFactories.put(clazz, newFactory);
            }
            return this;
        }

        @NonNull
        public MarkdownConfig build(MarkdownTheme theme) {
            BlockHandler blockHandler = mBlockHandler;
            if (blockHandler == null) {
                blockHandler = new DefaultBlockHandler();
            }
            return new MarkdownConfig(theme, mVisitors, blockHandler, mSpanFactories);
        }

        static class CompositeSpanFactory<N extends Node>
                extends ArrayList<SpanFactory<N>>
                implements SpanFactory<N> {

            public CompositeSpanFactory(SpanFactory<N> first, SpanFactory<N> second) {
                add(first);
                add(second);
            }

            @Override
            public Object create(@NonNull MarkdownConfig config, @NonNull N node, @NonNull DataSet args) {
                int n = size();
                Object[] spans = new Object[n];
                for (int i = 0; i < n; i++) {
                    spans[i] = get(i).create(config, node, args);
                }
                return spans;
            }
        }

        static class DefaultBlockHandler implements BlockHandler {

            @Override
            public void blockStart(@NonNull MarkdownVisitor visitor, @NonNull Node node) {
                visitor.ensureNewLine();
            }

            @Override
            public void blockEnd(@NonNull MarkdownVisitor visitor, @NonNull Node node) {
                if (visitor.hasNext(node)) {
                    visitor.ensureNewLine();
                }
            }
        }
    }
}
