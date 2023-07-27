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

import java.util.*;

/**
 * Controlling the styled attributes for rendering Markdown.
 */
public final class MarkdownConfig {

    final Map<Class<? extends Node>, NodeVisitor<Node>> mVisitors;
    final BlockHandler mBlockHandler;

    private final Map<Class<? extends Node>, SpanFactory<Node>> mSpanFactories;

    private MarkdownConfig(Map<Class<? extends Node>, NodeVisitor<Node>> visitors,
                           BlockHandler blockHandler, Map<Class<? extends Node>, SpanFactory<Node>> spanFactories) {
        mVisitors = new HashMap<>(visitors);
        mBlockHandler = blockHandler;
        mSpanFactories = new HashMap<>(spanFactories);
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

        @NonNull
        public <N extends Node> Builder appendSpanFactory(@NonNull Class<? extends N> clazz,
                                                          @Nullable SpanFactory<N> factory) {
            SpanFactory<Node> one = mSpanFactories.get(clazz);
            if (one != null) {
                if (one instanceof CompositeSpanFactory<Node> f) {
                    f.mList.add((SpanFactory<Node>) factory);
                } else {
                    mSpanFactories.put(clazz, new CompositeSpanFactory<>(one, (SpanFactory<Node>) factory));
                }
            } else {
                mSpanFactories.put(clazz, (SpanFactory<Node>) factory);
            }
            return this;
        }

        public MarkdownConfig build() {
            BlockHandler blockHandler = mBlockHandler;
            if(blockHandler == null) {
                blockHandler = new DefaultBlockHandler();
            }
            return new MarkdownConfig(mVisitors, blockHandler, mSpanFactories);
        }

        static class CompositeSpanFactory<N extends Node> implements SpanFactory<N> {

            final ArrayList<SpanFactory<N>> mList = new ArrayList<>();

            public CompositeSpanFactory(SpanFactory<N> first, SpanFactory<N> second) {
                mList.add(first);
                mList.add(second);
            }

            @Override
            public Object create(@NonNull MarkdownConfig config, @NonNull N node) {
                int n = mList.size();
                Object[] spans = new Object[n];
                for (int i = 0; i < n; i++) {
                    spans[i] = mList.get(i).create(config, node);
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
