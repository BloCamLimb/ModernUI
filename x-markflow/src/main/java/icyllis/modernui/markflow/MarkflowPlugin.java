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
import icyllis.modernui.text.Spanned;
import icyllis.modernui.widget.TextView;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * Class represents an extension to {@link Markflow} to configure how parsing and rendering
 * of Markdown is carried on.
 */
@ApiStatus.OverrideOnly
public interface MarkflowPlugin {

    /**
     * This class holds registered plugins and used to set dependencies.
     *
     * @see #configure(Registry)
     */
    interface Registry {

        /**
         * Creates a dependency and returns that plugin instance.
         */
        @NonNull
        <P extends MarkflowPlugin> P require(@NonNull Class<P> plugin);

        /**
         * Creates a dependency and consumes that plugin instance
         */
        <P extends MarkflowPlugin> void require(
                @NonNull Class<P> plugin,
                @NonNull Consumer<? super P> action);
    }

    /**
     * This method will be called before any other during {@link Markflow} instance construction.
     * Used to set dependencies and tweak certain plugin.
     *
     * @param registry the plugin registry
     */
    default void configure(@NonNull Registry registry) {
    }

    /**
     * Method to configure {@link Parser} (for example register custom extension, etc.).
     */
    default void configureParser(@NonNull Parser.Builder builder) {
    }

    /**
     * Method to configure {@link MarkflowTheme} that is used for rendering of Markdown.
     */
    default void configureTheme(@NonNull MarkflowTheme.Builder builder) {
    }

    /**
     * Method to configure span factories and miscellaneous used for rendering of Markdown.
     */
    default void configureConfig(@NonNull MarkflowConfig.Builder builder) {
    }

    /**
     * Method to configure node visitors used for rendering of Markdown.
     */
    default void configureVisitor(@NonNull MarkflowVisitor.Builder builder) {
    }

    /**
     * Process input Markdown text and return a CharSequence to be used in parsing stage
     * further. Can be described as <code>pre-processing</code> of raw Markdown.
     * <p>
     * The input CharSequence must be read-only by the method implementation. The
     * implementation should return either the input as-is or return a new CharSequence.
     * The implementation should never hold references to the input; and should not hold
     * references the return object, unless the return object is a String (immutable).
     * <p>
     * This method will be called from ANY thread.
     *
     * @param markdown input text to process
     * @return processed Markdown text
     */
    @NonNull
    default CharSequence processMarkdown(@NonNull CharSequence markdown) {
        return markdown;
    }

    /**
     * This method will be called <strong>before</strong> rendering will occur thus making possible
     * to <code>post-process</code> parsed node (make changes for example).
     * <p>
     * This method will be called from ANY thread.
     *
     * @param document the root document
     */
    default void beforeRender(@NonNull Node document) {
    }

    /**
     * This method will be called <strong>after</strong> rendering (but before applying markdown to a
     * TextView, if such action will happen). It can be used to clean some
     * internal state, or trigger certain action. Please note that modifying <code>node</code> won\'t
     * have any effect as it has been already <i>visited</i> at this stage.
     * <p>
     * This method will be called from ANY thread.
     *
     * @param document the root document
     * @param visitor  {@link MarkflowVisitor} instance used to render Markdown
     */
    default void afterRender(@NonNull Node document, @NonNull MarkflowVisitor visitor) {
    }

    /**
     * This method will be called <strong>before</strong> calling <code>TextView#setText</code>.
     * <p>
     * It can be useful to prepare a TextView for rendered Markdown.
     * <p>
     * This method will be called from UI thread.
     *
     * @param textView the TextView to which <code>markdown</code> will be applied
     * @param markdown the rendered Markdown
     */
    default void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
    }

    /**
     * This method will be called <strong>after</strong> markdown was applied.
     * <p>
     * It can be useful to trigger certain action on spans/textView.
     * <p>
     * Unlike {@link #beforeSetText(TextView, Spanned)} this method does not receive rendered Markdown
     * as at this point spans must be queried by calling <code>TextView#getText#getSpans</code>.
     * <p>
     * This method will be called from UI thread.
     *
     * @param textView the TextView to which markdown was applied
     */
    default void afterSetText(@NonNull TextView textView) {
    }
}
