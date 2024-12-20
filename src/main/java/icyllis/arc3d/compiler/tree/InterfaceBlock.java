/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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
import icyllis.arc3d.compiler.ShaderKind;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

/**
 * An interface block declaration & definition, as in:
 * <pre>
 * out SV_PerVertex {
 *   layout(position) out float4 SV_Position;
 * };
 * </pre>
 * At the AST level, this is represented by a single variable of struct type.
 */
public final class InterfaceBlock extends TopLevelElement {

    private final WeakReference<Variable> mVariable;

    public InterfaceBlock(int position, @NonNull Variable variable) {
        super(position);
        mVariable = new WeakReference<>(variable);
        variable.setInterfaceBlock(this);
    }

    private static boolean checkBlock(@NonNull Context context,
                                      int pos,
                                      @NonNull Modifiers modifiers,
                                      @NonNull Type blockType,
                                      int blockStorage) {
        boolean success = true;
        if (blockType.isUnsizedArray()) {
            context.error(pos, "interface blocks may not have unsized array type");
            success = false;
        }
        int permittedFlags = Modifiers.kStorage_Flags;
        int permittedLayoutFlags = 0;
        if ((blockStorage & (Modifiers.kIn_Flag | Modifiers.kOut_Flag)) != 0) {
            permittedLayoutFlags |= Layout.kLocation_LayoutFlag;
        }
        if ((blockStorage & (Modifiers.kUniform_Flag | Modifiers.kBuffer_Flag)) != 0) {
            permittedLayoutFlags |= Layout.kBinding_LayoutFlag |
                    Layout.kSet_LayoutFlag |
                    Layout.kPushConstant_LayoutFlag |
                    Layout.kStd140_LayoutFlag |
                    Layout.kStd430_LayoutFlag;
        }
        if (blockStorage == Modifiers.kBuffer_Flag) {
            permittedFlags |= Modifiers.kMemory_Flags;
        }
        if ((modifiers.layoutFlags() & Layout.kPushConstant_LayoutFlag) != 0 &&
                (modifiers.layoutFlags() & (Layout.kBinding_LayoutFlag | Layout.kSet_LayoutFlag)) != 0) {
            context.error(pos, "'push_constant' cannot be used with 'binding' or 'set'");
            success = false;
        }
        success &= modifiers.checkFlags(context, permittedFlags);
        success &= modifiers.checkLayoutFlags(context, permittedLayoutFlags);
        return success;
    }

    private static boolean checkFields(@NonNull Context context,
                                       @Unmodifiable @NonNull List<Type.Field> fields,
                                       int blockStorage) {
        boolean success = true;
        for (int i = 0; i < fields.size(); i++) {
            Type.Field field = fields.get(i);
            Modifiers fieldModifiers = field.modifiers();
            int permittedFlags = Modifiers.kStorage_Flags;
            int permittedLayoutFlags = 0;
            if ((fieldModifiers.flags() & Modifiers.kStorage_Flags) != 0 &&
                    (fieldModifiers.flags() & Modifiers.kStorage_Flags) != blockStorage) {
                context.error(field.modifiers().mPosition,
                        "storage qualifier of a member must be storage qualifier '" +
                                Modifiers.describeFlag(blockStorage) + "' of the block");
                success = false;
            }
            if ((blockStorage & (Modifiers.kIn_Flag | Modifiers.kOut_Flag)) != 0) {
                permittedFlags |= Modifiers.kInterpolation_Flags;
                permittedLayoutFlags |= Layout.kBuiltin_LayoutFlag |
                        Layout.kLocation_LayoutFlag | Layout.kComponent_LayoutFlag;
            }
            if ((blockStorage & (Modifiers.kUniform_Flag | Modifiers.kBuffer_Flag)) != 0) {
                permittedLayoutFlags |= Layout.kOffset_LayoutFlag;
            }
            if (blockStorage == Modifiers.kBuffer_Flag) {
                permittedFlags |= Modifiers.kMemory_Flags;
            }
            success &= fieldModifiers.checkFlags(context, permittedFlags);
            success &= fieldModifiers.checkLayoutFlags(context, permittedLayoutFlags);
            if (field.type().isOpaque()) {
                context.error(field.position(), "opaque type '" + field.type().getName() +
                        "' is not permitted in an interface block");
                success = false;
            }
            if (field.type().isUnsizedArray()) {
                if (blockStorage == Modifiers.kBuffer_Flag) {
                    if (i != fields.size() - 1) {
                        context.error(field.position(),
                                "runtime sized array must be the last member of a shader storage block");
                        success = false;
                    }
                } else {
                    context.error(field.position(),
                            "runtime sized array is only permitted in shader storage blocks");
                    success = false;
                }
            }
        }
        return success;
    }

    @Nullable
    public static InterfaceBlock convert(@NonNull Context context,
                                         int pos,
                                         @NonNull Modifiers modifiers,
                                         @NonNull Type blockType,
                                         @NonNull String instanceName) {
        if (!blockType.getElementType().isInterfaceBlock()) {
            context.error(pos, "type '" + blockType + "' is not an interface block");
            return null;
        }
        boolean success = true;
        ShaderKind kind = context.getKind();
        if (!kind.isFragment() && !kind.isVertex() && !kind.isCompute()) {
            context.error(pos, "interface blocks are not allowed in this shader kind");
            success = false;
        }

        if ((modifiers.flags() & Modifiers.kWorkgroup_Flag) != 0) {
            context.error(pos, "workgroup qualifier is not allowed on an interface block");
            return null;
        }
        int blockStorage = modifiers.flags() & Modifiers.kStorage_Flags;
        if (Integer.bitCount(blockStorage) != 1) {
            context.error(pos, "an interface block must start with one of in, out, uniform, or buffer qualifier");
            return null;
        }

        if (kind.isVertex() && blockStorage == Modifiers.kIn_Flag) {
            context.error(pos, "an input block is not allowed in vertex shader kind");
            success = false;
        }
        if (kind.isFragment() && blockStorage == Modifiers.kOut_Flag) {
            context.error(pos, "an output block is not allowed in fragment shader kind");
            success = false;
        }

        success &= checkBlock(context, pos, modifiers, blockType, blockStorage);
        success &= checkFields(context, blockType.getElementType().getFields(), blockStorage);

        if (!success) {
            return null;
        }

        Variable variable = Variable.convert(
                context,
                pos,
                modifiers,
                blockType,
                instanceName,
                Variable.kGlobal_Storage
        );

        return InterfaceBlock.make(context, pos, variable);
    }

    @NonNull
    public static InterfaceBlock make(@NonNull Context context,
                                      int pos,
                                      @NonNull Variable variable) {
        assert variable.getType().getElementType().isInterfaceBlock();

        if (variable.getName().isEmpty()) {
            // This interface block is anonymous. Add each field to the top-level symbol table.
            var fields = variable.getType().getFields();
            for (int i = 0; i < fields.size(); i++) {
                context.getSymbolTable().insert(
                        context,
                        new AnonymousField(fields.get(i).position(), variable, i)
                );
            }
        } else {
            // Add the global variable to the top-level symbol table.
            context.getSymbolTable().insert(context, variable);
        }

        return new InterfaceBlock(pos, variable);
    }

    @NonNull
    public Variable getVariable() {
        return Objects.requireNonNull(mVariable.get(), "your symbol table is gone");
    }

    @NonNull
    public String getBlockName() {
        return getVariable().getType().getElementType().getName();
    }

    @NonNull
    public String getInstanceName() {
        return getVariable().getName();
    }

    public int getArraySize() {
        Type type = getVariable().getType();
        return type.isArray() ? type.getArraySize() : 0;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.INTERFACE_BLOCK;
    }

    @NonNull
    @Override
    public String toString() {
        Variable variable = getVariable();
        StringBuilder result = new StringBuilder(
                variable.getModifiers().toString() + getBlockName() + " {\n");
        Type type = variable.getType();
        for (var field : type.getElementType().getFields()) {
            result.append(field.toString()).append("\n");
        }
        result.append("}");
        if (!getInstanceName().isEmpty()) {
            result.append(" ").append(getInstanceName());
            if (type.isArray()) {
                result.append("[").append(type.getArraySize()).append("]");
            }
        }
        result.append(";");
        return result.toString();
    }
}
