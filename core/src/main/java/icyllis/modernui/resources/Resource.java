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

package icyllis.modernui.resources;

public class Resource {

    public static final int
            TYPE_ATTR = 0,
            TYPE_ID = 1,
            TYPE_STYLE = 2,
            TYPE_STRING = 3,
            TYPE_DIMEN = 4,
            TYPE_COLOR = 5,
            TYPE_ARRAY = 6,
            TYPE_DRAWABLE = 7,
            TYPE_LAYOUT = 8,
            TYPE_BOOL = 9,
            TYPE_ANIMATOR = 10,
            TYPE_INTERPOLATOR = 11,
            TYPE_MIPMAP = 12,
            TYPE_INTEGER = 13,
            TYPE_TRANSITION = 14,
            TYPE_RAW = 15,
            TYPE_MENU = 16,
            TYPE_XML = 17;

    public static String getTypeName(int type) {
        return switch (type) {
            case TYPE_ANIMATOR -> "animator";
            case TYPE_ARRAY -> "array";
            case TYPE_ATTR -> "attr";
            case TYPE_BOOL -> "bool";
            case TYPE_COLOR -> "color";
            case TYPE_DIMEN -> "dimen";
            case TYPE_DRAWABLE -> "drawable";
            case TYPE_ID -> "id";
            case TYPE_INTEGER -> "integer";
            case TYPE_INTERPOLATOR -> "interpolator";
            case TYPE_LAYOUT -> "layout";
            case TYPE_MENU -> "menu";
            case TYPE_MIPMAP -> "mipmap";
            case TYPE_RAW -> "raw";
            case TYPE_STRING -> "string";
            case TYPE_STYLE -> "style";
            case TYPE_TRANSITION -> "transition";
            case TYPE_XML -> "xml";
            default -> "";
        };
    }
}
