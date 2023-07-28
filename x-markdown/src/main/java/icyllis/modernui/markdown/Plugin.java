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

import java.util.function.Consumer;

/**
 * Class represents a plugin (extension) to {@link Markdown} to configure how parsing and rendering
 * of markdown is carried on.
 */
public interface Plugin {

    /**
     * @see #configure(Registry)
     */
    interface Registry {

        @NonNull
        <P extends Plugin> P require(@NonNull Class<P> plugin);

        <P extends Plugin> void require(
                @NonNull Class<P> plugin,
                @NonNull Consumer<? super P> action);
    }

    /**
     * This method will be called before any other during {@link Markdown} instance construction.
     *
     * @param registry the plugin registry
     */
    default void configure(@NonNull Registry registry) {
    }

    /**
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
