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

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.Context;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * A struct at global scope, as in:
 * <pre>{@code
 * struct Material {
 *     float roughness;
 *     float metalness;
 * };
 * }</pre>
 */
public final class StructDefinition extends TopLevelElement {

    @NonNull
    private final Type mType;

    public StructDefinition(int position, @NonNull Type type) {
        super(position);
        mType = type;
    }

    @Nullable
    public static StructDefinition convert(@NonNull Context context,
                                           int pos,
                                           @NonNull String structName,
                                           @NonNull List<Type.Field> fields) {
        Type type = Type.makeStructType(context,
                pos,
                structName,
                fields,
                false
        );
        return make(pos, context.getSymbolTable().insert(context, type));
    }

    @Nullable
    public static StructDefinition make(int pos, Type type) {
        if (type == null) {
            return null;
        }
        return new StructDefinition(pos, type);
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.STRUCT_DEFINITION;
    }

    @Override
    public boolean accept(@NonNull TreeVisitor visitor) {
        return visitor.visitStructDefinition(this);
    }

    @NonNull
    public Type getType() {
        return mType;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(
                "struct " + mType.getName() + " {\n");
        for (var field : mType.getFields()) {
            result.append(field.toString()).append("\n");
        }
        result.append("}");
        return result.toString();
    }
}
