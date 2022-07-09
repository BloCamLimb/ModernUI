/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.textmc;

import icyllis.modernui.textmc.mixin.*;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.*;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * The text layout engine requires the raw chars without formatting codes and
 * in logical order. This prevents text from being reordered. Additionally,
 * {@link FormattedCharSequence} converts string to code points, but we will
 * combine these code points to char[] again in layout engine, this helps to
 * get the original {@link FormattedText} (via instanceof), and the performance
 * will be better.
 *
 * @author BloCamLimb
 * @see MixinBidiReorder
 * @see MixinClientLanguage
 * @see MixinLanguage
 * @see MultilayerTextKey.Lookup#update(FormattedCharSequence)
 * @see TextLayoutProcessor#performSequenceLayout(FormattedCharSequence)
 * @see net.minecraft.network.chat.SubStringSource
 * @see net.minecraft.client.resources.language.FormattedBidiReorder
 */
public class FormattedTextWrapper implements FormattedCharSequence {

    @Nonnull
    public final FormattedText mText;

    public FormattedTextWrapper(@Nonnull FormattedText text) {
        mText = text;
    }

    /**
     * Needed only when compositing, do not use explicitly. This should be equivalent to
     * {@link StringDecomposer#iterateFormatted(FormattedText, Style, FormattedCharSink)}.
     *
     * @param sink code point consumer
     * @return true if all chars consumed, false otherwise
     * @see FormattedCharSequence#composite(List)
     */
    @Override
    public boolean accept(@Nonnull FormattedCharSink sink) {
        // do not reorder, transfer code points in logical order
        return mText.visit((style, text) ->
                StringDecomposer.iterateFormatted(text, style, sink) ? Optional.empty()
                        : FormattedText.STOP_ITERATION, Style.EMPTY).isEmpty();
    }
}
