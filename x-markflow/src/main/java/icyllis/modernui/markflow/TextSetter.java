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

import java.util.function.Consumer;

/**
 * Interface to set text on a TextView. Primary goal is to give a way to use PrecomputedText
 * functionality.
 */
@FunctionalInterface
public interface TextSetter {

    /**
     * Called by {@link Markflow} to install rendered Markdown to TextView.
     * <p>
     * The implementation must invoke the given <var>afterSetText</var> with
     * the given TextView on UI thread when set-text is finished. However,
     * the implementation can create a WeakReference for the TextView. If
     * it is no longer reachable, the set-text operation should be canceled
     * and there is no need to invoke <var>afterSetText</var>.
     *
     * @param textView     the TextView
     * @param markdown     the rendered Markdown
     * @param bufferType   the desired BufferType
     * @param afterSetText action to run when set-text is finished (required to call in order
     *                     to execute {@link MarkflowPlugin#afterSetText(TextView)})
     */
    void setText(@NonNull TextView textView,
                 @NonNull Spanned markdown,
                 @NonNull TextView.BufferType bufferType,
                 @NonNull Consumer<TextView> afterSetText);
}
