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

package icyllis.modernui.graphics.text;

import icyllis.arc3d.core.Strike;
import icyllis.modernui.graphics.Rect;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Platform abstract font face, represents a single font file.
 */
public abstract class Font {

    public abstract boolean hasGlyph(int ch, int vs);

    public abstract float doSimpleLayout(char[] buf,
                                         int start, int limit,
                                         int style, int size, int render_flags,
                                         IntArrayList glyphs, FloatArrayList positions);

    public abstract float doComplexLayout(char[] buf,
                                          int context_start, int context_limit,
                                          int layout_start, int layout_limit,
                                          int style, int size, int render_flags,
                                          IntArrayList glyphs, FloatArrayList positions,
                                          float[] advances, int advance_offset,
                                          Rect bounds);

    public abstract Strike findOrCreateStrike(int style, int size, int render_flags);
}
