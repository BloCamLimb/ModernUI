/*
 * Modern UI.
 * Copyright (C) 2023-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.markflow;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import org.commonmark.node.Node;

import javax.annotation.concurrent.Immutable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds configuration for rendering Markdown.
 */
@Immutable
public final class MarkflowConfig {

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    private final MarkflowTheme mTheme;
    private final Map<Class<? extends Node>, SpanFactory<Node>> mSpanFactories;

    private MarkflowConfig(@NonNull MarkflowTheme theme,
                           @NonNull Builder b) {
        mTheme = theme;
        var factories = new HashMap<>(b.mSpanFactories);
        b.mSpanFactories.clear(); // clear Builder for safety, CompositeSpanFactory is mutable
        mSpanFactories = factories;
    }

    @NonNull
    public MarkflowTheme getTheme() {
        return mTheme;
    }

    @Nullable
    public SpanFactory<Node> getSpanFactory(@NonNull Class<? extends Node> clazz) {
        return mSpanFactories.get(clazz);
    }

    @NonNull
    public SpanFactory<Node> requireSpanFactory(@NonNull Class<? extends Node> clazz) {
        return Objects.requireNonNull(mSpanFactories.get(clazz));
    }

    @SuppressWarnings("unchecked")
    public static final class Builder {

        private final HashMap<Class<? extends Node>, SpanFactory<Node>> mSpanFactories =
                new HashMap<>();

        Builder() {
        }

        /**
         * Replace/remove the span factory for the given type.
         *
         * @param clazz   node type
         * @param factory span factory
         * @return this
         */
        @NonNull
        public <N extends Node>
        Builder setSpanFactory(@NonNull Class<? extends N> clazz,
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
        public <N extends Node>
        Builder appendSpanFactory(@NonNull Class<? extends N> clazz,
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
        public <N extends Node>
        Builder prependSpanFactory(@NonNull Class<? extends N> clazz,
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
        public <N extends Node>
        SpanFactory<N> getSpanFactory(@NonNull Class<N> node) {
            return (SpanFactory<N>) mSpanFactories.get(node);
        }

        /**
         * Build the instance, and this builder <em>cannot</em> be reused.
         */
        @NonNull
        public MarkflowConfig build(@NonNull MarkflowTheme theme) {
            Objects.requireNonNull(theme);
            return new MarkflowConfig(theme, this);
        }
    }
}
