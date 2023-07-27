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

package icyllis.modernui.text.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.text.*;
import icyllis.modernui.util.BinaryIO;

import java.io.*;

/**
 * Span that allows defining the alignment of text at the paragraph level.
 */
public interface AlignmentSpan extends ParagraphStyle {

    /**
     * Returns the alignment of the text.
     *
     * @return the text alignment
     */
    Layout.Alignment getAlignment();

    class Standard implements AlignmentSpan, ParcelableSpan {
        private final Layout.Alignment mAlignment;

        /**
         * Constructs a {@link Standard} from an alignment.
         */
        public Standard(@NonNull Layout.Alignment align) {
            mAlignment = align;
        }

        /**
         * Constructs a {@link Standard} from a parcel.
         */
        public Standard(@NonNull DataInput in) throws IOException {
            mAlignment = Layout.Alignment.valueOf(BinaryIO.readString(in));
        }

        @Override
        public int getSpanTypeId() {
            return TextUtils.ALIGNMENT_SPAN;
        }

        @Override
        public Layout.Alignment getAlignment() {
            return mAlignment;
        }

        @Override
        public void write(@NonNull DataOutput dest) throws IOException {
            BinaryIO.writeString(dest, mAlignment.name());
        }
    }
}
