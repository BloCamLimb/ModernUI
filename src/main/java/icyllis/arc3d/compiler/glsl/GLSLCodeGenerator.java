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

package icyllis.arc3d.compiler.glsl;

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.compiler.tree.InterfaceBlock;
import icyllis.arc3d.compiler.tree.Layout;
import icyllis.arc3d.compiler.tree.Modifiers;
import icyllis.arc3d.compiler.tree.Type;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Standard GLSL code generator for OpenGL 3.3 or above and Vulkan 1.0 or above (Vulkan GLSL).
 * <p>
 * A GLSL shader is a UTF-8 encoded string. However, our compiler only outputs ASCII characters.
 */
//TODO WIP
public final class GLSLCodeGenerator extends CodeGenerator {

    public final TargetApi mOutputTarget;
    public final GLSLVersion mOutputVersion;

    private Output mOutput;
    private int mIndentation = 0;
    private boolean mAtLineStart = false;

    private boolean mPrettyPrint;

    public GLSLCodeGenerator(@NonNull ShaderCompiler compiler,
                             @NonNull TranslationUnit translationUnit,
                             @NonNull ShaderCaps shaderCaps) {
        super(compiler, translationUnit);
        mOutputTarget = Objects.requireNonNullElse(shaderCaps.mTargetApi, TargetApi.OPENGL_4_5);
        mOutputVersion = Objects.requireNonNullElse(shaderCaps.mGLSLVersion, GLSLVersion.GLSL_450);
        mPrettyPrint = !getContext().getOptions().mMinifyCode;
    }

    @Override
    public @Nullable ByteBuffer generateCode() {
        return null;
    }

    private void write(char c) {
        assert c != '\n' && c <= 0x7F;
        mOutput.write(c);
        mAtLineStart = false;
    }

    private void write(@NonNull String s) {
        if (s.isEmpty()) {
            return;
        }
        if (mAtLineStart && mPrettyPrint) {
            for (int i = 0; i < mIndentation; i++) {
                mOutput.writeString8("    ");
            }
        }
        if (!mOutput.writeString8(s)) {
            getContext().error(Position.NO_POS, "invalid string '" + s + "'");
        }
        mAtLineStart = false;
    }

    private void writeLine() {
        mOutput.write('\n');
        mAtLineStart = true;
    }

    private void writeLine(char c) {
        mOutput.write(c);
        mOutput.write('\n');
        mAtLineStart = true;
    }

    private void writeLine(@NonNull String s) {
        write(s);
        mOutput.write('\n');
        mAtLineStart = true;
    }

    private void finishLine() {
        if (!mAtLineStart) {
            writeLine();
        }
    }

    private void writeIdentifier(@NonNull String identifier) {
        write(identifier);
    }

    private void writeModifiers(@NonNull Modifiers modifiers) {
        Layout layout = modifiers.layout();
        if (layout != null) {
            write(layout.toString());
        }

        int flags = modifiers.flags();
        if ((flags & Modifiers.kFlat_Flag) != 0) {
            write("flat ");
        }
        if ((flags & Modifiers.kNoPerspective_Flag) != 0) {
            write("noperspective ");
        }
        if ((flags & Modifiers.kConst_Flag) != 0) {
            write("const ");
        }
        if ((flags & Modifiers.kUniform_Flag) != 0) {
            write("uniform ");
        }
        if ((flags & Modifiers.kIn_Flag) != 0 && (flags & Modifiers.kOut_Flag) != 0) {
            write("inout ");
        } else if ((flags & Modifiers.kIn_Flag) != 0) {
            write("in ");
        } else if ((flags & Modifiers.kOut_Flag) != 0) {
            write("out ");
        }
        if ((flags & Modifiers.kCoherent_Flag) != 0) {
            write("coherent ");
        }
        if ((flags & Modifiers.kVolatile_Flag) != 0) {
            write("volatile ");
        }
        if ((flags & Modifiers.kRestrict_Flag) != 0) {
            write("restrict ");
        }
        if ((flags & Modifiers.kReadOnly_Flag) != 0) {
            write("readonly ");
        }
        if ((flags & Modifiers.kWriteOnly_Flag) != 0) {
            write("writeonly ");
        }
        if ((flags & Modifiers.kBuffer_Flag) != 0) {
            write("buffer ");
        }

        if ((flags & Modifiers.kWorkgroup_Flag) != 0) {
            write("shared ");
        }
    }

    private @NonNull String getTypeName(@NonNull Type type) {
        switch (type.getTypeKind()) {
            case Type.kScalar_TypeKind -> {
                if (type.isFloat() && type.getWidth() == 32) {
                    return "float";
                }
                if (type.isSigned()) {
                    assert type.getWidth() == 32;
                    return "int";
                }
                if (type.isUnsigned()) {
                    assert type.getWidth() == 32;
                    return "uint";
                }
                return type.getName();
            }
            case Type.kVector_TypeKind -> {
                Type component = type.getComponentType();
                String result;
                if (component.isFloat() && component.getWidth() == 32) {
                    result = "vec";
                } else if (component.isSigned()) {
                    result = "ivec";
                } else if (component.isUnsigned()) {
                    result = "uvec";
                } else if (component.isBoolean()) {
                    result = "bvec";
                } else {
                    assert false;
                    result = "";
                }
                return result + type.getRows();
            }
            case Type.kMatrix_TypeKind -> {
                Type component = type.getComponentType();
                String result;
                if (component.isFloat() && component.getWidth() == 32) {
                    result = "mat";
                } else {
                    assert false;
                    result = "";
                }
                result += type.getCols();
                if (type.getCols() != type.getRows()) {
                    result += "x" + type.getRows();
                }
                return result;
            }
            case Type.kArray_TypeKind -> {
                String baseName = getTypeName(type.getElementType());
                return Type.getArrayName(baseName, type.getArraySize());
            }
            default -> {
                return type.getName();
            }
        }
    }

    private void writeType(@NonNull Type type) {
        writeIdentifier(getTypeName(type));
    }

    private @NonNull String getTypePrecision(@NonNull Type type) {
        if (getContext().getOptions().mUsePrecisionQualifiers) {
            switch (type.getTypeKind()) {
                case Type.kScalar_TypeKind -> {
                    if (type.getWidth() == 32) {
                        if (type.getMinWidth() == 32) {
                            return "highp ";
                        }
                        if (type.getMinWidth() == 16) {
                            return getContext().getOptions().mForceHighPrecision ? "highp " : "mediump ";
                        }
                    }
                    return "";
                }
                case Type.kVector_TypeKind,
                     Type.kMatrix_TypeKind -> {
                    return getTypePrecision(type.getComponentType());
                }
                case Type.kArray_TypeKind -> {
                    return getTypePrecision(type.getElementType());
                }
            }
        }
        return "";
    }

    private void writeTypePrecision(@NonNull Type type) {
        write(getTypePrecision(type));
    }

    private void writeInterfaceBlock(@NonNull InterfaceBlock block) {
        Type blockType = block.getVariable().getType().getElementType();
        boolean hasNonBuiltin = false;
        for (var f : blockType.getFields()) {
            if ((f.modifiers().layoutFlags() & Layout.kBuiltin_LayoutFlag) == 0) {
                hasNonBuiltin = true;
                break;
            }
        }
        if (!hasNonBuiltin) {
            // Blocks that only contain builtin variables do not need to exist
            return;
        }
        writeModifiers(block.getVariable().getModifiers());
        writeType(blockType);
        writeLine(" {");
        mIndentation++;
        for (var f : blockType.getFields()) {
            if ((f.modifiers().layoutFlags() & Layout.kBuiltin_LayoutFlag) == 0) {
                writeModifiers(f.modifiers());
                writeTypePrecision(f.type());
                writeType(f.type());
                write(' ');
                writeIdentifier(f.name());
                writeLine(';');
            }
        }
        mIndentation--;
        write('}');
        if (!block.getInstanceName().isEmpty()) {
            write(' ');
            writeIdentifier(block.getInstanceName());
            if (block.getArraySize() > 0) {
                write('[');
                write(Integer.toString(block.getArraySize()));
                write(']');
            }
        }
        writeLine(';');
    }
}
