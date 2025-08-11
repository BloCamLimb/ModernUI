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

package icyllis.modernui.markflow;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.util.DataSet;
import org.commonmark.node.Node;

import java.util.ArrayList;

// Since this class directly extends ArrayList, do not expose as public API
final class CompositeSpanFactory<N extends Node> extends ArrayList<SpanFactory<N>> implements SpanFactory<N> {

    public CompositeSpanFactory(@NonNull SpanFactory<N> first, @NonNull SpanFactory<N> second) {
        super(4);
        add(first);
        add(second);
    }

    @NonNull
    @Override
    public Object createSpans(@NonNull MarkflowConfig config, @NonNull N node, @NonNull DataSet args) {
        final int count = size();
        final Object[] spans = new Object[count];
        for (int i = 0; i < count; i++) {
            spans[i] = get(i).createSpans(config, node, args);
        }
        return spans;
    }
}
