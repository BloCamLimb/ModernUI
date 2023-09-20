/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.shaderc.dsl;

import icyllis.arc3d.shaderc.ThreadContext;
import icyllis.arc3d.shaderc.tree.Literal;
import icyllis.arc3d.shaderc.tree.Type;

public class DSLType {

    public static Type Array(Type type, int size, int position) {
        size = type.convertArraySize(position, Literal.makeInteger(position, size));
        if (size == 0) {
            return ThreadContext.getInstance().getTypes().mPoison;
        }
        return ThreadContext.getInstance().getSymbolTable().getArrayType(type, size);
    }

    public static Type UnsizedArray(Type type, int position) {
        if (!type.isUsableInArray(position)) {
            return ThreadContext.getInstance().getTypes().mPoison;
        }
        return ThreadContext.getInstance().getSymbolTable().getArrayType(type, Type.kUnsizedArray);
    }
}
