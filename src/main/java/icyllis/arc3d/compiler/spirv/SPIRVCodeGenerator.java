/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler.spirv;

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.compiler.tree.*;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;

import static org.lwjgl.util.spvc.Spv.*;

/**
 * SPIR-V code generator for OpenGL 4.6 and Vulkan 1.1.
 */
public final class SPIRVCodeGenerator extends CodeGenerator implements Output {

    // Arc 3D is not registered, so higher 16 bits are zero
    // we use 0x32D2 and 0x6D5C for the lower 16 bits
    public static final int GENERATOR_MAGIC_NUMBER = 0x00000000;

    public final SPIRVTarget mOutputTarget;
    public final SPIRVVersion mOutputVersion;

    private WordBuffer mConstantBuffer;
    private WordBuffer mDecorationBuffer;

    private IdentityHashMap<Type, Integer> mStructTable = new IdentityHashMap<>();
    private IdentityHashMap<FunctionDecl, Integer> mFunctionTable = new IdentityHashMap<>();
    private IdentityHashMap<Variable, Integer> mVariableTable = new IdentityHashMap<>();

    private IntSet mCapabilities = new IntOpenHashSet(16, 0.5f);

    // id 0 is reserved
    private int mIdCount = 1;
    private int mGLSLExtendedInstructions;

    private boolean mEmitNames = false;

    public SPIRVCodeGenerator(Context context,
                              TranslationUnit translationUnit,
                              SPIRVTarget outputTarget,
                              SPIRVVersion outputVersion) {
        super(context, translationUnit);
        mOutputTarget = outputTarget;
        mOutputVersion = outputVersion;
    }

    /**
     * Emit name strings for variables, functions, user-defined types, and members.
     * This has no semantic impact (debug only). The default is false.
     */
    public void setEmitNames(boolean emitNames) {
        mEmitNames = emitNames;
    }

    @Nonnull
    @Override
    public ByteBuffer generateCode() {
        assert mContext.getErrorHandler().errorCount() == 0;
        // Header
        // 0 - magic number
        // 1 - version number
        // 2 - generator magic
        // 3 - bound (set later)
        // 4 - schema (reserved)
        mBuffer = BufferUtils.createByteBuffer(1024)
                .putInt(SpvMagicNumber)
                .putInt(mOutputVersion.mVersionNumber)
                .putInt(GENERATOR_MAGIC_NUMBER)
                .putInt(0)
                .putInt(0);

        writeInstructions();

        writeCapabilities(this);
        writeInstruction(SpvOpExtInstImport, mGLSLExtendedInstructions, "GLSL.std.450", this);
        writeInstruction(SpvOpMemoryModel, SpvAddressingModelLogical, SpvMemoryModelGLSL450, this);


        ByteBuffer buffer = mBuffer.putInt(12, mIdCount); // set bound
        mBuffer = null;
        return buffer.flip();
    }

    @Override
    public void writeWord(int word) {
        grow(mBuffer.limit() + 4)
                .putInt(word);
    }

    @Override
    public void writeWords(int[] words, int size) {
        // int array is in host endianness (native byte order)
        grow(mBuffer.limit() + (size << 2))
                .asIntBuffer()
                .put(words, 0, size);
    }

    @Override
    public void writeString8(String s) {
        int len = s.length();
        ByteBuffer buffer = grow(mBuffer.limit() +
                (len + 4 & -4)); // +1 null-terminator
        int word = 0;
        int shift = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == 0 || c >= 0x80) {
                throw new AssertionError(c);
            }
            word |= c << shift;
            shift += 8;
            if (shift == 32) {
                buffer.putInt(word);
                word = 0;
                shift = 0;
            }
        }
        // null-terminator and padding
        buffer.putInt(word);
    }

    private void writeCapabilities(Output output) {
        for (var it = mCapabilities.iterator(); it.hasNext(); ) {
            writeInstruction(SpvOpCapability, it.nextInt(),
                    output);
        }
        writeInstruction(SpvOpCapability, SpvCapabilityShader,
                output);
    }

    private int getUniqueId(@Nonnull Type type) {
        return getUniqueId(type.isRelaxedPrecision());
    }

    private int getUniqueId(boolean relaxedPrecision) {
        int id = getUniqueId();
        if (relaxedPrecision) {
            writeInstruction(SpvOpDecorate, id, SpvDecorationRelaxedPrecision,
                    mDecorationBuffer);
        }
        return id;
    }

    private int getUniqueId() {
        return mIdCount++;
    }

    private int getType(Type type) {
        return 0;
    }

    private void writeLayout(@Nonnull Layout layout, int target, int pos) {
        boolean isPushConstant = (layout.layoutFlags() & Layout.kPushConstant_LayoutFlag) != 0;
        if (layout.mLocation >= 0) {
            writeInstruction(SpvOpDecorate, target, SpvDecorationLocation,
                    layout.mLocation, mDecorationBuffer);
        }
        if (layout.mComponent >= 0) {
            writeInstruction(SpvOpDecorate, target, SpvDecorationComponent,
                    layout.mComponent, mDecorationBuffer);
        }
        if (layout.mIndex >= 0) {
            writeInstruction(SpvOpDecorate, target, SpvDecorationIndex,
                    layout.mIndex, mDecorationBuffer);
        }
        if (layout.mBinding >= 0) {
            if (isPushConstant) {
                mContext.error(pos, "Can't apply 'binding' to push constants");
            } else {
                writeInstruction(SpvOpDecorate, target, SpvDecorationBinding,
                        layout.mBinding, mDecorationBuffer);
            }
        }
        if (layout.mSet >= 0) {
            if (isPushConstant) {
                mContext.error(pos, "Can't apply 'set' to push constants");
            } else {
                writeInstruction(SpvOpDecorate, target, SpvDecorationDescriptorSet,
                        layout.mSet, mDecorationBuffer);
            }
        }
        if (layout.mInputAttachmentIndex >= 0) {
            writeInstruction(SpvOpDecorate, target, SpvDecorationInputAttachmentIndex,
                    layout.mInputAttachmentIndex, mDecorationBuffer);
            mCapabilities.add(SpvCapabilityInputAttachment);
        }
        if (layout.mBuiltin >= 0) {
            writeInstruction(SpvOpDecorate, target, SpvDecorationBuiltIn,
                    layout.mBuiltin, mDecorationBuffer);
        }
    }

    private void writeInstructions() {
        mGLSLExtendedInstructions = getUniqueId();
    }

    private void writeOpcode(int opcode, int count, Output output) {
        output.writeWord((count << 16) | opcode);
    }

    private void writeInstruction(int opcode, Output output) {
        writeOpcode(opcode, 1, output);
    }

    private void writeInstruction(int opcode, int word1, Output output) {
        writeOpcode(opcode, 2, output);
        output.writeWord(word1);
    }

    private void writeInstruction(int opcode, int word1, int word2,
                                  Output output) {
        writeOpcode(opcode, 3, output);
        output.writeWord(word1);
        output.writeWord(word2);
    }

    private void writeInstruction(int opcode, int word1, String string,
                                  Output output) {
        writeOpcode(opcode, 2 + (string.length() + 4 >> 2), output);
        output.writeWord(word1);
        output.writeString8(string);
    }

    private void writeInstruction(int opcode, int word1, int word2,
                                  int word3, Output output) {
        writeOpcode(opcode, 4, output);
        output.writeWord(word1);
        output.writeWord(word2);
        output.writeWord(word3);
    }

    private void writeInstruction(int opcode, int word1, int word2,
                                  int word3, int word4, Output output) {
        writeOpcode(opcode, 5, output);
        output.writeWord(word1);
        output.writeWord(word2);
        output.writeWord(word3);
        output.writeWord(word4);
    }

    private void writeInstruction(int opcode, int word1, int word2,
                                  int word3, int word4, int word5,
                                  Output output) {
        writeOpcode(opcode, 6, output);
        output.writeWord(word1);
        output.writeWord(word2);
        output.writeWord(word3);
        output.writeWord(word4);
        output.writeWord(word5);
    }

    private void writeInstruction(int opcode, int word1, int word2,
                                  int word3, int word4, int word5,
                                  int word6, Output output) {
        writeOpcode(opcode, 7, output);
        output.writeWord(word1);
        output.writeWord(word2);
        output.writeWord(word3);
        output.writeWord(word4);
        output.writeWord(word5);
        output.writeWord(word6);
    }

    private void writeInstruction(int opcode, int word1, int word2,
                                  int word3, int word4, int word5,
                                  int word6, int word7, Output output) {
        writeOpcode(opcode, 8, output);
        output.writeWord(word1);
        output.writeWord(word2);
        output.writeWord(word3);
        output.writeWord(word4);
        output.writeWord(word5);
        output.writeWord(word6);
        output.writeWord(word7);
    }

    private void writeInstruction(int opcode, int word1, int word2,
                                  int word3, int word4, int word5,
                                  int word6, int word7, int word8,
                                  Output output) {
        writeOpcode(opcode, 9, output);
        output.writeWord(word1);
        output.writeWord(word2);
        output.writeWord(word3);
        output.writeWord(word4);
        output.writeWord(word5);
        output.writeWord(word6);
        output.writeWord(word7);
        output.writeWord(word8);
    }
}
