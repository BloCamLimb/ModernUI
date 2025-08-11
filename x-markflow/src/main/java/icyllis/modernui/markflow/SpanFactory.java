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
import icyllis.modernui.util.DataSet;
import org.commonmark.node.Node;

/**
 * Span factory is invoked by {@link MarkflowVisitor} to create spans for a given node.
 * Use {@link MarkflowConfig.Builder} to register span factories.
 */
@FunctionalInterface
public interface SpanFactory<N extends Node> {

    /**
     * Create a span object or an array of span objects for the given node.
     * This method can be called from ANY thread.
     *
     * @param config context options
     * @param node   AST node
     * @param args   optional arguments
     * @return span, array of spans, or null
     */
    @Nullable
    Object createSpans(@NonNull MarkflowConfig config, @NonNull N node, @NonNull DataSet args);
}
