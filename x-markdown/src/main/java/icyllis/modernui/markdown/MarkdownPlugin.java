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

/**
 * An extension point for a {@link Markdown}.
 */
public interface MarkdownPlugin {

    default void register(@NonNull MarkdownPluginRegistry registry) {
    }

    /**
     *
     * @param builder
     */
    default void configure(@NonNull MarkdownConfig.Builder builder) {
    }

    /**
     * Process input markdown and return new string to be used in parsing stage further.
     * Can be described as <code>pre-processing</code> of markdown String.
     *
     * @param markdown String to process
     * @return processed markdown String
     */
    @NonNull
    default String preProcess(@NonNull String markdown) {
        return markdown;
    }
}
