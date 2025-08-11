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
import org.commonmark.node.Node;

/**
 * Visitor for certain node class, can be called from ANY thread.
 * Use {@link MarkflowVisitor.Builder} to register node visitors.
 */
@FunctionalInterface
public interface NodeVisitor<N extends Node> {

    void visit(@NonNull MarkflowVisitor visitor, @NonNull N node);
}
