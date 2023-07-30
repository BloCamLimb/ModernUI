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

import javax.annotation.concurrent.Immutable;
import java.util.*;

@Immutable
public final class MarkdownConfig {

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    final MarkdownTheme mTheme;
    final Map<Class<? extends Node>, NodeVisitor<Node>> mVisitors;
    @Nullable
    final BlockHandler mBlockHandler;

    private final Map<Class<? extends Node>, SpanFactory<Node>> mSpanFactories;

    private MarkdownConfig(MarkdownTheme theme, Map<Class<? extends Node>, NodeVisitor<Node>> visitors,
                           @Nullable BlockHandler blockHandler,
                           Map<Class<? extends Node>, SpanFactory<Node>> spanFactories) {
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

    @SuppressWarnings("unchecked")
    public static final class Builder {

        private final HashMap<Class<? extends Node>, NodeVisitor<Node>> mVisitors =
                new HashMap<>();

        private final HashMap<Class<? extends Node>, SpanFactory<Node>> mSpanFactories =
                new HashMap<>();

        private BlockHandler mBlockHandler;

        Builder() {
        }

        /**
         * Override any existing visitor for the given type.
         *
         * @param clazz   node type (exact class)
         * @param visitor {@link NodeVisitor} to be used, null to remove existing
         * @return this
         */
        @NonNull
        public <N extends Node> Builder addVisitor(@NonNull Class<? extends N> clazz,
                                                   @Nullable NodeVisitor<? super N> visitor) {
            if (visitor == null) {
                mVisitors.remove(clazz);
            } else {
                mVisitors.put(clazz, (NodeVisitor<Node>) visitor);
            }
            return this;
        }

        @NonNull
        public <N extends Node> Builder setSpanFactory(@NonNull Class<? extends N> clazz,
                                                       @Nullable SpanFactory<? super N> factory) {
            if (factory == null) {
                mSpanFactories.remove(clazz);
            } else {
                mSpanFactories.put(clazz, (SpanFactory<Node>) factory);
            }
            return this;
        }

        /**
         * Append a factory to existing one (or make the first one for specified node). Specified factory
         * will be called <strong>after</strong> original (if present) factory. Can be used to
         * <em>change</em> behavior or original span factory.
         *
         * @param clazz   node type
         * @param factory span factory
         * @return this
         */
        @NonNull
        public <N extends Node> Builder appendSpanFactory(@NonNull Class<? extends N> clazz,
                                                          @NonNull SpanFactory<? super N> factory) {
            Objects.requireNonNull(factory);
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

        /**
         * Prepend a factory to existing one (or make the first one for specified node). Specified factory
         * will be called <string>before</string> original (if present) factory.
         *
         * @param clazz   node type
         * @param factory span factory
         * @return this
         */
        @NonNull
        public <N extends Node> Builder prependSpanFactory(@NonNull Class<? extends N> clazz,
                                                           @NonNull SpanFactory<? super N> factory) {
            Objects.requireNonNull(factory);
            SpanFactory<Node> oldFactory = mSpanFactories.get(clazz);
            SpanFactory<Node> newFactory = (SpanFactory<Node>) factory;
            if (oldFactory != null) {
                if (oldFactory instanceof CompositeSpanFactory<Node> list) {
                    list.add(0, newFactory);
                } else {
                    mSpanFactories.put(clazz, new CompositeSpanFactory<>(newFactory, oldFactory));
                }
            } else {
                mSpanFactories.put(clazz, newFactory);
            }
            return this;
        }

        /**
         * Can be useful when <em>enhancing</em> an already defined SpanFactory with another one.
         */
        @Nullable
        public <N extends Node> SpanFactory<N> getSpanFactory(@NonNull Class<N> node) {
            return (SpanFactory<N>) mSpanFactories.get(node);
        }

        /**
         * @param blockHandler to handle block start/end
         * @return this
         */
        @NonNull
        public Builder setBlockHandler(@Nullable BlockHandler blockHandler) {
            mBlockHandler = blockHandler;
            return this;
        }

        @NonNull
        public MarkdownConfig build(MarkdownTheme theme) {
            return new MarkdownConfig(theme, mVisitors, mBlockHandler, mSpanFactories);
        }

        static class CompositeSpanFactory<N extends Node>
                extends ArrayList<SpanFactory<N>>
                implements SpanFactory<N> {

            public CompositeSpanFactory(SpanFactory<N> first, SpanFactory<N> second) {
                super(3);
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
    }
}
