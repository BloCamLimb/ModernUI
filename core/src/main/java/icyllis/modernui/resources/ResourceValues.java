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

import icyllis.modernui.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class ResourceValues {

    public static class Value {
        protected boolean weak = false;
        protected boolean translatable = true;
    }

    public static class Reference extends Value {

        public static final byte RESOURCE = 0;
        public static final byte ATTRIBUTE = 1;

        @Nullable
        public Resource.ResourceName name;
        public boolean has_id;
        public int id;
        public boolean has_type_flags;
        public int type_flags;
        public byte reference_type;
        public boolean private_reference = false;
        public boolean is_dynamic = false;
        public boolean allow_raw = false;

        @Override
        public String toString() {
            return "Reference{" +
                    "name=" + name +
                    ", has_id=" + has_id +
                    ", id=" + id +
                    ", has_type_flags=" + has_type_flags +
                    ", type_flags=" + type_flags +
                    ", reference_type=" + reference_type +
                    ", private_reference=" + private_reference +
                    ", is_dynamic=" + is_dynamic +
                    ", allow_raw=" + allow_raw +
                    '}';
        }
    }

    public static class Attribute extends Value {
        public static class Symbol {
            Reference symbol;
            int value; // u
            byte type; // u
        }

        public int type_mask; // u
        public int min_int;
        public int max_int;
        public List<Symbol> symbols;

        public Attribute(int t) {
            type_mask = t;
            min_int = Integer.MIN_VALUE;
            max_int = Integer.MAX_VALUE;
        }

        @Override
        public String toString() {
            return "Attribute{" +
                    "type_mask=" + type_mask +
                    ", min_int=" + min_int +
                    ", max_int=" + max_int +
                    ", symbols=" + symbols +
                    '}';
        }
    }

    public static class Styleable extends Value {
        public List<Reference> entries;

        @Override
        public String toString() {
            return "Styleable{" +
                    "entries=" + entries +
                    '}';
        }
    }
}
