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
import icyllis.modernui.text.*;
import icyllis.modernui.util.DataSet;
import org.commonmark.node.Block;
import org.commonmark.node.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Used for rendering and building Markdown.
 */
public final class MarkflowVisitor {

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    private final MarkflowConfig mConfig;

    private final DataSet mArguments = new DataSet();
    private final SpannableStringBuilder mSpannableBuilder = new SpannableStringBuilder();

    private final Map<Class<? extends Node>, NodeVisitor<Node>> mVisitors;

    @Nullable
    private final BlockHandler mBlockHandler;
    private boolean mInBlockHandler = false;

    MarkflowVisitor(@NonNull MarkflowConfig config,
                    @NonNull Map<Class<? extends Node>, NodeVisitor<Node>> visitors,
                    @Nullable BlockHandler blockHandler) {
        mConfig = config;
        mVisitors = visitors;
        mBlockHandler = blockHandler;
    }

    public void visit(@NonNull Node node) {
        NodeVisitor<Node> visitor = mVisitors.get(node.getClass());
        if (visitor != null) {
            visitor.visit(this, node);
        } else {
            visitChildren(node);
        }
    }

    public void visitChildren(@NonNull Node parent) {
        Node child = parent.getFirstChild();
        while (child != null) {
            // A subclass of this visitor might modify the node, resulting in getNext returning a different node or no
            // node after visiting it. So get the next node before visiting.
            Node next = child.getNext();
            visit(child);
            child = next;
        }
    }

    @NonNull
    public MarkflowConfig getConfig() {
        return mConfig;
    }

    /**
     * Used to pass data between visitors and span factories.
     */
    @NonNull
    public DataSet getArguments() {
        return mArguments;
    }

    @NonNull
    public SpannableStringBuilder getSpannableBuilder() {
        return mSpannableBuilder;
    }

    public MarkflowVisitor append(char c) {
        mSpannableBuilder.append(c);
        return this;
    }

    public MarkflowVisitor append(@NonNull CharSequence text) {
        mSpannableBuilder.append(text);
        return this;
    }

    public int length() {
        return mSpannableBuilder.length();
    }

    /**
     * Executes a check if there is further content available.
     *
     * @param node to check
     * @return boolean indicating if there are more nodes after supplied one
     */
    public boolean hasNext(@NonNull Node node) {
        return node.getNext() != null;
    }

    public void ensureNewLine() {
        int len = mSpannableBuilder.length();
        if (len > 0 && mSpannableBuilder.charAt(len - 1) != '\n') {
            mSpannableBuilder.append('\n');
        }
    }

    public void forceNewLine() {
        mSpannableBuilder.append('\n');
    }

    public void beforeBlock(@NonNull Block block) {
        if (mBlockHandler != null && !mInBlockHandler) {
            mInBlockHandler = true;
            mBlockHandler.beforeBlock(this, block);
            mInBlockHandler = false;
        } else {
            ensureNewLine();
        }
    }

    public void afterBlock(@NonNull Block block) {
        if (mBlockHandler != null && !mInBlockHandler) {
            mInBlockHandler = true;
            mBlockHandler.afterBlock(this, block);
            mInBlockHandler = false;
        } else {
            if (hasNext(block)) {
                ensureNewLine();
                forceNewLine();
            }
        }
    }

    @Nullable
    public <N extends Node> Object preSetSpans(@NonNull N node, int offset) {
        SpanFactory<Node> factory = mConfig.getSpanFactory(node.getClass());
        if (factory != null) {
            Object spans = factory.createSpans(mConfig, node, mArguments);
            setSpans(spans, offset, offset, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            return spans;
        }
        return null;
    }

    public void postSetSpans(@Nullable Object spans, int offset) {
        setSpans(spans, offset, mSpannableBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public void setSpans(@Nullable Object spans, int start, int end, int flags) {
        if (spans != null) {
            if (spans.getClass().isArray()) {
                for (Object span : (Object[]) spans) {
                    setSpans(span, start, end, flags);
                }
            } else {
                mSpannableBuilder.setSpan(spans, start, end, flags);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static final class Builder {

        final HashMap<Class<? extends Node>, NodeVisitor<Node>> mVisitors =
                new HashMap<>();

        @Nullable
        BlockHandler mBlockHandler;

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
        public <N extends Node> Builder on(@NonNull Class<? extends N> clazz,
                                           @Nullable NodeVisitor<? super N> visitor) {
            if (visitor == null) {
                mVisitors.remove(clazz);
            } else {
                mVisitors.put(clazz, (NodeVisitor<Node>) visitor);
            }
            return this;
        }

        /**
         * @param blockHandler to handle block start/end
         * @return this
         */
        @NonNull
        public Builder blockHandler(@Nullable BlockHandler blockHandler) {
            mBlockHandler = blockHandler;
            return this;
        }

        /**
         * You can build an instance yourself for testing. This builder can be reused
         * and the state will be preserved.
         */
        @NonNull
        public MarkflowVisitor build(@NonNull MarkflowConfig config) {
            Objects.requireNonNull(config);
            return new MarkflowVisitor(
                    config,
                    new HashMap<>(mVisitors),
                    mBlockHandler
            );
        }
    }
}
