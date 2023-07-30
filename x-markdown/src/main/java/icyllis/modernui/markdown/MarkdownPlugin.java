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

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.widget.TextView;

import java.util.function.Consumer;

/**
 * Class represents an extension to {@link Markdown} to configure how parsing and rendering
 * of markdown is carried on.
 */
public interface MarkdownPlugin {

    /**
     * @see #configure(Registry)
     */
    interface Registry {

        @NonNull
        <P extends MarkdownPlugin> P require(@NonNull Class<P> plugin);

        <P extends MarkdownPlugin> void require(
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
    default void configureParser(@NonNull Parser.Builder builder) {
    }

    /**
     * @param builder
     */
    default void configureTheme(@NonNull MarkdownTheme.Builder builder) {
    }

    /**
     * @param builder
     */
    default void configureConfig(@NonNull MarkdownConfig.Builder builder) {
    }

    /**
     * Process input markdown and return new string to be used in parsing stage further.
     * Can be described as <code>pre-processing</code> of markdown String.
     *
     * @param markdown String to process
     * @return processed markdown String
     */
    @NonNull
    default String processMarkdown(@NonNull String markdown) {
        return markdown;
    }

    /**
     * This method will be called <strong>before</strong> rendering will occur thus making possible
     * to <code>post-process</code> parsed node (make changes for example).
     *
     * @param document root document
     */
    default void beforeRender(@NonNull Node document) {
    }

    /**
     * This method will be called <strong>after</strong> rendering (but before applying markdown to a
     * TextView, if such action will happen). It can be used to clean some
     * internal state, or trigger certain action. Please note that modifying <code>node</code> won\'t
     * have any effect as it has been already <i>visited</i> at this stage.
     *
     * @param document root document
     * @param visitor  {@link MarkdownVisitor} instance used to render markdown
     */
    default void afterRender(@NonNull Node document, @NonNull MarkdownVisitor visitor) {
    }

    /**
     * This method will be called <strong>before</strong> calling <code>TextView#setText</code>.
     * <p>
     * It can be useful to prepare a TextView for markdown.
     *
     * @param textView TextView to which <code>markdown</code> will be applied
     * @param markdown Rendered markdown
     */
    default void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
    }

    /**
     * This method will be called <strong>after</strong> markdown was applied.
     * <p>
     * It can be useful to trigger certain action on spans/textView.
     * <p>
     * Unlike {@link #beforeSetText(TextView, Spanned)} this method does not receive parsed markdown
     * as at this point spans must be queried by calling <code>TextView#getText#getSpans</code>.
     *
     * @param textView TextView to which markdown was applied
     */
    default void afterSetText(@NonNull TextView textView) {
    }
}
