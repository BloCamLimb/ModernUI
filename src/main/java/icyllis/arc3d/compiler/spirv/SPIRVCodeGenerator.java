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

package icyllis.arc3d.compiler.spirv;

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.compiler.analysis.Analysis;
import icyllis.arc3d.compiler.tree.*;
import icyllis.arc3d.core.MathUtil;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Objects;

import static org.lwjgl.util.spvc.Spv.*;

/**
 * SPIR-V code generator for OpenGL 4.5 and Vulkan 1.0 or above.
 * <p>
 * A SPIR-V module is a stream of uint32 words, the generated code is in host endianness,
 * which can be little-endian or big-endian.
 */
public final class SPIRVCodeGenerator extends CodeGenerator {

    // Arc3D is not registered, so higher 16 bits are zero
    // We use 0x32D2 and 0x6D5C for the lower 16 bits (in the future)
    public static final int GENERATOR_MAGIC_NUMBER = 0x00000000;
    // We reserve the max SpvId as a sentinel (meaning not available)
    // SpvId 0 is reserved by SPIR-V and also represents absence in map
    public static final int NONE_ID = 0xFFFFFFFF;

    public final TargetApi mOutputTarget;
    public final SPIRVVersion mOutputVersion;

    private final WordBuffer mNameBuffer = new WordBuffer();
    private final WordBuffer mConstantBuffer = new WordBuffer();
    private final WordBuffer mDecorationBuffer = new WordBuffer();
    private final WordBuffer mFunctionBuffer = new WordBuffer();
    private final WordBuffer mGlobalInitBuffer = new WordBuffer();
    // reusable buffers
    private final WordBuffer mVariableBuffer = new WordBuffer();
    private final WordBuffer mBodyBuffer = new WordBuffer();

    // key is a pointer to symbol table, hash is based on address (reference equality)
    // struct type to SpvId[MemoryLayout.ordinal + 1], no memory layout is at [0]
    private final HashMap<Type, int[]> mStructTable = new HashMap<>();
    private final Object2IntOpenHashMap<FunctionDecl> mFunctionTable = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<Variable> mVariableTable = new Object2IntOpenHashMap<>();

    // a stack of instruction builders
    // used to write nested types or structures
    private final InstructionBuilder[] mInstBuilderPool = new InstructionBuilder[Type.kMaxNestingDepth + 2];
    private int mInstBuilderPoolSize = 0;

    // A map of instruction -> SpvId:
    private final Object2IntOpenHashMap<Instruction> mOpCache = new Object2IntOpenHashMap<>();
    // A map of SpvId -> instruction:
    private final Int2ObjectOpenHashMap<Instruction> mSpvIdCache = new Int2ObjectOpenHashMap<>();
    // A map of SpvId -> value SpvId:
    final Int2IntOpenHashMap mStoreCache = new Int2IntOpenHashMap();

    // "Reachable" ops are instructions which can safely be accessed from the current block.
    // For instance, if our SPIR-V contains `%3 = OpFAdd %1 %2`, we would be able to access and
    // reuse that computation on following lines. However, if that Add operation occurred inside an
    // `if` block, then its SpvId becomes inaccessible once we complete the if statement (since
    // depending on the if condition, we may or may not have actually done that computation). The
    // same logic applies to other control-flow blocks as well. Once an instruction becomes
    // unreachable, we remove it from both op-caches.
    private final IntArrayList mReachableOps = new IntArrayList();

    // The "store-ops" list contains a running list of all the pointers in the store cache. If a
    // store occurs inside of a conditional block, once that block exits, we no longer know what is
    // stored in that particular SpvId. At that point, we must remove any associated entry from the
    // store cache.
    private final IntArrayList mStoreOps = new IntArrayList();

    // label of the current block, or 0 if we are not in a block
    private int mCurrentBlock = 0;

    // Use "BranchIsAbove" for labels which are referenced by OpBranch or OpBranchConditional
    // ops that are above the label in the code--i.e., the branch skips forward in the code.
    private static final int kBranchIsAbove = 0,

    // Use "BranchIsBelow" for labels which are referenced by OpBranch or OpBranchConditional
    // ops below the label in the code--i.e., the branch jumps backward in the code.
    kBranchIsBelow = 1,

    // Use "BranchesOnBothSides" for labels which have branches coming from both directions.
    kBranchesOnBothSides = 2;

    // our generator tracks a single access chain, like, a.b[c].d
    private final IntArrayList mAccessChain = new IntArrayList();

    private final IntOpenHashSet mCapabilities = new IntOpenHashSet(16, 0.5f);

    private final IntArrayList mInterfaceVariables = new IntArrayList();

    private FunctionDecl mEntryPointFunction;

    // SpvId 0 is reserved
    private int mIdCount = 1;
    private int mGLSLExtendedInstructions;

    /*
     * Emit name strings for variables, functions, user-defined types, and members.
     * This has no semantic impact (debug only). The default is false.
     */
    private boolean mEmitNames;

    // reused array storing SpvId
    private final IntArrayList mTmpIdList = new IntArrayList();

    public SPIRVCodeGenerator(@Nonnull ShaderCompiler compiler,
                              @Nonnull TranslationUnit translationUnit,
                              @Nonnull ShaderCaps shaderCaps) {
        super(compiler, translationUnit);
        mOutputTarget = Objects.requireNonNullElse(shaderCaps.mTargetApi, TargetApi.VULKAN_1_0);
        mOutputVersion = Objects.requireNonNullElse(shaderCaps.mSPIRVVersion, SPIRVVersion.SPIRV_1_0);
    }

    @Nullable
    @Override
    public ByteBuffer generateCode() {
        assert getContext().getErrorHandler().errorCount() == 0;

        if (mOutputTarget.isOpenGLES()) {
            getContext().error(Position.NO_POS, "OpenGL ES is not a valid client API for SPIR-V");
            return null;
        }
        ShaderKind kind = mTranslationUnit.getKind();
        if (!kind.isVertex() && !kind.isFragment() && !kind.isCompute()) {
            getContext().error(Position.NO_POS, "shader kind " + kind + " is not executable in SPIR-V");
            return null;
        }

        mEmitNames = !getContext().getOptions().mMinifyNames;
        // build into word buffers
        buildInstructions(mTranslationUnit);

        if (getContext().getErrorHandler().errorCount() != 0) {
            return null;
        }

        String entryPointName = mEntryPointFunction.getName();

        // estimate code size in bytes
        int estimatedSize = 20;
        estimatedSize += mCapabilities.size() * 2 * 4;
        estimatedSize += 24; // ExtInstImport
        estimatedSize += 12; // MemoryModel
        int entryPointWordCount = 3 + (entryPointName.length() + 4) / 4 + mInterfaceVariables.size();
        estimatedSize += entryPointWordCount * 4;
        estimatedSize += mNameBuffer.size() * 4;
        estimatedSize += mDecorationBuffer.size() * 4;
        estimatedSize += mConstantBuffer.size() * 4;
        estimatedSize += mFunctionBuffer.size() * 4;
        estimatedSize = estimatedSize + 12; // ExecutionMode

        // Header
        // 0 - magic number
        // 1 - version number
        // 2 - generator magic
        // 3 - bound
        // 4 - schema (reserved)
        BufferWriter writer = new BufferWriter(estimatedSize);
        writer.writeWord(SpvMagicNumber);
        writer.writeWord(mOutputVersion.mVersionNumber);
        writer.writeWord(GENERATOR_MAGIC_NUMBER);
        writer.writeWord(mIdCount);
        writer.writeWord(0);

        for (var it = mCapabilities.iterator(); it.hasNext(); ) {
            writeInstruction(SpvOpCapability, it.nextInt(), writer);
        }
        writeInstruction(SpvOpExtInstImport, mGLSLExtendedInstructions, "GLSL.std.450", writer);
        writeInstruction(SpvOpMemoryModel, SpvAddressingModelLogical, SpvMemoryModelGLSL450, writer);
        writeOpcode(SpvOpEntryPoint, entryPointWordCount, writer);
        if (kind.isVertex()) {
            writer.writeWord(SpvExecutionModelVertex);
        } else if (kind.isFragment()) {
            writer.writeWord(SpvExecutionModelFragment);
        } else if (kind.isCompute()) {
            writer.writeWord(SpvExecutionModelGLCompute);
        }

        int entryPoint = mFunctionTable.getInt(mEntryPointFunction);
        writer.writeWord(entryPoint);
        writer.writeString8(getContext(), entryPointName);
        for (int id : mInterfaceVariables) {
            writer.writeWord(id);
        }

        if (kind.isFragment()) {
            writeInstruction(SpvOpExecutionMode,
                    entryPoint,
                    mOutputTarget.isOpenGL()
                            ? SpvExecutionModeOriginLowerLeft
                            : SpvExecutionModeOriginUpperLeft,
                    writer);
        }
        writer.writeWords(mNameBuffer.elements(), mNameBuffer.size());
        writer.writeWords(mDecorationBuffer.elements(), mDecorationBuffer.size());
        writer.writeWords(mConstantBuffer.elements(), mConstantBuffer.size());
        writer.writeWords(mFunctionBuffer.elements(), mFunctionBuffer.size());

        if (getContext().getErrorHandler().errorCount() != 0) {
            return null;
        }
        return writer.detach();
    }

    private int getStorageClass(@Nonnull Variable variable) {
        Type type = variable.getType();
        if (type.isArray()) {
            type = type.getElementType();
        }
        if (type.isOpaque()) {
            return SpvStorageClassUniformConstant;
        }
        Modifiers modifiers = variable.getModifiers();
        if (variable.getStorage() == Variable.kGlobal_Storage) {
            if ((modifiers.flags() & Modifiers.kIn_Flag) != 0) {
                return SpvStorageClassInput;
            }
            if ((modifiers.flags() & Modifiers.kOut_Flag) != 0) {
                return SpvStorageClassOutput;
            }
        }
        if (modifiers.isBuffer() &&
                mOutputVersion.isAtLeast(SPIRVVersion.SPIRV_1_3)) {
            // missing before 1.3
            return SpvStorageClassStorageBuffer;
        }
        if ((modifiers.flags() & (Modifiers.kUniform_Flag | Modifiers.kBuffer_Flag)) != 0) {
            if ((modifiers.layoutFlags() & Layout.kPushConstant_LayoutFlag) != 0) {
                return SpvStorageClassPushConstant;
            }
            if (type.isInterfaceBlock()) {
                return SpvStorageClassUniform;
            }
            if (!mOutputTarget.isOpenGL()) {
                getContext().error(variable.mPosition, "uniform variables at global scope are not allowed");
            }
            return SpvStorageClassUniformConstant;
        }
        if ((modifiers.flags() & Modifiers.kWorkgroup_Flag) != 0) {
            return SpvStorageClassWorkgroup;
        }
        return variable.getStorage() == Variable.kGlobal_Storage
                ? SpvStorageClassPrivate
                : SpvStorageClassFunction;
    }

    private int getStorageClass(@Nonnull Expression expr) {
        switch (expr.getKind()) {
            case VARIABLE_REFERENCE -> {
                return getStorageClass(((VariableReference) expr).getVariable());
            }
            case INDEX -> {
                return getStorageClass(((IndexExpression) expr).getBase());
            }
            case FIELD_ACCESS -> {
                return getStorageClass(((FieldAccess) expr).getBase());
            }
            default -> {
                return SpvStorageClassFunction;
            }
        }
    }

    int getUniqueId(@Nonnull Type type) {
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

    @Nullable
    private Instruction resultTypeForInstruction(@Nonnull Instruction inst) {
        // This list should contain every op that we cache that has a result and result-type.
        // (If one is missing, we will not find some optimization opportunities.)
        // Generally, the result type of an op is in the 0th word, but I'm not sure if this is
        // universally true, so it's configurable on a per-op basis.
        int resultTypeWord;
        switch (inst.mOpcode) {
            case SpvOpConstant:
            case SpvOpConstantTrue:
            case SpvOpConstantFalse:
            case SpvOpConstantComposite:
            case SpvOpCompositeConstruct:
            case SpvOpCompositeExtract:
            case SpvOpLoad:
                resultTypeWord = 0;
                break;

            default:
                return null;
        }

        Instruction typeInst = mSpvIdCache.get(inst.mWords[resultTypeWord]);
        assert (typeInst != null);
        return typeInst;
    }

    private int numComponentsForVecInstruction(Instruction inst) {
        // If an instruction is in the op cache, its type should be as well.
        Instruction typeInst = resultTypeForInstruction(inst);
        assert (typeInst != null);
        assert (typeInst.mOpcode == SpvOpTypeVector || typeInst.mOpcode == SpvOpTypeFloat ||
                typeInst.mOpcode == SpvOpTypeInt || typeInst.mOpcode == SpvOpTypeBool);

        // For vectors, extract their column count. Scalars have one component by definition.
        //   SpvOpTypeVector ResultID ComponentType NumComponents
        return (typeInst.mOpcode == SpvOpTypeVector)
                ? typeInst.mWords[2]
                : 1;
    }

    // Extracts the requested component SpvId from a composite instruction, if it can be done.
    private int getComponent(int id, int index) {
        Instruction inst = mSpvIdCache.get(id);
        if (inst == null) {
            return NONE_ID;
        }

        if (inst.mOpcode == SpvOpConstantComposite) {
            // SpvOpConstantComposite ResultType ResultID [components...]
            // Add 2 to the component index to skip past ResultType and ResultID.
            return inst.mWords[2 + index];
        }
        if (inst.mOpcode == SpvOpCompositeConstruct) {
            // SpvOpCompositeConstruct ResultType ResultID [components...]
            // Vectors have special rules; check to see if we are composing a vector.
            Instruction composedType = mSpvIdCache.get(inst.mWords[0]);
            assert (composedType != null);

            // When composing a non-vector, each instruction word maps 1:1 to the component index.
            // We can just extract out the associated component directly.
            if (composedType.mOpcode != SpvOpTypeVector) {
                return inst.mWords[2 + index];
            }

            // When composing a vector, components can be either scalars or vectors.
            // This means we need to check the op type on each component. (+2 to skip ResultType/Result)
            for (int i = 2; i < inst.mWords.length; ++i) {
                int currentWord = inst.mWords[i];

                // Retrieve the sub-instruction pointed to by OpCompositeConstruct.
                Instruction subInst = mSpvIdCache.get(currentWord);
                if (subInst == null) {
                    return NONE_ID;
                }
                // If this subinstruction contains the component we're looking for...
                int numComponents = numComponentsForVecInstruction(subInst);
                if (index < numComponents) {
                    if (numComponents == 1) {
                        // ... it's a scalar. Return it.
                        assert (index == 0);
                        return currentWord;
                    } else {
                        // ... it's a vector. Recurse into it.
                        return getComponent(currentWord, index);
                    }
                }
                // This sub-instruction doesn't contain our component. Keep walking forward.
                index -= numComponents;
            }
            assert false : ("component index goes past the end of this composite value");
            return NONE_ID;
        }
        return NONE_ID;
    }

    // Converts the provided SpvId into an array of scalar OpConstants, if it can be done.
    private boolean getConstants(int value,
                                 IntArrayList constants) {
        Instruction inst = mSpvIdCache.get(value);
        if (inst == null) {
            return false;
        }
        return switch (inst.mOpcode) {
            case SpvOpConstant, SpvOpConstantTrue, SpvOpConstantFalse -> {
                constants.add(value);
                yield true;
            }
            case SpvOpConstantComposite -> {
                // OpConstantComposite ResultType ResultID Constituents...
                // Start at word 2 to skip past ResultType and ResultID.
                for (int i = 2; i < inst.mWords.length; ++i) {
                    if (!getConstants(inst.mWords[i], constants)) {
                        yield false;
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    /**
     * For {@link #writeInstructionWithCache(InstructionBuilder, Writer)}.
     */
    private InstructionBuilder getInstBuilder(int opcode) {
        if (mInstBuilderPoolSize == 0) {
            return new InstructionBuilder(opcode);
        }
        return mInstBuilderPool[--mInstBuilderPoolSize]
                .reset(opcode);
    }

    /**
     * Make type with cache, for types other than opaque types and interface blocks.
     */
    int writeType(@Nonnull Type type) {
        return writeType(type, null, null);
    }

    /**
     * Make type with cache.
     *
     * @param modifiers    non-null for opaque types, otherwise null
     * @param memoryLayout non-null for uniform and shader storage blocks, and their members, otherwise null
     */
    private int writeType(@Nonnull Type type,
                          @Nullable Modifiers modifiers,
                          @Nullable MemoryLayout memoryLayout) {
        return switch (type.getTypeKind()) {
            case Type.kVoid_TypeKind -> writeInstructionWithCache(
                    getInstBuilder(SpvOpTypeVoid)
                            .addResult(),
                    mConstantBuffer
            );
            case Type.kScalar_TypeKind -> {
                if (type.isBoolean()) {
                    yield writeInstructionWithCache(
                            getInstBuilder(SpvOpTypeBool)
                                    .addResult(),
                            mConstantBuffer
                    );
                } else if (type.isInteger()) {
                    assert type.getWidth() == 32;
                    yield writeInstructionWithCache(
                            getInstBuilder(SpvOpTypeInt)
                                    .addResult()
                                    .addWord(type.getWidth())
                                    .addWord(type.isSigned() ? 1 : 0),
                            mConstantBuffer
                    );
                } else if (type.isFloat()) {
                    assert type.getWidth() == 32;
                    yield writeInstructionWithCache(
                            getInstBuilder(SpvOpTypeFloat)
                                    .addResult()
                                    .addWord(type.getWidth()),
                            mConstantBuffer
                    );
                } else {
                    throw new AssertionError();
                }
            }
            case Type.kVector_TypeKind -> {
                int scalarTypeId = writeType(type.getElementType(), modifiers, memoryLayout);
                yield writeInstructionWithCache(
                        getInstBuilder(SpvOpTypeVector)
                                .addResult()
                                .addWord(scalarTypeId)
                                .addWord(type.getRows()),
                        mConstantBuffer
                );
            }
            case Type.kMatrix_TypeKind -> {
                int vectorTypeId = writeType(type.getElementType(), modifiers, memoryLayout);
                yield writeInstructionWithCache(
                        getInstBuilder(SpvOpTypeMatrix)
                                .addResult()
                                .addWord(vectorTypeId)
                                .addWord(type.getCols()),
                        mConstantBuffer
                );
            }
            case Type.kArray_TypeKind -> {
                final int stride;
                if (memoryLayout != null) {
                    if (!memoryLayout.isSupported(type)) {
                        getContext().error(type.mPosition, "type '" + type +
                                "' is not permitted here");
                        yield NONE_ID;
                    }
                    stride = memoryLayout.stride(type);
                    assert stride > 0;
                } else {
                    stride = 0;
                }
                int elementTypeId = writeType(type.getElementType(), modifiers, memoryLayout);
                final int resultId;
                if (type.isUnsizedArray()) {
                    resultId = writeInstructionWithCache(
                            getInstBuilder(SpvOpTypeRuntimeArray)
                                    .addKeyedResult(stride)
                                    .addWord(elementTypeId),
                            mConstantBuffer
                    );
                } else {
                    int arraySize = type.getArraySize();
                    if (arraySize <= 0) {
                        getContext().error(type.mPosition, "array size must be positive");
                        yield NONE_ID;
                    }
                    int arraySizeId = writeScalarConstant(arraySize, getContext().getTypes().mInt);
                    resultId = writeInstructionWithCache(
                            getInstBuilder(SpvOpTypeArray)
                                    .addKeyedResult(stride)
                                    .addWord(elementTypeId)
                                    .addWord(arraySizeId),
                            mConstantBuffer
                    );
                }
                if (stride > 0) {
                    writeInstructionWithCache(
                            getInstBuilder(SpvOpDecorate)
                                    .addWord(resultId)
                                    .addWord(SpvDecorationArrayStride)
                                    .addWord(stride),
                            mDecorationBuffer
                    );
                }
                yield resultId;
            }
            case Type.kStruct_TypeKind -> writeStruct(type, memoryLayout);
            default -> {
                // compiler defines some intermediate types
                getContext().error(type.mPosition, "type '" + type +
                        "' is not permitted here");
                yield NONE_ID;
            }
        };
    }

    /**
     * Make struct-like type with fast cache, nested structures are allowed.
     * <p>
     * This method ensures that a structure can have different layouts, with deduplication.
     * Structures that do not require layout can reuse structure types with any layout.
     *
     * @param memoryLayout non-null for uniform and shader storage blocks, null for others
     * @return structure type id
     */
    private int writeStruct(@Nonnull Type type,
                            @Nullable MemoryLayout memoryLayout) {
        assert type.isStruct();
        int[] cached = mStructTable.computeIfAbsent(type,
                // no memory layout at [0]
                // others at [ordinal + 1]
                // total number = max ordinal + 2
                __ -> new int[MemoryLayout.Scalar.ordinal() + 2]);
        int slot = memoryLayout == null
                ? 0
                : memoryLayout.ordinal() + 1;
        int resultId = cached[slot];
        if (resultId != 0) {
            return resultId;
        }
        //TODO may not be correct for structs in blocks
        if (slot == 0) {
            // a structure that does not require layout and can match any structure with layout
            for (int i = 1; i < cached.length; i++) {
                resultId = cached[i];
                if (resultId != 0) {
                    return resultId;
                }
            }
        } else {
            // requires layout, try to reuse the one without layout
            resultId = cached[0];
            if (resultId != 0) {
                // a structure can have different layouts, so clear this default
                // since we are going to decorate it
                cached[0] = 0;
            }
        }

        var fields = type.getFields();
        final boolean unique;
        if (resultId == 0) {
            // cannot reuse
            unique = true;
            var builder = getInstBuilder(SpvOpTypeStruct)
                    .addUniqueResult();
            for (var f : fields) {
                // use builder stack
                int typeId = writeType(f.type(), f.modifiers(), memoryLayout);
                builder.addWord(typeId);
            }
            resultId = writeInstructionWithCache(builder, mConstantBuffer);
            if (mEmitNames) {
                writeInstruction(SpvOpName, resultId, type.getName(), mNameBuffer);
            }
        } else {
            unique = false;
        }
        // cache it
        cached[slot] = resultId;

        int offset = 0;
        int[] size = new int[3]; // size, matrix stride and array stride
        for (int i = 0; i < fields.length; i++) {
            var field = fields[i];
            if (unique) {
                if (mEmitNames) {
                    writeInstruction(SpvOpMemberName, resultId, i, field.name(), mNameBuffer);
                }
                if (field.type().isRelaxedPrecision()) {
                    writeInstruction(SpvOpMemberDecorate, resultId, i,
                            SpvDecorationRelaxedPrecision, mDecorationBuffer);
                }
                writeFieldModifiers(field.modifiers(), resultId, i);
            }

            // layout will never be unique
            if (memoryLayout != null) {
                if (!memoryLayout.isSupported(field.type())) {
                    getContext().error(field.position(), "type '" + field.type() +
                            "' is not permitted here");
                    return resultId;
                }

                int alignment = memoryLayout.alignment(field.type(), size);

                int fieldOffset = field.modifiers().layoutOffset();
                if (fieldOffset >= 0) {
                    // must be in declaration order
                    if (fieldOffset < offset) {
                        getContext().error(field.position(), "offset of field '" +
                                field.name() + "' must be at least " + offset);
                    }
                    if ((fieldOffset & alignment - 1) != 0) {
                        getContext().error(field.position(), "offset of field '" +
                                field.name() + "' must round up to " + alignment);
                    }
                    offset = fieldOffset;
                } else {
                    offset = MathUtil.alignTo(offset, alignment);
                }

                if (field.modifiers().layoutBuiltin() >= 0) {
                    getContext().error(field.position(), "builtin field '" + field.name() +
                            "' cannot be explicitly laid out.");
                } else {
                    writeInstruction(SpvOpMemberDecorate, resultId, i,
                            SpvDecorationOffset, offset, mDecorationBuffer);
                }

                int matrixStride = size[1];
                if (matrixStride > 0) {
                    // matrix or array-of-matrices
                    assert field.type().isMatrix() ||
                            (field.type().isArray() && field.type().getElementType().isMatrix());
                    writeInstruction(SpvOpMemberDecorate, resultId, i,
                            SpvDecorationColMajor, mDecorationBuffer);
                    writeInstruction(SpvOpMemberDecorate, resultId, i,
                            SpvDecorationMatrixStride, matrixStride, mDecorationBuffer);
                }

                // end padding of matrix, array and struct is already included
                offset += size[0];
            }
        }

        return resultId;
    }

    private int writeFunction(@Nonnull FunctionDefinition func, Writer writer) {
        int numReachableOps = mReachableOps.size();
        int numStoreOps = mStoreOps.size();

        mVariableBuffer.clear();
        int result = writeFunctionDecl(func.getFunctionDecl(), writer);
        mCurrentBlock = 0;
        writeLabel(getUniqueId(), writer);
        mBodyBuffer.clear();
        writeBlock(func.getBody(), mBodyBuffer);
        writer.writeWords(mVariableBuffer.elements(), mVariableBuffer.size());
        if (func.getFunctionDecl().isEntryPoint()) {
            writer.writeWords(mGlobalInitBuffer.elements(), mGlobalInitBuffer.size());
        }
        writer.writeWords(mBodyBuffer.elements(), mBodyBuffer.size());

        if (mCurrentBlock != 0) {
            if (func.getFunctionDecl().getReturnType().isVoid()) {
                writeInstruction(SpvOpReturn, writer);
            } else {
                writeInstruction(SpvOpUnreachable, writer);
            }
        }
        writeInstruction(SpvOpFunctionEnd, writer);
        pruneConditionalOps(numReachableOps, numStoreOps);
        return result;
    }

    private int writeFunctionDecl(@Nonnull FunctionDecl decl, Writer writer) {
        int result = mFunctionTable.getInt(decl);
        int returnTypeId = writeType(decl.getReturnType());
        int functionTypeId = writeFunctionType(decl);
        writeInstruction(SpvOpFunction, returnTypeId, result,
                SpvFunctionControlMaskNone, functionTypeId, writer);
        if (mEmitNames) {
            String mangledName = decl.getMangledName();
            if (mangledName.length() <= 5200) {
                writeInstruction(SpvOpName,
                        result,
                        mangledName,
                        mNameBuffer);
            }
        }
        for (Variable parameter : decl.getParameters()) {
            int id = getUniqueId();
            mVariableTable.put(parameter, id);

            int type = writeFunctionParameterType(parameter.getType(), parameter.getModifiers());
            writeInstruction(SpvOpFunctionParameter, type, id, writer);
        }
        return result;
    }

    private int writeFunctionType(@Nonnull FunctionDecl function) {
        int returnTypeId = writeType(function.getReturnType());
        var builder = getInstBuilder(SpvOpTypeFunction)
                .addResult()
                .addWord(returnTypeId);
        for (var parameter : function.getParameters()) {
            // use builder stack
            int typeId = writeFunctionParameterType(parameter.getType(),
                    parameter.getModifiers());
            builder.addWord(typeId);
        }
        return writeInstructionWithCache(builder, mConstantBuffer);
    }

    private int writeFunctionParameterType(@Nonnull Type paramType,
                                           @Nullable Modifiers paramMods) {
        //TODO rvalue, block pointer
        int storageClass;
        if (paramType.isOpaque()) {
            storageClass = SpvStorageClassUniformConstant;
        } else {
            storageClass = SpvStorageClassFunction;
        }
        return writePointerType(paramType,
                paramMods,
                null,
                storageClass);
    }

    private int writePointerType(@Nonnull Type type,
                                 int storageClass) {
        return writePointerType(type, null, null, storageClass);
    }

    private int writePointerType(@Nonnull Type type,
                                 @Nullable Modifiers modifiers,
                                 @Nullable MemoryLayout memoryLayout,
                                 int storageClass) {
        int typeId = writeType(type, modifiers, memoryLayout);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpTypePointer)
                        .addResult()
                        .addWord(storageClass)
                        .addWord(typeId),
                mConstantBuffer
        );
    }

    private int writeOpCompositeExtract(@Nonnull Type type,
                                        int base,
                                        int index,
                                        Writer writer) {
        // If the base op is a composite, we can extract from it directly.
        int result = getComponent(base, index);
        if (result != NONE_ID) {
            return result;
        }
        int typeId = writeType(type);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpCompositeExtract)
                        .addWord(typeId)
                        .addResult()
                        .addWord(base)
                        .addWord(index),
                writer
        );
    }

    private int writeOpCompositeConstruct(@Nonnull Type type,
                                          int[] values, int count,
                                          Writer writer) {
        // If this is a vector/matrix composed entirely of literals, write a constant-composite instead.
        if (type.isVector() || type.isMatrix()) {
            IntArrayList constants = mTmpIdList;
            constants.clear();
            boolean isConstant = true;
            for (int i = 0; i < count; i++) {
                if (!getConstants(values[i], constants)) {
                    isConstant = false;
                    break;
                }
            }
            if (isConstant) {
                if (type.isVector()) {
                    // Create a vector from literals.
                    return writeOpConstantComposite(type, constants.elements(), 0, constants.size());
                } else {
                    // Create each matrix column.
                    assert type.isMatrix();
                    assert constants.size() == type.getComponents();
                    int start = constants.size();
                    Type columnType = type.getComponentType().toVector(getContext(), type.getRows());
                    for (int index = 0; index < type.getCols(); index++) {
                        constants.add(
                                writeOpConstantComposite(columnType,
                                        constants.elements(), index * type.getRows(), type.getRows())
                        );
                    }
                    // Compose the matrix from its columns.
                    return writeOpConstantComposite(type, constants.elements(), start, type.getCols());
                }
            }
        }

        int typeId = writeType(type);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpCompositeConstruct)
                        .addWord(typeId)
                        .addResult()
                        .addWords(values, 0, count),
                writer
        );
    }

    private int writeOpConstantTrue(@Nonnull Type type) {
        assert type.isBoolean();
        int typeId = writeType(type);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpConstantTrue)
                        .addWord(typeId)
                        .addResult(),
                mConstantBuffer
        );
    }

    private int writeOpConstantFalse(@Nonnull Type type) {
        assert type.isBoolean();
        int typeId = writeType(type);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpConstantFalse)
                        .addWord(typeId)
                        .addResult(),
                mConstantBuffer
        );
    }

    private int writeOpConstant(@Nonnull Type type, int valueBits) {
        assert type.isInteger() || type.isFloat();
        assert type.getWidth() == 32;
        int typeId = writeType(type);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpConstant)
                        .addWord(typeId)
                        .addResult()
                        .addWord(valueBits),
                mConstantBuffer
        );
    }

    private int writeOpConstantComposite(@Nonnull Type type,
                                         int[] values, int offset, int count) {
        assert !type.isVector() || count == type.getRows();
        assert !type.isMatrix() || count == type.getCols();
        assert !type.isArray() || count == type.getArraySize();
        assert !type.isStruct() || count == type.getFields().length;
        int typeId = writeType(type);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpConstantComposite)
                        .addWord(typeId)
                        .addResult()
                        .addWords(values, offset, count),
                mConstantBuffer
        );
    }

    /**
     * Make scalar constant with cache, not a literal.
     */
    private int writeScalarConstant(double value, Type type) {
        final int valueBits;
        switch (type.getScalarKind()) {
            case Type.kBoolean_ScalarKind -> {
                return value != 0
                        ? writeOpConstantTrue(type)
                        : writeOpConstantFalse(type);
            }
            case Type.kFloat_ScalarKind -> {
                valueBits = Float.floatToRawIntBits((float) value);
            }
            case Type.kSigned_ScalarKind -> {
                valueBits = (int) value;
            }
            case Type.kUnsigned_ScalarKind -> {
                // bit pattern is two's complement
                valueBits = (int) (long) value;
            }
            default -> throw new AssertionError();
        }
        assert type.getWidth() == 32;
        return writeOpConstant(type, valueBits);
    }

    /**
     * Write decorations.
     */
    private void writeModifiers(@Nonnull Modifiers modifiers, int targetId) {
        Layout layout = modifiers.layout();
        boolean hasLocation = false;
        boolean hasBinding = false;
        int descriptorSet = -1;
        if (layout != null) {
            boolean isPushConstant = (layout.layoutFlags() & Layout.kPushConstant_LayoutFlag) != 0;
            if (layout.mLocation >= 0) {
                writeInstruction(SpvOpDecorate, targetId, SpvDecorationLocation,
                        layout.mLocation, mDecorationBuffer);
                hasLocation = true;
            }
            if (layout.mComponent >= 0) {
                writeInstruction(SpvOpDecorate, targetId, SpvDecorationComponent,
                        layout.mComponent, mDecorationBuffer);
            }
            if (layout.mIndex >= 0) {
                writeInstruction(SpvOpDecorate, targetId, SpvDecorationIndex,
                        layout.mIndex, mDecorationBuffer);
            }
            if (layout.mBinding >= 0) {
                if (isPushConstant) {
                    getContext().error(modifiers.mPosition, "cannot combine 'binding' with 'push_constants'");
                } else {
                    writeInstruction(SpvOpDecorate, targetId, SpvDecorationBinding,
                            layout.mBinding, mDecorationBuffer);
                }
                hasBinding = true;
            }
            if (layout.mSet >= 0) {
                if (isPushConstant) {
                    getContext().error(modifiers.mPosition, "cannot combine 'set' with 'push_constants'");
                } else {
                    writeInstruction(SpvOpDecorate, targetId, SpvDecorationDescriptorSet,
                            layout.mSet, mDecorationBuffer);
                }
                descriptorSet = layout.mSet;
            }
            if (layout.mInputAttachmentIndex >= 0) {
                writeInstruction(SpvOpDecorate, targetId, SpvDecorationInputAttachmentIndex,
                        layout.mInputAttachmentIndex, mDecorationBuffer);
                mCapabilities.add(SpvCapabilityInputAttachment);

            }
            if (layout.mBuiltin >= 0) {
                writeInstruction(SpvOpDecorate, targetId, SpvDecorationBuiltIn,
                        layout.mBuiltin, mDecorationBuffer);
            }
        }

        // in/out variables must have location

        if ((modifiers.flags() & (Modifiers.kUniform_Flag | Modifiers.kBuffer_Flag)) != 0) {
            if (!hasBinding) {
                getContext().warning(modifiers.mPosition, "'binding' is missing");
            }
            if (descriptorSet < 0) {
                if (!mOutputTarget.isOpenGL()) {
                    getContext().error(modifiers.mPosition, "'set' is missing");
                }
            } else if (mOutputTarget.isOpenGL() && descriptorSet != 0) {
                getContext().error(modifiers.mPosition, "'set' must be 0");
            }
        }

        // interpolation qualifiers
        if ((modifiers.flags() & Modifiers.kSmooth_Flag) == 0) {
            // no smooth
            if ((modifiers.flags() & Modifiers.kNoPerspective_Flag) != 0) {
                writeInstruction(SpvOpDecorate, targetId, SpvDecorationNoPerspective,
                        mDecorationBuffer);
            } else if ((modifiers.flags() & Modifiers.kFlat_Flag) != 0) {
                writeInstruction(SpvOpDecorate, targetId, SpvDecorationFlat,
                        mDecorationBuffer);
            }
        }

        // memory qualifiers
        if ((modifiers.flags() & Modifiers.kVolatile_Flag) != 0) {
            writeInstruction(SpvOpDecorate, targetId, SpvDecorationVolatile,
                    mDecorationBuffer);
        }
        if ((modifiers.flags() & (Modifiers.kCoherent_Flag | Modifiers.kVolatile_Flag)) != 0) {
            // volatile is always coherent
            writeInstruction(SpvOpDecorate, targetId, SpvDecorationCoherent,
                    mDecorationBuffer);
        }
        if ((modifiers.flags() & Modifiers.kRestrict_Flag) != 0) {
            writeInstruction(SpvOpDecorate, targetId, SpvDecorationRestrict,
                    mDecorationBuffer);
        }
        if ((modifiers.flags() & Modifiers.kReadOnly_Flag) != 0) {
            writeInstruction(SpvOpDecorate, targetId, SpvDecorationNonWritable,
                    mDecorationBuffer);
        }
        if ((modifiers.flags() & Modifiers.kWriteOnly_Flag) != 0) {
            writeInstruction(SpvOpDecorate, targetId, SpvDecorationNonReadable,
                    mDecorationBuffer);
        }
    }

    /**
     * Write member decorations, excluding offset.
     */
    private void writeFieldModifiers(@Nonnull Modifiers modifiers, int targetId, int member) {
        Layout layout = modifiers.layout();
        if (layout != null) {
            assert (layout.mIndex == -1);
            assert (layout.mBinding == -1);
            assert (layout.mSet == -1);
            assert (layout.mInputAttachmentIndex == -1);
            if (layout.mLocation >= 0) {
                writeInstruction(SpvOpMemberDecorate, targetId, member, SpvDecorationLocation,
                        layout.mLocation, mDecorationBuffer);
            }
            if (layout.mComponent >= 0) {
                writeInstruction(SpvOpMemberDecorate, targetId, member, SpvDecorationComponent,
                        layout.mComponent, mDecorationBuffer);
            }
            if (layout.mBuiltin >= 0) {
                writeInstruction(SpvOpMemberDecorate, targetId, member, SpvDecorationBuiltIn,
                        layout.mBuiltin, mDecorationBuffer);
            }
        }

        // interpolation qualifiers
        if ((modifiers.flags() & Modifiers.kSmooth_Flag) == 0) {
            // no smooth
            if ((modifiers.flags() & Modifiers.kNoPerspective_Flag) != 0) {
                writeInstruction(SpvOpMemberDecorate, targetId, member, SpvDecorationNoPerspective,
                        mDecorationBuffer);
            } else if ((modifiers.flags() & Modifiers.kFlat_Flag) != 0) {
                writeInstruction(SpvOpMemberDecorate, targetId, member, SpvDecorationFlat,
                        mDecorationBuffer);
            }
        }

        // memory qualifiers
        if ((modifiers.flags() & Modifiers.kVolatile_Flag) != 0) {
            writeInstruction(SpvOpMemberDecorate, targetId, member, SpvDecorationVolatile,
                    mDecorationBuffer);
        }
        if ((modifiers.flags() & (Modifiers.kCoherent_Flag | Modifiers.kVolatile_Flag)) != 0) {
            // volatile is always coherent
            writeInstruction(SpvOpMemberDecorate, targetId, member, SpvDecorationCoherent,
                    mDecorationBuffer);
        }
        if ((modifiers.flags() & Modifiers.kRestrict_Flag) != 0) {
            writeInstruction(SpvOpMemberDecorate, targetId, member, SpvDecorationRestrict,
                    mDecorationBuffer);
        }
        if ((modifiers.flags() & Modifiers.kReadOnly_Flag) != 0) {
            writeInstruction(SpvOpMemberDecorate, targetId, member, SpvDecorationNonWritable,
                    mDecorationBuffer);
        }
        if ((modifiers.flags() & Modifiers.kWriteOnly_Flag) != 0) {
            writeInstruction(SpvOpMemberDecorate, targetId, member, SpvDecorationNonReadable,
                    mDecorationBuffer);
        }
    }

    private int writeInterfaceBlock(@Nonnull InterfaceBlock block) {
        //TODO resource array is not allowed in OpenGL,
        // but allowed in Vulkan (VkDescriptorSetLayoutBinding.descriptorCount)
        // the resource array type doesn't have ArrayStride
        int resultId = getUniqueId();
        Variable variable = block.getVariable();
        Modifiers modifiers = variable.getModifiers();
        final MemoryLayout desiredLayout;
        if ((modifiers.layoutFlags() & Layout.kStd140_LayoutFlag) != 0) {
            desiredLayout = MemoryLayout.Std140;
        } else if ((modifiers.layoutFlags() & Layout.kStd430_LayoutFlag) != 0) {
            desiredLayout = MemoryLayout.Std430;
        } else {
            desiredLayout = null;
        }
        final MemoryLayout memoryLayout;
        if (modifiers.isBuffer()) {
            memoryLayout = desiredLayout != null ? desiredLayout : MemoryLayout.Std430;
        } else if (modifiers.isUniform()) {
            memoryLayout = desiredLayout != null ? desiredLayout :
                    (modifiers.layoutFlags() & Layout.kPushConstant_LayoutFlag) != 0
                            ? MemoryLayout.Std430
                            : MemoryLayout.Extended;
        } else {
            memoryLayout = null;
        }
        Type type = variable.getType();
        assert type.isInterfaceBlock();
        if (memoryLayout != null && !memoryLayout.isSupported(type)) {
            getContext().error(type.mPosition, "type '" + type +
                    "' is not permitted here");
            return resultId;
        }

        int typeId = writeStruct(type, memoryLayout);
        //FIXME not check block modifiers
        if (modifiers.layoutBuiltin() == -1) {
            boolean legacyBufferBlock =
                    modifiers.isBuffer() &&
                            mOutputVersion.isBefore(SPIRVVersion.SPIRV_1_3);
            writeInstruction(SpvOpDecorate,
                    typeId,
                    legacyBufferBlock
                            ? SpvDecorationBufferBlock
                            : SpvDecorationBlock,
                    mDecorationBuffer);
        }
        writeModifiers(modifiers, resultId);

        int ptrTypeId = getUniqueId();
        int storageClass = getStorageClass(variable);
        writeInstruction(SpvOpTypePointer, ptrTypeId, storageClass, typeId, mConstantBuffer);
        writeInstruction(SpvOpVariable, ptrTypeId, resultId, storageClass, mConstantBuffer);
        if (mEmitNames) {
            writeInstruction(SpvOpName, resultId, variable.getName(), mNameBuffer);
        }

        mVariableTable.put(variable, resultId);
        return resultId;
    }

    private static boolean isCompileTimeConstant(VariableDecl variableDecl) {
        return variableDecl.getVariable().getModifiers().isConst() &&
                (variableDecl.getVariable().getType().isScalar() ||
                        variableDecl.getVariable().getType().isVector()) &&
                (ConstantFolder.getConstantValueOrNullForVariable(variableDecl.getInit()) != null ||
                        Analysis.isCompileTimeConstant(variableDecl.getInit()));
    }

    private boolean writeGlobalVariableDecl(VariableDecl variableDecl) {
        // If this global variable is a compile-time constant then we'll emit OpConstant or
        // OpConstantComposite later when the variable is referenced. Avoid declaring an OpVariable now.
        if (isCompileTimeConstant(variableDecl)) {
            return true;
        }

        int storageClass = getStorageClass(variableDecl.getVariable());

        int id = writeGlobalVariable(storageClass, variableDecl.getVariable());
        if (variableDecl.getInit() != null) {
            int init = writeExpression(variableDecl.getInit(), mGlobalInitBuffer);
            writeOpStore(storageClass, id, init, mGlobalInitBuffer);
        }

        return true;
    }

    private int writeGlobalVariable(int storageClass, Variable variable) {
        Type type = variable.getType();
        int resultId = getUniqueId(type);

        Modifiers modifiers = variable.getModifiers();
        writeModifiers(modifiers, resultId);

        // non-interface block variables have no memory layout
        int ptrTypeId = writePointerType(type, modifiers,
                null, storageClass);
        writeInstruction(SpvOpVariable, ptrTypeId, resultId, storageClass, mConstantBuffer);
        if (mEmitNames) {
            writeInstruction(SpvOpName, resultId, variable.getName(), mNameBuffer);
        }

        mVariableTable.put(variable, resultId);
        return resultId;
    }

    private int writeExpression(@Nonnull Expression expr, Writer writer) {
        return switch (expr.getKind()) {
            case BINARY -> writeBinaryExpression((BinaryExpression) expr, writer);
            case VARIABLE_REFERENCE -> writeVariableReference((VariableReference) expr, writer);
            case FIELD_ACCESS -> writeLValue(expr, writer).load(this, writer);
            default -> {
                getContext().error(expr.mPosition, "unsupported expression");
                yield NONE_ID;
            }
        };
    }

    private int broadcast(@Nonnull Type type, int id, Writer writer) {
        // Scalars require no additional work; we can return the passed-in ID as is.
        if (!type.isScalar()) {
            assert type.isVector();

            // Splat the input scalar across a vector.
            int vectorSize = type.getRows();

            IntArrayList values = mTmpIdList;
            values.clear();
            for (int i = 0; i < vectorSize; i++) {
                values.add(id);
            }
            return writeOpCompositeConstruct(type, values.elements(), vectorSize, writer);
        }

        return id;
    }

    private int writeBinaryExpression(@Nonnull BinaryExpression expr, Writer writer) {
        Expression left = expr.getLeft();
        Expression right = expr.getRight();
        Operator op = expr.getOperator();

        switch (op) {
            case ASSIGN:
                // Handles assignment.
                int rhs = writeExpression(right, writer);
                writeLValue(left, writer).store(this, rhs, writer);
                return rhs;

            case LOGICAL_AND:
                if (!getContext().getOptions().mNoShortCircuit) {
                    // Handles short-circuiting; we don't necessarily evaluate both LHS and RHS.
                    return writeLogicalAndSC(left, right, writer);
                }
                break;

            case LOGICAL_OR:
                if (!getContext().getOptions().mNoShortCircuit) {
                    // Handles short-circuiting; we don't necessarily evaluate both LHS and RHS.
                    return writeLogicalOrSC(left, right, writer);
                }
                break;

            default:
                break;
        }

        LValue lvalue;
        int lhs;
        if (op.isAssignment()) {
            lvalue = writeLValue(left, writer);
            lhs = lvalue.load(this, writer);
        } else {
            lvalue = null;
            lhs = writeExpression(left, writer);
        }

        int rhs = writeExpression(right, writer);
        int result = writeBinaryExpression(expr.mPosition, left.getType(), lhs, op.removeAssignment(),
                right.getType(), rhs, expr.getType(), writer);
        if (lvalue != null) {
            lvalue.store(this, result, writer);
        }
        return result;
    }

    // short-circuit version '&&'
    private int writeLogicalAndSC(@Nonnull Expression left, @Nonnull Expression right,
                                  Writer writer) {
        int falseConstant = writeScalarConstant(0, getContext().getTypes().mBool);
        int lhs = writeExpression(left, writer);

        int numReachableOps = mReachableOps.size();
        int numStoreOps = mStoreOps.size();

        int rhsLabel = getUniqueId();
        int end = getUniqueId();
        int lhsBlock = mCurrentBlock;
        writeInstruction(SpvOpSelectionMerge, end, SpvSelectionControlMaskNone, writer);
        writeInstruction(SpvOpBranchConditional, lhs, rhsLabel, end, writer);
        writeLabel(rhsLabel, writer);
        int rhs = writeExpression(right, writer);
        int rhsBlock = mCurrentBlock;
        writeInstruction(SpvOpBranch, end, writer);
        writeLabel(end, kBranchIsAbove, numReachableOps, numStoreOps, writer);
        int result = getUniqueId();
        int typeId = writeType(getContext().getTypes().mBool);
        writeInstruction(SpvOpPhi, typeId, result, falseConstant,
                lhsBlock, rhs, rhsBlock, writer);

        return result;
    }

    // short-circuit version '||'
    private int writeLogicalOrSC(@Nonnull Expression left, @Nonnull Expression right,
                                 Writer writer) {
        int trueConstant = writeScalarConstant(1, getContext().getTypes().mBool);
        int lhs = writeExpression(left, writer);

        int numReachableOps = mReachableOps.size();
        int numStoreOps = mStoreOps.size();

        int rhsLabel = getUniqueId();
        int end = getUniqueId();
        int lhsBlock = mCurrentBlock;
        writeInstruction(SpvOpSelectionMerge, end, SpvSelectionControlMaskNone, writer);
        writeInstruction(SpvOpBranchConditional, lhs, end, rhsLabel, writer);
        writeLabel(rhsLabel, writer);
        int rhs = writeExpression(right, writer);
        int rhsBlock = mCurrentBlock;
        writeInstruction(SpvOpBranch, end, writer);
        writeLabel(end, kBranchIsAbove, numReachableOps, numStoreOps, writer);
        int result = getUniqueId();
        int typeId = writeType(getContext().getTypes().mBool);
        writeInstruction(SpvOpPhi, typeId, result, trueConstant,
                lhsBlock, rhs, rhsBlock, writer);

        return result;
    }

    // non-assignment, no short-circuit
    private int writeBinaryExpression(int pos, Type leftType, int lhs, Operator op,
                                      Type rightType, int rhs,
                                      Type resultType, Writer writer) {
        // The comma operator ignores the type of the left-hand side entirely.
        if (op == Operator.COMMA) {
            return rhs;
        }
        Type operandType;
        if (leftType.matches(rightType)) {
            operandType = leftType;
        } else {
            Type leftComponentType = leftType.getComponentType();
            Type rightComponentType = rightType.getComponentType();
            if (leftType.getTypeKind() == rightType.getTypeKind() &&
                    (leftType.isScalar() || leftType.isVector() || leftType.isMatrix()) &&
                    leftType.getCols() == rightType.getCols() &&
                    leftType.getRows() == rightType.getRows() &&
                    leftComponentType.getScalarKind() == rightComponentType.getScalarKind() &&
                    leftComponentType.getWidth() == rightComponentType.getWidth()) {
                // only minWidth differs
                operandType = leftType;
            } else if (leftType.isVector() && rightType.isNumeric()) {
                if (resultType.getComponentType().isFloat()) {
                    switch (op) {
                        case DIV: {
                            int one = writeScalarConstant(1.0, rightType);
                            int reciprocal = getUniqueId(rightType);
                            writeInstruction(SpvOpFDiv, writeType(rightType), reciprocal, one, rhs, writer);
                            rhs = reciprocal;
                            // fallthrough
                        }
                        case MUL: {
                            int result = getUniqueId(resultType);
                            writeInstruction(SpvOpVectorTimesScalar, writeType(resultType),
                                    result, lhs, rhs, writer);
                            return result;
                        }
                        default:
                            break;
                    }
                }
                operandType = rightType;
            } else {
                getContext().error(pos, "unsupported mixed-type expression " +
                        leftType + " " + op + " " + rightType);
                return NONE_ID;
            }
        }

        switch (op) {
            case LT:
                assert resultType.isBoolean();
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFOrdLessThan, SpvOpSLessThan,
                        SpvOpULessThan, SpvOpUndef,
                        writer);
            case GT:
                assert resultType.isBoolean();
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFOrdGreaterThan, SpvOpSGreaterThan,
                        SpvOpUGreaterThan, SpvOpUndef,
                        writer);
            case LE:
                assert resultType.isBoolean();
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFOrdLessThanEqual, SpvOpSLessThanEqual,
                        SpvOpULessThanEqual, SpvOpUndef,
                        writer);
            case GE:
                assert resultType.isBoolean();
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFOrdGreaterThanEqual, SpvOpSGreaterThanEqual,
                        SpvOpUGreaterThanEqual, SpvOpUndef,
                        writer);
            case ADD:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/true,
                        SpvOpFAdd, SpvOpIAdd, SpvOpIAdd, SpvOpUndef,
                        writer);
            case SUB:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/true,
                        SpvOpFSub, SpvOpISub, SpvOpISub, SpvOpUndef,
                        writer);
            case MUL:
                if (leftType.isMatrix() && rightType.isMatrix()) {
                    int resultId = getUniqueId(resultType);
                    int typeId = writeType(resultType);
                    writeInstruction(SpvOpMatrixTimesMatrix, typeId, resultId,
                            lhs, rhs, writer);
                    return resultId;
                }
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFMul, SpvOpIMul, SpvOpIMul, SpvOpUndef,
                        writer);
            case DIV:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/true,
                        SpvOpFDiv, SpvOpSDiv, SpvOpUDiv, SpvOpUndef,
                        writer);
            case MOD:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFMod, SpvOpSMod, SpvOpUMod, SpvOpUndef,
                        writer);
            default:
                getContext().error(pos, "unsupported expression " +
                        leftType + " " + op + " " + rightType);
                return NONE_ID;
        }
    }

    // raw binary op
    // matrix * matrix ,    component-wise
    // matrix op matrix,    op in {+, -, /}
    // matrix op scalar,    op in {+, -, /}
    // scalar op matrix,    op in {+, -, /}
    private int writeBinaryMatrixOperation(@Nonnull Type resultType,
                                           @Nonnull Type leftType, @Nonnull Type rightType,
                                           int lhs, int rhs, // SpvId
                                           int op, // SpvOp
                                           Writer writer) {
        assert resultType.isMatrix();
        boolean leftMat = leftType.isMatrix();
        boolean rightMat = rightType.isMatrix();
        assert leftMat || rightMat;
        Type columnType = resultType.getComponentType().toVector(getContext(), resultType.getRows());
        int columnTypeId = writeType(columnType);

        if (leftType.isScalar()) {
            lhs = broadcast(columnType, lhs, writer);
        }
        if (rightType.isScalar()) {
            rhs = broadcast(columnType, rhs, writer);
        }

        // do each vector op
        IntArrayList columns = mTmpIdList;
        columns.clear();
        for (int i = 0; i < resultType.getCols(); i++) {
            int leftColumn = leftMat ? writeOpCompositeExtract(columnType, lhs, i, writer) : lhs;
            int rightColumn = rightMat ? writeOpCompositeExtract(columnType, rhs, i, writer) : rhs;
            int resultId = getUniqueId(resultType);
            writeInstruction(op, columnTypeId, resultId, leftColumn, rightColumn, writer);
            columns.add(resultId);
        }
        return writeOpCompositeConstruct(resultType, columns.elements(), resultType.getCols(), writer);
    }

    // raw binary op
    private int writeBinaryMatrixOperation(int pos, Type resultType,
                                           Type leftType, Type rightType,
                                           int lhs, int rhs, // SpvId
                                           int floatOp, int signedOp, int unsignedOp, // SpvOp
                                           Writer writer) {
        int op = select_by_component_type(resultType, floatOp, signedOp, unsignedOp, SpvOpUndef);
        if (op == SpvOpUndef) {
            getContext().error(pos,
                    "unsupported operation for binary expression");
            return NONE_ID;
        }
        return writeBinaryMatrixOperation(resultType, leftType, rightType,
                lhs, rhs, op, writer);
    }

    // raw binary op
    private int writeBinaryOperation(int pos, Type resultType, Type operandType,
                                     int lhs, int rhs, // SpvId
                                     boolean matrixOpIsComponentWise,
                                     int floatOp, int signedOp, int unsignedOp, int booleanOp, // SpvOp
                                     Writer writer) {
        int op = select_by_component_type(operandType, floatOp, signedOp, unsignedOp, booleanOp);
        if (op == SpvOpUndef) {
            getContext().error(pos,
                    "unsupported operand type for binary expression: " + operandType);
            return NONE_ID;
        }
        if (matrixOpIsComponentWise && operandType.isMatrix()) {
            return writeBinaryMatrixOperation(resultType, operandType, operandType,
                    lhs, rhs, op, writer);
        }
        int resultId = getUniqueId(resultType);
        int typeId = writeType(resultType);
        writeInstruction(op, typeId, resultId, lhs, rhs, writer);
        return resultId;
    }

    private int writeVariableReference(VariableReference ref, Writer writer) {
        Expression constExpr = ConstantFolder.getConstantValueOrNullForVariable(ref);
        if (constExpr != null) {
            return writeExpression(constExpr, writer);
        }
        return writeLValue(ref, writer).load(this, writer);
    }

    private void pruneConditionalOps(int numReachableOps, int numStoreOps) {
        // Remove ops which are no longer reachable.
        while (mReachableOps.size() > numReachableOps) {
            int id = mReachableOps.popInt();
            Instruction inst = mSpvIdCache.remove(id);

            if (inst != null) {
                mOpCache.removeInt(inst);
            } else {
                throw new AssertionError("reachable-op list contains unrecognized SpvId");
            }
        }

        // Remove any cached stores that occurred during the conditional block.
        while (mStoreOps.size() > numStoreOps) {
            int id = mStoreOps.popInt();
            mStoreCache.remove(id);
        }
    }

    private void writeStatement(@Nonnull Statement stmt, Writer writer) {
        switch (stmt.getKind()) {
            case EXPRESSION -> writeExpression(((ExpressionStatement) stmt).getExpression(), writer);
            case DISCARD -> {
                if (mOutputVersion.isAtLeast(SPIRVVersion.SPIRV_1_6)) {
                    writeInstruction(SpvOpTerminateInvocation, writer);
                } else {
                    writeInstruction(SpvOpKill, writer);
                }
            }
        }
    }

    private static int select_by_component_type(@Nonnull Type type,
                                                int whenFloat,
                                                int whenSigned,
                                                int whenUnsigned,
                                                int whenBoolean) {
        if (type.isFloatOrCompound()) {
            return whenFloat;
        }
        if (type.isSignedOrCompound()) {
            return whenSigned;
        }
        if (type.isUnsignedOrCompound()) {
            return whenUnsigned;
        }
        if (type.isBooleanOrCompound()) {
            return whenBoolean;
        }
        throw new AssertionError(type);
    }

    private void writeBlock(BlockStatement blockStmt, Writer writer) {
        for (var stmt : blockStmt.getStatements()) {
            writeStatement(stmt, writer);
        }
    }

    private void writeLabel(int label, Writer writer) {
        assert mCurrentBlock == 0;
        mCurrentBlock = label;
        writeInstruction(SpvOpLabel, label, writer);
    }

    private void writeLabel(int label, int type, int numReachableOps,
                            int numStoreOps, Writer writer) {
        switch (type) {
            case kBranchIsBelow:
            case kBranchesOnBothSides:
                // With a backward or bidirectional branch, we haven't seen the code between the label
                // and the branch yet, so any stored value is potentially suspect. Without scanning
                // ahead to check, the only safe option is to ditch the store cache entirely.
                mStoreCache.clear();
                // fallthrough
            case kBranchIsAbove:
                // With a forward branch, we can rely on stores that we had cached at the start of the
                // statement/expression, if they haven't been touched yet. Anything newer than that is
                // pruned.
                pruneConditionalOps(numReachableOps, numStoreOps);
                break;
        }

        // Emit the label.
        writeLabel(label, writer);
    }

    private void writeAccessChain(@Nonnull Expression expr, Writer writer, IntList chain) {
        switch (expr.getKind()) {
            case INDEX -> {
                IndexExpression indexExpr = (IndexExpression) expr;
                if (indexExpr.getBase() instanceof Swizzle) {
                    //TODO
                    getContext().error(indexExpr.mPosition, "indexing on swizzle is not allowed");
                }
                writeAccessChain(indexExpr.getBase(), writer, chain);
                int id = writeExpression(indexExpr.getIndex(), writer);
                chain.add(id);
            }
            case FIELD_ACCESS -> {
                FieldAccess fieldAccess = (FieldAccess) expr;
                writeAccessChain(fieldAccess.getBase(), writer, chain);
                int id = writeScalarConstant(fieldAccess.getFieldIndex(), getContext().getTypes().mInt);
                chain.add(id);
            }
            default -> {
                int id = writeLValue(expr, writer).getPointer();
                assert id != NONE_ID;
                chain.add(id);
            }
        }
    }

    @Nonnull
    private LValue writeLValue(@Nonnull Expression expr, Writer writer) {
        Type type = expr.getType();
        boolean relaxedPrecision = type.isRelaxedPrecision();
        switch (expr.getKind()) {
            case INDEX, FIELD_ACCESS -> {
                IntArrayList chain = mAccessChain;
                chain.clear();
                writeAccessChain(expr, writer, chain);
                int member = getUniqueId();
                int storageClass = getStorageClass(expr);
                writeOpcode(SpvOpAccessChain, 3 + chain.size(), writer);
                writer.writeWord(writePointerType(type, storageClass));
                writer.writeWord(member);
                writer.writeWords(chain.elements(), chain.size());
                //TODO layout may be wrong
                int typeId = writeType(type, null, null);
                return new PointerLValue(member, false, typeId,
                        relaxedPrecision, storageClass);
            }
            case VARIABLE_REFERENCE -> {
                Variable variable = ((VariableReference) expr).getVariable();

                int entry = mVariableTable.getInt(variable);
                assert entry != 0;

                //TODO layout may be wrong
                int typeId = writeType(type, variable.getModifiers(), null);
                return new PointerLValue(entry, true, typeId,
                        relaxedPrecision, getStorageClass(expr));
            }
            case SWIZZLE -> {
                Swizzle swizzle = (Swizzle) expr;
                LValue lvalue = writeLValue(swizzle.getBase(), writer);
                if (lvalue.applySwizzle(swizzle.getComponents(), type)) {
                    return lvalue;
                }
                int base = lvalue.getPointer();
                if (base == NONE_ID) {
                    getContext().error(swizzle.mPosition,
                            "unable to retrieve lvalue from swizzle");
                }
                int storageClass = getStorageClass(swizzle.getBase());
                if (swizzle.getComponents().length == 1) {
                    int member = getUniqueId();
                    int typeId = writePointerType(type, storageClass);
                    int indexId = writeScalarConstant(swizzle.getComponents()[0], getContext().getTypes().mInt);
                    writeInstruction(SpvOpAccessChain, typeId, member, base, indexId, writer);
                    return new PointerLValue(member,
                            /*isMemoryObjectPointer=*/false,
                            writeType(type),
                            relaxedPrecision, storageClass);
                } else {
                    return new SwizzleLValue(base, swizzle.getComponents(),
                            swizzle.getBase().getType(), type, storageClass);
                }
            }
            default -> {
                //TODO temp vars
                assert false;
                throw new UnsupportedOperationException();
            }
        }
    }

    private void buildInstructions(@Nonnull TranslationUnit translationUnit) {
        mGLSLExtendedInstructions = getUniqueId();
        // always enable Shader capability, this implicitly declares Matrix capability
        mCapabilities.add(SpvCapabilityShader);

        for (var e : translationUnit) {
            if (e instanceof FunctionDefinition funcDef) {
                // Assign SpvIds to functions.
                FunctionDecl function = funcDef.getFunctionDecl();
                mFunctionTable.put(function, getUniqueId());
                if (function.isEntryPoint()) {
                    mEntryPointFunction = function;
                }
            }
        }

        if (mEntryPointFunction == null) {
            getContext().error(Position.NO_POS, "translation unit does not contain an entry point");
            return;
        }

        // Emit interface blocks.
        for (var e : translationUnit) {
            if (e instanceof InterfaceBlock block) {
                writeInterfaceBlock(block);
            }
        }

        // Emit global variable declarations.
        for (var e : translationUnit) {
            if (e instanceof GlobalVariableDecl globalVariableDecl) {
                VariableDecl variableDecl = globalVariableDecl.getVariableDecl();
                if (!writeGlobalVariableDecl(variableDecl)) {
                    return;
                }
            }
        }

        // Emit all the functions.
        for (var e : translationUnit) {
            if (e instanceof FunctionDefinition functionDef) {
                writeFunction(functionDef, mFunctionBuffer);
            }
        }

        // Add global variables to the list of interface variables.
        for (var e : mVariableTable.object2IntEntrySet()) {
            Variable variable = e.getKey();
            if (variable.getStorage() == Variable.kGlobal_Storage &&
                    variable.getModifiers().layoutBuiltin() == -1) {
                // Before version 1.4, the interface’s storage classes are
                // limited to the Input and Output storage classes. Starting with
                // version 1.4, the interface’s storage classes are all storage classes
                // used in declaring all global variables referenced by the entry point's
                // call tree.
                if (mOutputVersion.isAtLeast(SPIRVVersion.SPIRV_1_4) ||
                        (variable.getModifiers().flags() & (Modifiers.kIn_Flag | Modifiers.kOut_Flag)) != 0) {
                    mInterfaceVariables.add(e.getIntValue());
                }
            }
        }
    }

    int writeOpLoad(int type,
                    boolean relaxedPrecision,
                    int pointer,
                    Writer writer) {
        // Look for this pointer in our load-cache.
        int cachedOp = mStoreCache.get(pointer);
        if (cachedOp != 0) {
            return cachedOp;
        }

        // Write the requested OpLoad instruction.
        int resultId = getUniqueId(relaxedPrecision);
        writeInstruction(SpvOpLoad, type, resultId, pointer, writer);
        return resultId;
    }

    void writeOpStore(int storageClass,
                      int pointer,
                      int rvalue,
                      Writer writer) {
        // Write the uncached SpvOpStore directly.
        writeInstruction(SpvOpStore, pointer, rvalue, writer);

        if (storageClass == SpvStorageClassFunction) {
            // Insert a pointer-to-SpvId mapping into the load cache. A writeOpLoad to this pointer will
            // return the cached value as-is.
            mStoreCache.put(pointer, rvalue);
            mStoreOps.add(pointer);
        }
    }

    void writeOpcode(int opcode, int count, Writer writer) {
        if ((count & 0xFFFF0000) != 0) {
            getContext().error(Position.NO_POS, "too many words");
        }
        writer.writeWord((count << 16) | opcode);
    }

    void writeInstruction(int opcode, Writer writer) {
        writeOpcode(opcode, 1, writer);
    }

    void writeInstruction(int opcode, int word1, Writer writer) {
        writeOpcode(opcode, 2, writer);
        writer.writeWord(word1);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          Writer writer) {
        writeOpcode(opcode, 3, writer);
        writer.writeWord(word1);
        writer.writeWord(word2);
    }

    private void writeInstruction(int opcode, int word1, String string,
                                  Writer writer) {
        writeOpcode(opcode, 2 + (string.length() + 4 >> 2), writer);
        writer.writeWord(word1);
        writer.writeString8(getContext(), string);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          int word3, Writer writer) {
        writeOpcode(opcode, 4, writer);
        writer.writeWord(word1);
        writer.writeWord(word2);
        writer.writeWord(word3);
    }

    private void writeInstruction(int opcode, int word1, int word2,
                                  String string, Writer writer) {
        writeOpcode(opcode, 3 + (string.length() + 4 >> 2), writer);
        writer.writeWord(word1);
        writer.writeWord(word2);
        writer.writeString8(getContext(), string);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          int word3, int word4, Writer writer) {
        writeOpcode(opcode, 5, writer);
        writer.writeWord(word1);
        writer.writeWord(word2);
        writer.writeWord(word3);
        writer.writeWord(word4);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          int word3, int word4, int word5,
                          Writer writer) {
        writeOpcode(opcode, 6, writer);
        writer.writeWord(word1);
        writer.writeWord(word2);
        writer.writeWord(word3);
        writer.writeWord(word4);
        writer.writeWord(word5);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          int word3, int word4, int word5,
                          int word6, Writer writer) {
        writeOpcode(opcode, 7, writer);
        writer.writeWord(word1);
        writer.writeWord(word2);
        writer.writeWord(word3);
        writer.writeWord(word4);
        writer.writeWord(word5);
        writer.writeWord(word6);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          int word3, int word4, int word5,
                          int word6, int word7, Writer writer) {
        writeOpcode(opcode, 8, writer);
        writer.writeWord(word1);
        writer.writeWord(word2);
        writer.writeWord(word3);
        writer.writeWord(word4);
        writer.writeWord(word5);
        writer.writeWord(word6);
        writer.writeWord(word7);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          int word3, int word4, int word5,
                          int word6, int word7, int word8,
                          Writer writer) {
        writeOpcode(opcode, 9, writer);
        writer.writeWord(word1);
        writer.writeWord(word2);
        writer.writeWord(word3);
        writer.writeWord(word4);
        writer.writeWord(word5);
        writer.writeWord(word6);
        writer.writeWord(word7);
        writer.writeWord(word8);
    }

    /**
     * With bidirectional map.
     */
    private int writeInstructionWithCache(@Nonnull InstructionBuilder key,
                                          @Nonnull Writer writer) {
        assert (key.mOpcode != SpvOpLoad);
        assert (key.mOpcode != SpvOpStore);
        int cachedId = mOpCache.getInt(key);
        if (cachedId != 0) {
            releaseInstBuilder(key);
            return cachedId;
        }
        Instruction instruction = key.copy();

        int resultId = NONE_ID;
        boolean relaxedPrecision = false;

        switch (key.mResultKind) {
            case Instruction.kUniqueResult:
                resultId = getUniqueId();
                mSpvIdCache.put(resultId, instruction);
                break;
            case Instruction.kNoResult:
                mOpCache.put(instruction, resultId);
                break;

            case Instruction.kRelaxedPrecisionResult:
                relaxedPrecision = true;
                // fallthrough
            case Instruction.kKeyedResult:
                // fallthrough
            case Instruction.kDefaultPrecisionResult:
                resultId = getUniqueId(relaxedPrecision);
                mOpCache.put(instruction, resultId);
                mSpvIdCache.put(resultId, instruction);
                break;

            default:
                throw new AssertionError();
        }

        // Write the requested instruction.
        int[] values = key.mValues.elements();
        int[] kinds = key.mKinds.elements();
        int s = key.mValues.size();
        writeOpcode(key.mOpcode, s + 1, writer);
        for (int i = 0; i < s; i++) {
            if (Instruction.isResult(kinds[i])) {
                assert resultId != NONE_ID;
                writer.writeWord(resultId);
            } else {
                writer.writeWord(values[i]);
            }
        }

        releaseInstBuilder(key);
        // Return the result.
        return resultId;
    }

    private void releaseInstBuilder(InstructionBuilder key) {
        if (mInstBuilderPoolSize == mInstBuilderPool.length) {
            return;
        }
        mInstBuilderPool[mInstBuilderPoolSize++] = key;
    }
}
