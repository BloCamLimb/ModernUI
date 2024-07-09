/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite;

/**
 * A SubRun represents a method to draw a subregion of a GlyphRun, where
 * GlyphRun represents the shaped text (positioned glyphs) and a strike.
 * SubRun is the basic unit that is ready for GPU task generation, except
 * that it does not contain the current transformation matrix.
 */
public class SubRunContainer {

    public interface AtlasSubRun {

    }

    /**
     * SubRun defines the most basic functionality of a SubRun; the ability to draw, and the
     * ability to be in a list.
     */
    public static abstract class SubRun {
        SubRun mNext;
    }
}
