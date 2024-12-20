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
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;

import static icyllis.arc3d.compiler.GLSLstd450.*;
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
    private final Reference2IntOpenHashMap<FunctionDecl> mFunctionTable = new Reference2IntOpenHashMap<>();
    private final Reference2IntOpenHashMap<Variable> mVariableTable = new Reference2IntOpenHashMap<>();

    // reused arrays storing SpvId; there are nested calls, but won't be too deep
    private final IntArrayList[] mIdListPool = new IntArrayList[6];
    private int mIdListPoolSize = 0;

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
    private final IntStack mBreakTarget = new IntArrayList();
    private final IntStack mContinueTarget = new IntArrayList();

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

    public SPIRVCodeGenerator(@NonNull Context context,
                              @NonNull TranslationUnit translationUnit,
                              @NonNull ShaderCaps shaderCaps) {
        super(context, translationUnit);
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
        NativeOutput output = new NativeOutput(estimatedSize);
        output.writeWord(SpvMagicNumber);
        output.writeWord(mOutputVersion.mVersionNumber);
        output.writeWord(GENERATOR_MAGIC_NUMBER);
        output.writeWord(mIdCount);
        output.writeWord(0);

        for (var it = mCapabilities.iterator(); it.hasNext(); ) {
            writeInstruction(SpvOpCapability, it.nextInt(), output);
        }
        writeInstruction(SpvOpExtInstImport, mGLSLExtendedInstructions, "GLSL.std.450", output);
        writeInstruction(SpvOpMemoryModel, SpvAddressingModelLogical, SpvMemoryModelGLSL450, output);
        writeOpcode(SpvOpEntryPoint, entryPointWordCount, output);
        if (kind.isVertex()) {
            output.writeWord(SpvExecutionModelVertex);
        } else if (kind.isFragment()) {
            output.writeWord(SpvExecutionModelFragment);
        } else if (kind.isCompute()) {
            output.writeWord(SpvExecutionModelGLCompute);
        }

        int entryPoint = mFunctionTable.getInt(mEntryPointFunction);
        output.writeWord(entryPoint);
        output.writeString8(getContext(), entryPointName);
        for (int id : mInterfaceVariables) {
            output.writeWord(id);
        }

        if (kind.isFragment()) {
            writeInstruction(SpvOpExecutionMode,
                    entryPoint,
                    mOutputTarget.isOpenGL()
                            ? SpvExecutionModeOriginLowerLeft
                            : SpvExecutionModeOriginUpperLeft,
                    output);
        }
        output.writeWords(mNameBuffer.elements(), mNameBuffer.size());
        output.writeWords(mDecorationBuffer.elements(), mDecorationBuffer.size());
        output.writeWords(mConstantBuffer.elements(), mConstantBuffer.size());
        output.writeWords(mFunctionBuffer.elements(), mFunctionBuffer.size());

        if (getContext().getErrorHandler().errorCount() != 0) {
            return null;
        }
        return output.detach();
    }

    private int getStorageClass(@NonNull Variable variable) {
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
            if (mOutputTarget.isVulkan()) {
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

    private int getStorageClass(@NonNull Expression expr) {
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

    int getUniqueId(@NonNull Type type) {
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
    private Instruction resultTypeForInstruction(@NonNull Instruction inst) {
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

    private boolean getConstants(IntArrayList values, IntArrayList constants) {
        for (int i = 0; i < values.size(); i++) {
            if (!getConstants(values.getInt(i), constants)) {
                return false;
            }
        }
        return true;
    }

    /**
     * For {@link #writeInstructionWithCache(InstructionBuilder, Output)}.
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
    int writeType(@NonNull Type type) {
        return writeType(type, null, null);
    }

    /**
     * Make type with cache.
     *
     * @param modifiers    non-null for opaque types, otherwise null
     * @param memoryLayout non-null for uniform and shader storage blocks, and their members, otherwise null
     */
    private int writeType(@NonNull Type type,
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
                    // If explicit layout is not required, we still need to use a default layout
                    // to ensure type matching, unless it is not supported (bool and bvec array)
                    if (MemoryLayout.Std140.isSupported(type)) {
                        stride = MemoryLayout.Std140.stride(type);
                        assert stride > 0;
                    } else {
                        stride = 0;
                    }
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
            case Type.kSampler_TypeKind -> {
                if (type.isSeparateSampler()) {
                    // pure sampler
                    yield writeInstructionWithCache(
                            getInstBuilder(SpvOpTypeSampler)
                                    .addResult(),
                            mConstantBuffer
                    );
                }
                int sampledTypeId = writeType(type.getComponentType());
                //TODO get image format for storage image
                int format = SpvImageFormatUnknown;

                int imageTypeId = writeInstructionWithCache(
                        getInstBuilder(SpvOpTypeImage)
                                .addResult()
                                .addWord(sampledTypeId)
                                .addWord(type.getDimensions())      // dim
                                .addWord(type.isShadow() ? 1 : 0)   // depth
                                .addWord(type.isArrayed() ? 1 : 0)
                                .addWord(type.isMultiSampled() ? 1 : 0)
                                .addWord(type.isSampled() ? 1 : 2)
                                .addWord(format),
                        mConstantBuffer
                );
                if (type.isCombinedSampler()) {
                    //TODO spir-v 1.6
                    if (type.getDimensions() == SpvDimBuffer) {
                        mCapabilities.add(SpvCapabilitySampledBuffer);
                    }
                    yield writeInstructionWithCache(
                            getInstBuilder(SpvOpTypeSampledImage)
                                    .addResult()
                                    .addWord(imageTypeId),
                            mConstantBuffer
                    );
                } else {
                    // pure texture (sampled image), storage image, or subpass data
                    yield imageTypeId;
                }
            }
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
    private int writeStruct(@NonNull Type type,
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
        for (int i = 0; i < fields.size(); i++) {
            var field = fields.get(i);
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

    private int writeFunction(@NonNull FunctionDefinition func, Output output) {
        int numReachableOps = mReachableOps.size();
        int numStoreOps = mStoreOps.size();

        mVariableBuffer.clear();
        int result = writeFunctionDecl(func.getFunctionDecl(), output);
        mCurrentBlock = 0;
        writeLabel(getUniqueId(), output);
        mBodyBuffer.clear();
        writeBlockStatement(func.getBody(), mBodyBuffer);
        output.writeWords(mVariableBuffer.elements(), mVariableBuffer.size());
        if (func.getFunctionDecl().isEntryPoint()) {
            output.writeWords(mGlobalInitBuffer.elements(), mGlobalInitBuffer.size());
        }
        output.writeWords(mBodyBuffer.elements(), mBodyBuffer.size());

        if (mCurrentBlock != 0) {
            if (func.getFunctionDecl().getReturnType().isVoid()) {
                writeInstruction(SpvOpReturn, output);
            } else {
                writeInstruction(SpvOpUnreachable, output);
            }
        }
        writeInstruction(SpvOpFunctionEnd, output);
        pruneConditionalOps(numReachableOps, numStoreOps);
        return result;
    }

    private int writeFunctionDecl(@NonNull FunctionDecl decl, Output output) {
        int result = mFunctionTable.getInt(decl);
        int returnTypeId = writeType(decl.getReturnType());
        int functionTypeId = writeFunctionType(decl);
        writeInstruction(SpvOpFunction, returnTypeId, result,
                SpvFunctionControlMaskNone, functionTypeId, output);
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
            writeInstruction(SpvOpFunctionParameter, type, id, output);
        }
        return result;
    }

    private int writeFunctionType(@NonNull FunctionDecl function) {
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

    private int writeFunctionParameterType(@NonNull Type paramType,
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

    private int writePointerType(@NonNull Type type,
                                 int storageClass) {
        return writePointerType(type, null, null, storageClass);
    }

    private int writePointerType(@NonNull Type type,
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

    private int writeOpCompositeExtract(@NonNull Type type,
                                        int base,
                                        int index,
                                        Output output) {
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
                output
        );
    }

    private int writeOpCompositeExtract(@NonNull Type type,
                                        int base,
                                        int index1,
                                        int index2,
                                        Output output) {
        // If the base op is a composite, we can extract from it directly.
        int result = getComponent(base, index1);
        if (result != NONE_ID) {
            return writeOpCompositeExtract(type, result, index2, output);
        }
        int typeId = writeType(type);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpCompositeExtract)
                        .addWord(typeId)
                        .addResult()
                        .addWord(base)
                        .addWord(index1)
                        .addWord(index2),
                output
        );
    }

    private int writeOpCompositeConstruct(@NonNull Type type,
                                          IntArrayList values,
                                          Output output) {
        // If this is a vector/matrix composed entirely of literals, write a constant-composite instead.
        if (type.isVector() || type.isMatrix()) {
            IntArrayList constants = obtainIdList();
            if (getConstants(values, constants)) {
                int resultId;
                if (type.isVector()) {
                    // Create a vector from literals.
                    resultId = writeOpConstantComposite(type, constants.elements(), 0, constants.size());
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
                    resultId = writeOpConstantComposite(type, constants.elements(), start, type.getCols());
                }
                releaseIdList(constants);
                return resultId;
            }
            releaseIdList(constants);
        }

        int typeId = writeType(type);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpCompositeConstruct)
                        .addWord(typeId)
                        .addResult()
                        .addWords(values.elements(), 0, values.size()),
                output
        );
    }

    private int writeOpConstantTrue(@NonNull Type type) {
        assert type.isBoolean();
        int typeId = writeType(type);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpConstantTrue)
                        .addWord(typeId)
                        .addResult(),
                mConstantBuffer
        );
    }

    private int writeOpConstantFalse(@NonNull Type type) {
        assert type.isBoolean();
        int typeId = writeType(type);
        return writeInstructionWithCache(
                getInstBuilder(SpvOpConstantFalse)
                        .addWord(typeId)
                        .addResult(),
                mConstantBuffer
        );
    }

    private int writeOpConstant(@NonNull Type type, int valueBits) {
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

    private int writeOpConstantComposite(@NonNull Type type,
                                         int[] values, int offset, int count) {
        assert !type.isVector() || count == type.getRows();
        assert !type.isMatrix() || count == type.getCols();
        assert !type.isArray() || count == type.getArraySize();
        assert !type.isStruct() || count == type.getFields().size();
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

    private int writeLiteral(@NonNull Literal literal) {
        return writeScalarConstant(literal.getValue(), literal.getType());
    }

    /**
     * Write decorations.
     */
    private void writeModifiers(@NonNull Modifiers modifiers, int targetId) {
        Layout layout = modifiers.layout();
        boolean hasLocation = false;
        boolean hasBinding = false;
        boolean isPushConstant = false;
        int descriptorSet = -1;
        if (layout != null) {
            isPushConstant = (layout.layoutFlags() & Layout.kPushConstant_LayoutFlag) != 0;
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
                if (mOutputTarget.isVulkan() && !isPushConstant) {
                    // add a default descriptor set
                    writeInstruction(SpvOpDecorate, targetId, SpvDecorationDescriptorSet,
                            0, mDecorationBuffer);
                }
            } else if (mOutputTarget.isOpenGL()) {
                getContext().error(modifiers.mPosition, "'set' is not allowed");
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
    private void writeFieldModifiers(@NonNull Modifiers modifiers, int targetId, int member) {
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

    private int writeInterfaceBlock(@NonNull InterfaceBlock block) {
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

    private static boolean is_compile_time_constant(VariableDecl variableDecl) {
        return variableDecl.getVariable().getModifiers().isConst() &&
                (variableDecl.getVariable().getType().isScalar() ||
                        variableDecl.getVariable().getType().isVector()) &&
                (ConstantFolder.getConstantValueOrNullForVariable(variableDecl.getInit()) != null ||
                        Analysis.isCompileTimeConstant(variableDecl.getInit()));
    }

    private boolean writeGlobalVariableDecl(VariableDecl variableDecl) {
        // If this global variable is a compile-time constant then we'll emit OpConstant or
        // OpConstantComposite later when the variable is referenced. Avoid declaring an OpVariable now.
        if (is_compile_time_constant(variableDecl)) {
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

    private void writeVariableDecl(VariableDecl variableDecl, Output output) {
        // If this variable is a compile-time constant then we'll emit OpConstant or
        // OpConstantComposite later when the variable is referenced. Avoid declaring an OpVariable now.
        if (is_compile_time_constant(variableDecl)) {
            return;
        }

        Variable variable = variableDecl.getVariable();
        int id = getUniqueId(variable.getType());
        mVariableTable.put(variable, id);
        int ptrTypeId = writePointerType(variable.getType(), SpvStorageClassFunction);
        writeInstruction(SpvOpVariable, ptrTypeId, id, SpvStorageClassFunction, mVariableBuffer);
        if (mEmitNames) {
            writeInstruction(SpvOpName, id, variable.getName(), mNameBuffer);
        }
        if (variableDecl.getInit() != null) {
            int init = writeExpression(variableDecl.getInit(), output);
            writeOpStore(SpvStorageClassFunction, id, init, output);
        }
    }

    private int writeExpression(@NonNull Expression expr, Output output) {
        return switch (expr.getKind()) {
            case LITERAL -> writeLiteral((Literal) expr);
            case PREFIX -> writePrefixExpression((PrefixExpression) expr, output);
            case POSTFIX -> writePostfixExpression((PostfixExpression) expr, output);
            case BINARY -> writeBinaryExpression((BinaryExpression) expr, output);
            case CONDITIONAL -> writeConditionalExpression((ConditionalExpression) expr, output);
            case VARIABLE_REFERENCE -> writeVariableReference((VariableReference) expr, output);
            case INDEX -> writeIndexExpression((IndexExpression) expr, output);
            case FIELD_ACCESS -> writeFieldAccess((FieldAccess) expr, output);
            case SWIZZLE -> writeSwizzle((Swizzle) expr, output);
            case FUNCTION_CALL -> writeFunctionCall((FunctionCall) expr, output);
            case CONSTRUCTOR_COMPOUND -> writeConstructorCompound((ConstructorCompound) expr, output);
            case CONSTRUCTOR_VECTOR_SPLAT -> writeConstructorVectorSplat((ConstructorVectorSplat) expr, output);
            case CONSTRUCTOR_DIAGONAL_MATRIX ->
                    writeConstructorDiagonalMatrix((ConstructorDiagonalMatrix) expr, output);
            case CONSTRUCTOR_SCALAR_CAST -> writeConstructorScalarCast((ConstructorScalarCast) expr, output);
            case CONSTRUCTOR_COMPOUND_CAST -> writeConstructorCompoundCast((ConstructorCompoundCast) expr, output);
            case CONSTRUCTOR_ARRAY, CONSTRUCTOR_STRUCT -> writeCompositeConstructor((ConstructorCall) expr, output);
            case CONSTRUCTOR_ARRAY_CAST -> writeExpression(((ConstructorArrayCast) expr).getArgument(), output);
            default -> {
                getContext().error(expr.mPosition, "unsupported expression: " + expr.getKind());
                yield NONE_ID;
            }
        };
    }

    private int broadcast(@NonNull Type type, int id, Output output) {
        // Scalars require no additional work; we can return the passed-in ID as is.
        if (!type.isScalar()) {
            assert type.isVector();

            // Splat the input scalar across a vector.
            int vectorSize = type.getRows();

            IntArrayList values = obtainIdList();
            for (int i = 0; i < vectorSize; i++) {
                values.add(id);
            }
            id = writeOpCompositeConstruct(type, values, output);
            releaseIdList(values);
        }

        return id;
    }

    // construct scalar constant or vector constant
    private int vectorize(double value, Type type, int vectorSize, Output output) {
        assert (vectorSize >= 1 && vectorSize <= 4);
        int id = writeScalarConstant(value, type);
        if (vectorSize > 1) {
            return broadcast(type.toVector(getContext(), vectorSize), id, output);
        }
        return id;
    }

    /**
     * Promotes an expression to a vector. If the expression is already a vector with vectorSize
     * columns, returns it unmodified. If the expression is a scalar, either promotes it to a
     * vector (if vectorSize > 1) or returns it unmodified (if vectorSize == 1). Asserts if the
     * expression is already a vector and it does not have vectorSize columns.
     */
    private int vectorize(Expression arg, int vectorSize, Output output) {
        assert (vectorSize >= 1 && vectorSize <= 4);
        Type argType = arg.getType();
        if (argType.isScalar() && vectorSize > 1) {
            int argId = writeExpression(arg, output);
            return broadcast(argType.toVector(getContext(), vectorSize), argId, output);
        }

        assert (vectorSize == argType.getRows());
        return writeExpression(arg, output);
    }

    /**
     * Given a list of potentially mixed scalars and vectors, promotes the scalars to match the
     * size of the vectors and returns the ids of the written expressions. e.g. given (float, vec2),
     * returns (vec2(float), vec2). It is an error to use mismatched vector sizes, e.g. (float,
     * vec2, vec3).
     */
    private void vectorize(Expression[] args, IntArrayList result, Output output) {
        int vectorSize = 1;
        for (Expression arg : args) {
            if (arg.getType().isVector()) {
                if (vectorSize > 1) {
                    assert (arg.getType().getRows() == vectorSize);
                } else {
                    vectorSize = arg.getType().getRows();
                }
            }
        }
        for (Expression arg : args) {
            result.add(vectorize(arg, vectorSize, output));
        }
    }

    private int writePrefixExpression(@NonNull PrefixExpression expr, Output output) {
        Type type = expr.getType();
        return switch (expr.getOperator()) {
            case ADD -> writeExpression(expr.getOperand(), output); // positive
            case SUB -> {
                // negative
                int negateOp = select_by_component_type(type,
                        SpvOpFNegate, SpvOpSNegate, SpvOpSNegate, SpvOpUndef);
                assert (negateOp != SpvOpUndef);
                int id = writeExpression(expr.getOperand(), output);
                if (type.isMatrix()) {
                    yield writeUnaryMatrixOperation(type, id, negateOp, output);
                }
                int resultId = getUniqueId(type);
                int typeId = writeType(type);
                writeInstruction(negateOp, typeId, resultId, id, output);
                yield resultId;
            }
            case INC -> {
                LValue lv = writeLValue(expr.getOperand(), output);
                int oneId = vectorize(1, type.getComponentType(), type.getRows(), output);
                int resultId = writeBinaryOperation(expr.mPosition,
                        type, type,
                        lv.load(this, output), oneId,
                        /*matrixOpIsComponentWise*/true,
                        SpvOpFAdd, SpvOpIAdd,
                        SpvOpIAdd, SpvOpUndef,
                        output);
                lv.store(this, resultId, output);
                yield resultId;
            }
            case DEC -> {
                LValue lv = writeLValue(expr.getOperand(), output);
                int oneId = vectorize(1, type.getComponentType(), type.getRows(), output);
                int resultId = writeBinaryOperation(expr.mPosition,
                        type, type,
                        lv.load(this, output), oneId,
                        /*matrixOpIsComponentWise*/true,
                        SpvOpFSub, SpvOpISub,
                        SpvOpISub, SpvOpUndef,
                        output);
                lv.store(this, resultId, output);
                yield resultId;
            }
            case LOGICAL_NOT -> {
                assert expr.getOperand().getType().isBoolean();
                int id = writeExpression(expr.getOperand(), output);
                int resultId = getUniqueId();
                int typeId = writeType(type);
                writeInstruction(SpvOpLogicalNot, typeId, resultId, id, output);
                yield resultId;
            }
            case BITWISE_NOT -> {
                int id = writeExpression(expr.getOperand(), output);
                int resultId = getUniqueId();
                int typeId = writeType(type);
                writeInstruction(SpvOpNot, typeId, resultId, id, output);
                yield resultId;
            }
            default -> {
                getContext().error(expr.mPosition,
                        "unsupported prefix expression");
                yield NONE_ID;
            }
        };
    }

    private int writePostfixExpression(@NonNull PostfixExpression expr, Output output) {
        Type type = expr.getType();
        LValue lv = writeLValue(expr.getOperand(), output);
        int oneId = vectorize(1, type.getComponentType(), type.getRows(), output);
        int resultId = lv.load(this, output);
        return switch (expr.getOperator()) {
            case INC -> {
                int tmp = writeBinaryOperation(expr.mPosition,
                        type, type, resultId, oneId,
                        /*matrixOpIsComponentWise*/true,
                        SpvOpFAdd, SpvOpIAdd,
                        SpvOpIAdd, SpvOpUndef,
                        output);
                lv.store(this, tmp, output);
                yield resultId;
            }
            case DEC -> {
                int tmp = writeBinaryOperation(expr.mPosition,
                        type, type, resultId, oneId,
                        /*matrixOpIsComponentWise*/true,
                        SpvOpFSub, SpvOpISub,
                        SpvOpISub, SpvOpUndef,
                        output);
                lv.store(this, tmp, output);
                yield resultId;
            }
            default -> {
                getContext().error(expr.mPosition,
                        "unsupported postfix expression");
                yield NONE_ID;
            }
        };
    }

    private int writeMatrixComparison(Type operandType, int lhs, int rhs,
                                      int floatOp, int intOp,
                                      int vectorMergeOp, int mergeOp,
                                      Output output) {
        int compareOp = operandType.isFloatOrCompound() ? floatOp : intOp;
        assert (operandType.isMatrix());
        Type columnType = operandType.getComponentType().toVector(
                getContext(), operandType.getRows());
        Type boolType = getContext().getTypes().mBool;
        Type bvecType = boolType.toVector(getContext(), operandType.getRows());
        int boolTypeId = writeType(boolType);
        int bvecTypeId = writeType(bvecType);
        int result = 0;
        for (int i = 0; i < operandType.getCols(); i++) {
            int columnL = writeOpCompositeExtract(columnType, lhs, i, output);
            int columnR = writeOpCompositeExtract(columnType, rhs, i, output);
            int compare = getUniqueId(operandType);
            writeInstruction(compareOp, bvecTypeId, compare, columnL, columnR, output);
            int merge = getUniqueId();
            writeInstruction(vectorMergeOp, boolTypeId, merge, compare, output);
            if (result != 0) {
                int next = getUniqueId();
                writeInstruction(mergeOp, boolTypeId, next, result, merge, output);
                result = next;
            } else {
                result = merge;
            }
        }
        return result;
    }

    // negate matrix
    private int writeUnaryMatrixOperation(@NonNull Type operandType,
                                          int operand,
                                          int op,
                                          Output output) {
        assert (operandType.isMatrix());
        Type columnType = operandType.getComponentType().toVector(getContext(), operandType.getRows());
        int columnTypeId = writeType(columnType);

        IntArrayList columns = obtainIdList();
        for (int i = 0; i < operandType.getCols(); i++) {
            int srcColumn = writeOpCompositeExtract(columnType, operand, i, output);
            int dstColumn = getUniqueId(operandType);
            writeInstruction(op, columnTypeId, dstColumn, srcColumn, output);
            columns.add(dstColumn);
        }

        int resultId = writeOpCompositeConstruct(operandType, columns, output);
        releaseIdList(columns);
        return resultId;
    }

    private int writeBinaryExpression(@NonNull BinaryExpression expr, Output output) {
        Expression left = expr.getLeft();
        Expression right = expr.getRight();
        Operator op = expr.getOperator();

        switch (op) {
            case ASSIGN:
                // Handles assignment.
                int rhs = writeExpression(right, output);
                writeLValue(left, output).store(this, rhs, output);
                return rhs;

            case LOGICAL_AND:
                if (!getContext().getOptions().mNoShortCircuit) {
                    // Handles short-circuiting; we don't necessarily evaluate both LHS and RHS.
                    return writeLogicalAndSC(left, right, output);
                }
                break;

            case LOGICAL_OR:
                if (!getContext().getOptions().mNoShortCircuit) {
                    // Handles short-circuiting; we don't necessarily evaluate both LHS and RHS.
                    return writeLogicalOrSC(left, right, output);
                }
                break;

            default:
                break;
        }

        LValue lvalue;
        int lhs;
        if (op.isAssignment()) {
            lvalue = writeLValue(left, output);
            lhs = lvalue.load(this, output);
        } else {
            lvalue = null;
            lhs = writeExpression(left, output);
        }

        int rhs = writeExpression(right, output);
        int result = writeBinaryExpression(expr.mPosition, left.getType(), lhs, op.removeAssignment(),
                right.getType(), rhs, expr.getType(), output);
        if (lvalue != null) {
            lvalue.store(this, result, output);
        }
        return result;
    }

    // short-circuit version '&&'
    private int writeLogicalAndSC(@NonNull Expression left, @NonNull Expression right,
                                  Output output) {
        int falseConstant = writeScalarConstant(0, getContext().getTypes().mBool);
        int lhs = writeExpression(left, output);

        int numReachableOps = mReachableOps.size();
        int numStoreOps = mStoreOps.size();

        int rhsLabel = getUniqueId();
        int end = getUniqueId();
        int lhsBlock = mCurrentBlock;
        writeInstruction(SpvOpSelectionMerge, end, SpvSelectionControlMaskNone, output);
        writeInstruction(SpvOpBranchConditional, lhs, rhsLabel, end, output);
        writeLabel(rhsLabel, output);
        int rhs = writeExpression(right, output);
        int rhsBlock = mCurrentBlock;
        writeInstruction(SpvOpBranch, end, output);
        writeLabel(end, kBranchIsAbove, numReachableOps, numStoreOps, output);
        int result = getUniqueId();
        int typeId = writeType(getContext().getTypes().mBool);
        writeInstruction(SpvOpPhi, typeId, result, falseConstant,
                lhsBlock, rhs, rhsBlock, output);

        return result;
    }

    // short-circuit version '||'
    private int writeLogicalOrSC(@NonNull Expression left, @NonNull Expression right,
                                 Output output) {
        int trueConstant = writeScalarConstant(1, getContext().getTypes().mBool);
        int lhs = writeExpression(left, output);

        int numReachableOps = mReachableOps.size();
        int numStoreOps = mStoreOps.size();

        int rhsLabel = getUniqueId();
        int end = getUniqueId();
        int lhsBlock = mCurrentBlock;
        writeInstruction(SpvOpSelectionMerge, end, SpvSelectionControlMaskNone, output);
        writeInstruction(SpvOpBranchConditional, lhs, end, rhsLabel, output);
        writeLabel(rhsLabel, output);
        int rhs = writeExpression(right, output);
        int rhsBlock = mCurrentBlock;
        writeInstruction(SpvOpBranch, end, output);
        writeLabel(end, kBranchIsAbove, numReachableOps, numStoreOps, output);
        int result = getUniqueId();
        int typeId = writeType(getContext().getTypes().mBool);
        writeInstruction(SpvOpPhi, typeId, result, trueConstant,
                lhsBlock, rhs, rhsBlock, output);

        return result;
    }

    // non-assignment, no short-circuit
    private int writeBinaryExpression(int pos, Type leftType, int lhs, Operator op,
                                      Type rightType, int rhs,
                                      Type resultType, Output output) {
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
            } else
                // IR allows mismatched types in expressions (e.g. float2 * float), but they need special
                // handling in SPIR-V
                if (leftType.isVector() && rightType.isNumeric()) {
                    if (resultType.getComponentType().isFloat()) {
                        if (op == Operator.MUL) {
                            int resultId = getUniqueId(resultType);
                            int typeId = writeType(resultType);
                            writeInstruction(SpvOpVectorTimesScalar, typeId, resultId,
                                    lhs, rhs, output);
                            return resultId;
                        }
                    }
                    // Vectorize the right-hand side.
                    rhs = broadcast(leftType, rhs, output);
                    operandType = leftType;
                } else if (leftType.isNumeric() && rightType.isVector()) {
                    if (resultType.getComponentType().isFloat()) {
                        if (op == Operator.MUL) {
                            int resultId = getUniqueId(resultType);
                            int typeId = writeType(resultType);
                            writeInstruction(SpvOpVectorTimesScalar, typeId, resultId,
                                    rhs, lhs, output);
                            return resultId;
                        }
                    }
                    // Vectorize the left-hand side.
                    lhs = broadcast(rightType, lhs, output);
                    operandType = rightType;
                } else if (leftType.isMatrix() || rightType.isMatrix()) {
                    if (op == Operator.MUL) {
                        // Matrix-times-vector and matrix-times-scalar have dedicated ops in SPIR-V.
                        int spvOp;
                        if (leftType.isMatrix() && rightType.isVector()) {
                            spvOp = SpvOpMatrixTimesVector;
                        } else if (leftType.isVector() && rightType.isMatrix()) {
                            spvOp = SpvOpVectorTimesMatrix;
                        } else if (leftType.isScalar() || rightType.isScalar()) {
                            spvOp = SpvOpMatrixTimesScalar;
                        } else {
                            getContext().error(pos, "unsupported mixed-type expression");
                            return NONE_ID;
                        }
                        int resultId = getUniqueId(resultType);
                        int typeId = writeType(resultType);
                        if (leftType.isScalar()) {
                            writeInstruction(spvOp, typeId, resultId,
                                    rhs, lhs, output);
                        } else {
                            writeInstruction(spvOp, typeId, resultId,
                                    lhs, rhs, output);
                        }
                        return resultId;
                    } else {
                        assert (leftType.isMatrix() && rightType.isScalar()) ||
                                (rightType.isMatrix() && leftType.isScalar());
                        return switch (op) {
                            case ADD -> writeBinaryMatrixOperation(pos,
                                    resultType, leftType, rightType, lhs, rhs,
                                    SpvOpFAdd, SpvOpIAdd, SpvOpIAdd,
                                    output);
                            case SUB -> writeBinaryMatrixOperation(pos,
                                    resultType, leftType, rightType, lhs, rhs,
                                    SpvOpFSub, SpvOpISub, SpvOpISub,
                                    output);
                            case DIV -> writeBinaryMatrixOperation(pos,
                                    resultType, leftType, rightType, lhs, rhs,
                                    SpvOpFDiv, SpvOpSDiv, SpvOpUDiv,
                                    output);
                            default -> {
                                getContext().error(pos, "unsupported mixed-type expression");
                                yield NONE_ID;
                            }
                        };
                    }
                } else {
                    getContext().error(pos, "unsupported mixed-type expression");
                    return NONE_ID;
                }
        }

        switch (op) {
            case EQ:
                if (operandType.isMatrix()) {
                    return writeMatrixComparison(operandType, lhs, rhs,
                            SpvOpFOrdEqual, SpvOpIEqual, SpvOpAll, SpvOpLogicalAnd, output);
                }
                if (operandType.isArray()) {
                    return writeArrayComparison(operandType, lhs, op, rhs, output);
                }
            {
                assert resultType.isBoolean();
                Type tmpType;
                if (operandType.isVector()) {
                    tmpType = resultType.toVector(getContext(), operandType.getRows());
                } else {
                    tmpType = resultType;
                }
                int id = writeBinaryOperation(pos,
                        tmpType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFOrdEqual, SpvOpIEqual,
                        SpvOpIEqual, SpvOpLogicalEqual,
                        output);
                if (operandType.isVector()) {
                    int resultId = getUniqueId();
                    int typeId = writeType(resultType);
                    writeInstruction(SpvOpAll, typeId, resultId, id, output);
                    return resultId;
                }
                return id;
            }
            case NE:
                if (operandType.isMatrix()) {
                    return writeMatrixComparison(operandType, lhs, rhs,
                            SpvOpFUnordNotEqual, SpvOpINotEqual, SpvOpAny, SpvOpLogicalOr, output);
                }
                if (operandType.isArray()) {
                    return writeArrayComparison(operandType, lhs, op, rhs, output);
                }
                // fallthrough
            case LOGICAL_XOR: {
                assert resultType.isBoolean();
                Type tmpType;
                if (operandType.isVector()) {
                    tmpType = resultType.toVector(getContext(), operandType.getRows());
                } else {
                    tmpType = resultType;
                }
                int id = writeBinaryOperation(pos,
                        tmpType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFUnordNotEqual, SpvOpINotEqual,
                        SpvOpINotEqual, SpvOpLogicalNotEqual,
                        output);
                if (operandType.isVector()) {
                    int resultId = getUniqueId();
                    int typeId = writeType(resultType);
                    writeInstruction(SpvOpAny, typeId, resultId, id, output);
                    return resultId;
                }
                return id;
            }
            case LOGICAL_AND:
                assert resultType.isBoolean();
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpUndef, SpvOpUndef, SpvOpUndef, SpvOpLogicalAnd,
                        output);
            case LOGICAL_OR:
                assert resultType.isBoolean();
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpUndef, SpvOpUndef, SpvOpUndef, SpvOpLogicalOr,
                        output);
            case LT:
                assert resultType.isBoolean();
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFOrdLessThan, SpvOpSLessThan,
                        SpvOpULessThan, SpvOpUndef,
                        output);
            case GT:
                assert resultType.isBoolean();
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFOrdGreaterThan, SpvOpSGreaterThan,
                        SpvOpUGreaterThan, SpvOpUndef,
                        output);
            case LE:
                assert resultType.isBoolean();
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFOrdLessThanEqual, SpvOpSLessThanEqual,
                        SpvOpULessThanEqual, SpvOpUndef,
                        output);
            case GE:
                assert resultType.isBoolean();
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFOrdGreaterThanEqual, SpvOpSGreaterThanEqual,
                        SpvOpUGreaterThanEqual, SpvOpUndef,
                        output);
            case ADD:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/true,
                        SpvOpFAdd, SpvOpIAdd, SpvOpIAdd, SpvOpUndef,
                        output);
            case SUB:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/true,
                        SpvOpFSub, SpvOpISub, SpvOpISub, SpvOpUndef,
                        output);
            case MUL:
                if (leftType.isMatrix() && rightType.isMatrix()) {
                    int resultId = getUniqueId(resultType);
                    int typeId = writeType(resultType);
                    writeInstruction(SpvOpMatrixTimesMatrix, typeId, resultId,
                            lhs, rhs, output);
                    return resultId;
                }
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFMul, SpvOpIMul, SpvOpIMul, SpvOpUndef,
                        output);
            case DIV:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/true,
                        SpvOpFDiv, SpvOpSDiv, SpvOpUDiv, SpvOpUndef,
                        output);
            case MOD:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpFMod, SpvOpSMod, SpvOpUMod, SpvOpUndef,
                        output);
            case SHL:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpUndef, SpvOpShiftLeftLogical, SpvOpShiftLeftLogical, SpvOpUndef,
                        output);
            case SHR:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpUndef, SpvOpShiftRightArithmetic, SpvOpShiftRightLogical, SpvOpUndef,
                        output);
            case BITWISE_XOR:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpUndef, SpvOpBitwiseXor, SpvOpBitwiseXor, SpvOpUndef,
                        output);
            case BITWISE_AND:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpUndef, SpvOpBitwiseAnd, SpvOpBitwiseAnd, SpvOpUndef,
                        output);
            case BITWISE_OR:
                return writeBinaryOperation(pos,
                        resultType, operandType, lhs, rhs,
                        /*matrixOpIsComponentWise*/false,
                        SpvOpUndef, SpvOpBitwiseOr, SpvOpBitwiseOr, SpvOpUndef,
                        output);
            default:
                getContext().error(pos, "unsupported expression");
                return NONE_ID;
        }
    }

    // raw binary op
    // matrix * matrix ,    component-wise
    // matrix op matrix,    op in {+, -, /}
    // matrix op scalar,    op in {+, -, /}
    // scalar op matrix,    op in {+, -, /}
    private int writeBinaryMatrixOperation(@NonNull Type resultType,
                                           @NonNull Type leftType, @NonNull Type rightType,
                                           int lhs, int rhs, // SpvId
                                           int op, // SpvOp
                                           Output output) {
        assert resultType.isMatrix();
        boolean leftMat = leftType.isMatrix();
        boolean rightMat = rightType.isMatrix();
        assert leftMat || rightMat;
        Type columnType = resultType.getComponentType().toVector(getContext(), resultType.getRows());
        int columnTypeId = writeType(columnType);

        if (leftType.isScalar()) {
            lhs = broadcast(columnType, lhs, output);
        }
        if (rightType.isScalar()) {
            rhs = broadcast(columnType, rhs, output);
        }

        // do each vector op
        IntArrayList columns = obtainIdList();
        for (int i = 0; i < resultType.getCols(); i++) {
            int leftColumn = leftMat ? writeOpCompositeExtract(columnType, lhs, i, output) : lhs;
            int rightColumn = rightMat ? writeOpCompositeExtract(columnType, rhs, i, output) : rhs;
            int resultId = getUniqueId(resultType);
            writeInstruction(op, columnTypeId, resultId, leftColumn, rightColumn, output);
            columns.add(resultId);
        }
        int resultId = writeOpCompositeConstruct(resultType, columns, output);
        releaseIdList(columns);
        return resultId;
    }

    // raw binary op
    private int writeBinaryMatrixOperation(int pos, Type resultType,
                                           Type leftType, Type rightType,
                                           int lhs, int rhs, // SpvId
                                           int floatOp, int signedOp, int unsignedOp, // SpvOp
                                           Output output) {
        int op = select_by_component_type(resultType, floatOp, signedOp, unsignedOp, SpvOpUndef);
        if (op == SpvOpUndef) {
            getContext().error(pos,
                    "unsupported operation for binary expression");
            return NONE_ID;
        }
        return writeBinaryMatrixOperation(resultType, leftType, rightType,
                lhs, rhs, op, output);
    }

    // raw binary op
    private int writeBinaryOperation(int pos, Type resultType, Type operandType,
                                     int lhs, int rhs, // SpvId
                                     boolean matrixOpIsComponentWise,
                                     int floatOp, int signedOp, int unsignedOp, int booleanOp, // SpvOp
                                     Output output) {
        int op = select_by_component_type(operandType, floatOp, signedOp, unsignedOp, booleanOp);
        if (op == SpvOpUndef) {
            getContext().error(pos,
                    "unsupported operand type for binary expression: " + operandType);
            return NONE_ID;
        }
        if (matrixOpIsComponentWise && operandType.isMatrix()) {
            return writeBinaryMatrixOperation(resultType, operandType, operandType,
                    lhs, rhs, op, output);
        }
        int resultId = getUniqueId(resultType);
        int typeId = writeType(resultType);
        writeInstruction(op, typeId, resultId, lhs, rhs, output);
        return resultId;
    }

    private int writeArrayComparison(Type arrayType, int lhs, Operator op,
                                     int rhs, Output output) {
        // The inputs must be arrays, and the op must be == or !=.
        assert (op == Operator.EQ || op == Operator.NE);
        assert (arrayType.isArray());
        Type elementType = arrayType.getElementType();
        int arraySize = arrayType.getArraySize();
        assert (arraySize > 0);

        // Synthesize equality checks for each item in the array.
        Type boolType = getContext().getTypes().mBool;
        int result = 0;
        for (int index = 0; index < arraySize; ++index) {
            // Get the left and right item in the array.
            int itemL = writeOpCompositeExtract(elementType, lhs, index, output);
            int itemR = writeOpCompositeExtract(elementType, rhs, index, output);
            // Use `writeBinaryExpression` with the requested == or != operator on these items.
            int comparison = writeBinaryExpression(Position.NO_POS, elementType, itemL, op,
                    elementType, itemR, boolType, output);
            // Merge this comparison result with all the other comparisons we've done.
            result = mergeComparisons(comparison, result, op, output);
        }
        return result;
    }

    private int mergeComparisons(int comparison, int result, Operator op,
                                 Output output) {
        // If this is the first entry, we don't need to merge comparison results with anything.
        if (result == 0) {
            return comparison;
        }
        // Use LogicalAnd or LogicalOr to combine the comparison with all the other comparisons.
        Type boolType = getContext().getTypes().mBool;
        int boolTypeId = writeType(boolType);
        int next = getUniqueId();
        switch (op) {
            case EQ:
                writeInstruction(SpvOpLogicalAnd, boolTypeId, next,
                        comparison, result, output);
                break;
            case NE:
                writeInstruction(SpvOpLogicalOr, boolTypeId, next,
                        comparison, result, output);
                break;
            default:
                assert false : ("mergeComparisons only supports == and !=, not " + op);
                return NONE_ID;
        }
        return next;
    }

    private int writeConditionalExpression(ConditionalExpression expr, Output output) {
        Type type = expr.getType();
        int cond = writeExpression(expr.getCondition(), output);
        int trueId = writeExpression(expr.getWhenTrue(), output);
        int falseId = writeExpression(expr.getWhenFalse(), output);
        int resultId = getUniqueId();
        int typeId = writeType(type);

        // result type is scalar or vector, and expressions are trivial
        if (type.isScalar() || type.isVector()) {
            if (getContext().getOptions().mNoShortCircuit ||
                    (Analysis.isTrivialExpression(expr.getWhenTrue()) &&
                            Analysis.isTrivialExpression(expr.getWhenFalse()))) {
                // use OpSelect for this ?: expression

                // broadcast condition to vector, if necessary (AST is always scalar)
                // Before 1.4, like for mix(), starting with 1.4, keep it scalar
                if (mOutputVersion.isBefore(SPIRVVersion.SPIRV_1_4) && type.isVector()) {
                    Type condType = expr.getCondition().getType();
                    assert condType.isBoolean();
                    cond = broadcast(condType.toVector(getContext(), type.getRows()), cond, output);
                }

                writeInstruction(SpvOpSelect, typeId, resultId,
                        cond, trueId, falseId, output);
                return resultId;
            }
        }

        int numReachableOps = mReachableOps.size();
        int numStoreOps = mStoreOps.size();

        // similar to glslang
        int variable = getUniqueId();
        int ptrTypeId = writePointerType(type, SpvStorageClassFunction);
        writeInstruction(SpvOpVariable, ptrTypeId,
                variable, SpvStorageClassFunction, mVariableBuffer);
        int trueLabel = getUniqueId();
        int falseLabel = getUniqueId();
        int end = getUniqueId();
        writeInstruction(SpvOpSelectionMerge, end, SpvSelectionControlMaskNone, output);
        writeInstruction(SpvOpBranchConditional, cond, trueLabel, falseLabel, output);
        writeLabel(trueLabel, output);
        writeOpStore(SpvStorageClassFunction, variable, trueId, output);
        writeInstruction(SpvOpBranch, end, output);
        writeLabel(falseLabel, kBranchIsAbove, numReachableOps, numStoreOps, output);
        writeOpStore(SpvStorageClassFunction, variable, falseId, output);
        writeInstruction(SpvOpBranch, end, output);
        writeLabel(end, kBranchIsAbove, numReachableOps, numStoreOps, output);
        writeInstruction(SpvOpLoad, typeId, resultId,
                variable, output);

        return resultId;
    }

    private int writeVariableReference(VariableReference ref, Output output) {
        Expression constExpr = ConstantFolder.getConstantValueOrNullForVariable(ref);
        if (constExpr != null) {
            return writeExpression(constExpr, output);
        }
        return writeLValue(ref, output).load(this, output);
    }

    private int writeIndexExpression(IndexExpression expr, Output output) {
        if (expr.getBase().getType().isVector()) {
            int base = writeExpression(expr.getBase(), output);
            int index = writeExpression(expr.getIndex(), output);
            int resultId = getUniqueId();
            int typeId = writeType(expr.getType());
            writeInstruction(SpvOpVectorExtractDynamic, typeId, resultId,
                    base, index, output);
            return resultId;
        }
        return writeLValue(expr, output).load(this, output);
    }

    private int writeFieldAccess(FieldAccess f, Output output) {
        return writeLValue(f, output).load(this, output);
    }

    private int writeRValueSwizzle(Expression baseExpr,
                                   byte[] components, Output output) {
        int count = components.length;
        assert count >= 1 && count <= 4;
        Type type = baseExpr.getType().getComponentType().toVector(getContext(), count);
        int baseId = writeExpression(baseExpr, output);

        if (count == 1) {
            return writeOpCompositeExtract(type, baseId, components[0], output);
        }

        int resultId = getUniqueId(type);
        int typeId = writeType(type);
        writeOpcode(SpvOpVectorShuffle, 5 + count, output);
        output.writeWord(typeId);
        output.writeWord(resultId);
        output.writeWord(baseId);
        output.writeWord(baseId);
        for (int component : components) {
            output.writeWord(component); // 0...3
        }
        return resultId;
    }

    private int writeSwizzle(Swizzle swizzle, Output output) {
        return writeRValueSwizzle(swizzle.getBase(), swizzle.getComponents(), output);
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

    private void writeStatement(@NonNull Statement stmt, Output output) {
        switch (stmt.getKind()) {
            case BLOCK -> writeBlockStatement((BlockStatement) stmt, output);
            case RETURN -> writeReturnStatement((ReturnStatement) stmt, output);
            case IF -> writeIfStatement((IfStatement) stmt, output);
            case FOR_LOOP -> writeForLoop((ForLoop) stmt, output);
            case SWITCH -> writeSwitchStatement((SwitchStatement) stmt, output);
            case VARIABLE_DECL -> writeVariableDecl((VariableDecl) stmt, output);
            case EXPRESSION -> writeExpression(((ExpressionStatement) stmt).getExpression(), output);
            case BREAK -> writeInstruction(SpvOpBranch, mBreakTarget.topInt(), output);
            case CONTINUE -> writeInstruction(SpvOpBranch, mContinueTarget.topInt(), output);
            case DISCARD -> {
                if (mOutputVersion.isAtLeast(SPIRVVersion.SPIRV_1_6)) {
                    writeInstruction(SpvOpTerminateInvocation, output);
                } else {
                    writeInstruction(SpvOpKill, output);
                }
            }
            default -> getContext().error(stmt.mPosition, "unsupported statement: " + stmt.getKind());
        }
    }

    private static int select_by_component_type(@NonNull Type type,
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

    private void writeBlockStatement(BlockStatement b, Output output) {
        for (var stmt : b.getStatements()) {
            writeStatement(stmt, output);
        }
    }

    private void writeReturnStatement(ReturnStatement r, Output output) {
        if (r.getExpression() != null) {
            int expr = writeExpression(r.getExpression(), output);
            writeInstruction(SpvOpReturnValue, expr, output);
        } else {
            writeInstruction(SpvOpReturn, output);
        }
    }

    private void writeIfStatement(IfStatement stmt, Output output) {
        int cond = writeExpression(stmt.getCondition(), output);
        int trueId = getUniqueId();
        int falseId = getUniqueId();

        int numReachableOps = mReachableOps.size();
        int numStoreOps = mStoreOps.size();

        if (stmt.getWhenFalse() != null) {
            int end = getUniqueId();
            writeInstruction(SpvOpSelectionMerge, end, SpvSelectionControlMaskNone, output);
            writeInstruction(SpvOpBranchConditional, cond, trueId, falseId, output);
            writeLabel(trueId, output);
            writeStatement(stmt.getWhenTrue(), output);
            if (mCurrentBlock != 0) {
                writeInstruction(SpvOpBranch, end, output);
            }
            writeLabel(falseId, kBranchIsAbove, numReachableOps, numStoreOps, output);
            writeStatement(stmt.getWhenFalse(), output);
            if (mCurrentBlock != 0) {
                writeInstruction(SpvOpBranch, end, output);
            }
            writeLabel(end, kBranchIsAbove, numReachableOps, numStoreOps, output);
        } else {
            writeInstruction(SpvOpSelectionMerge, falseId, SpvSelectionControlMaskNone, output);
            writeInstruction(SpvOpBranchConditional, cond, trueId, falseId, output);
            writeLabel(trueId, output);
            writeStatement(stmt.getWhenTrue(), output);
            if (mCurrentBlock != 0) {
                writeInstruction(SpvOpBranch, falseId, output);
            }
            writeLabel(falseId, kBranchIsAbove, numReachableOps, numStoreOps, output);
        }
    }

    private void writeForLoop(ForLoop f, Output output) {
        if (f.getInit() != null) {
            writeStatement(f.getInit(), output);
        }

        int numReachableOps = mReachableOps.size();
        int numStoreOps = mStoreOps.size();

        // The store cache isn't trustworthy in the presence of branches; store caching only makes sense
        // in the context of linear straight-line execution. If we wanted to be more clever, we could
        // only invalidate store cache entries for variables affected by the loop body, but for now we
        // simply clear the entire cache whenever branching occurs.
        int header = getUniqueId();
        int start = getUniqueId();
        int body = getUniqueId();
        int next = getUniqueId();
        mContinueTarget.push(next);
        int end = getUniqueId();
        mBreakTarget.push(end);
        writeInstruction(SpvOpBranch, header, output);
        writeLabel(header, kBranchIsBelow, numReachableOps, numStoreOps, output);
        writeInstruction(SpvOpLoopMerge, end, next, SpvLoopControlMaskNone, output);
        writeInstruction(SpvOpBranch, start, output);
        writeLabel(start, output);
        if (f.getCondition() != null) {
            int cond = writeExpression(f.getCondition(), output);
            writeInstruction(SpvOpBranchConditional, cond, body, end, output);
        } else {
            writeInstruction(SpvOpBranch, body, output);
        }
        writeLabel(body, output);
        writeStatement(f.getStatement(), output);
        if (mCurrentBlock != 0) {
            writeInstruction(SpvOpBranch, next, output);
        }
        writeLabel(next, kBranchIsAbove, numReachableOps, numStoreOps, output);
        if (f.getStep() != null) {
            writeExpression(f.getStep(), output);
        }
        writeInstruction(SpvOpBranch, header, output);
        writeLabel(end, kBranchIsAbove, numReachableOps, numStoreOps, output);
        mBreakTarget.popInt();
        mContinueTarget.popInt();
    }

    private void writeSwitchStatement(SwitchStatement s, Output output) {
        int init = writeExpression(s.getInit(), output);

        int numReachableOps = mReachableOps.size();
        int numStoreOps = mStoreOps.size();

        IntArrayList labels = obtainIdList();
        int end = getUniqueId();
        int defaultLabel = end;
        mBreakTarget.push(end);
        int size = 3;
        List<Statement> cases = s.getCases();
        for (Statement stmt : cases) {
            SwitchCase sc = (SwitchCase) stmt;
            int label = getUniqueId();
            labels.add(label);
            if (!sc.isDefault()) {
                size += 2;
            } else {
                defaultLabel = label;
            }
        }

        // We should have exactly one label for each case.
        assert (labels.size() == cases.size());

        // Collapse adjacent switch-cases into one; that is, reduce `case 1: case 2: case 3:` into a
        // single OpLabel. The Tint SPIR-V reader does not support switch-case fallthrough, but it
        // does support multiple switch-cases branching to the same label.
        BitSet caseIsCollapsed = new BitSet(cases.size());
        for (int index = cases.size() - 2; index >= 0; index--) {
            SwitchCase sc = (SwitchCase) cases.get(index);
            if (sc.getStatement().isEmpty()) {
                caseIsCollapsed.set(index);
                labels.set(index, labels.getInt(index + 1));
            }
        }

        labels.add(end);

        writeInstruction(SpvOpSelectionMerge, end, SpvSelectionControlMaskNone, output);
        writeOpcode(SpvOpSwitch, size, output);
        output.writeWord(init);
        output.writeWord(defaultLabel);

        for (int i = 0; i < cases.size(); ++i) {
            SwitchCase sc = (SwitchCase) cases.get(i);
            if (sc.isDefault()) {
                continue;
            }
            output.writeWord((int) sc.getValue());
            output.writeWord(labels.getInt(i));
        }
        for (int i = 0; i < cases.size(); ++i) {
            if (caseIsCollapsed.get(i)) {
                continue;
            }
            SwitchCase sc = (SwitchCase) cases.get(i);
            if (i == 0) {
                writeLabel(labels.getInt(i), output);
            } else {
                writeLabel(labels.getInt(i), kBranchIsAbove, numReachableOps, numStoreOps, output);
            }
            writeStatement(sc.getStatement(), output);
            if (mCurrentBlock != 0) {
                writeInstruction(SpvOpBranch, labels.getInt(i + 1), output);
            }
        }
        writeLabel(end, kBranchIsAbove, numReachableOps, numStoreOps, output);
        mBreakTarget.popInt();
    }

    private void writeLabel(int label, Output output) {
        assert mCurrentBlock == 0;
        mCurrentBlock = label;
        writeInstruction(SpvOpLabel, label, output);
    }

    private void writeLabel(int label, int type, int numReachableOps,
                            int numStoreOps, Output output) {
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
        writeLabel(label, output);
    }

    private void writeAccessChain(@NonNull Expression expr, Output output, IntList chain) {
        switch (expr.getKind()) {
            case INDEX -> {
                IndexExpression indexExpr = (IndexExpression) expr;
                if (indexExpr.getBase() instanceof Swizzle) {
                    //TODO
                    getContext().error(indexExpr.mPosition, "indexing on swizzle is not allowed");
                }
                writeAccessChain(indexExpr.getBase(), output, chain);
                int id = writeExpression(indexExpr.getIndex(), output);
                chain.add(id);
            }
            case FIELD_ACCESS -> {
                FieldAccess fieldAccess = (FieldAccess) expr;
                writeAccessChain(fieldAccess.getBase(), output, chain);
                int id = writeScalarConstant(fieldAccess.getFieldIndex(), getContext().getTypes().mInt);
                chain.add(id);
            }
            default -> {
                int id = writeLValue(expr, output).getPointer();
                assert id != NONE_ID;
                chain.add(id);
            }
        }
    }

    @NonNull
    private LValue writeLValue(@NonNull Expression expr, Output output) {
        Type type = expr.getType();
        boolean relaxedPrecision = type.isRelaxedPrecision();
        switch (expr.getKind()) {
            case INDEX, FIELD_ACCESS -> {
                IntArrayList chain = mAccessChain;
                chain.clear();
                writeAccessChain(expr, output, chain);
                int member = getUniqueId();
                int storageClass = getStorageClass(expr);
                writeOpcode(SpvOpAccessChain, 3 + chain.size(), output);
                output.writeWord(writePointerType(type, storageClass));
                output.writeWord(member);
                output.writeWords(chain.elements(), chain.size());
                //TODO layout may be wrong
                int typeId = writeType(type, null, null);
                return new PointerLValue(member, false, typeId,
                        relaxedPrecision, storageClass);
            }
            case VARIABLE_REFERENCE -> {
                Variable variable = ((VariableReference) expr).getVariable();

                int entry = mVariableTable.getInt(variable);
                assert entry != 0 : variable;

                //TODO layout may be wrong
                int typeId = writeType(type, variable.getModifiers(), null);
                return new PointerLValue(entry, true, typeId,
                        relaxedPrecision, getStorageClass(expr));
            }
            case SWIZZLE -> {
                Swizzle swizzle = (Swizzle) expr;
                LValue lvalue = writeLValue(swizzle.getBase(), output);
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
                    writeInstruction(SpvOpAccessChain, typeId, member, base, indexId, output);
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
                assert false : expr;
                throw new UnsupportedOperationException();
            }
        }
    }

    private void writeFunctionCallArgument(IntArrayList argList,
                                           FunctionCall call,
                                           int argIndex,
                                           ArrayList<OutVar> tmpOutVars,
                                           Output output) {
        FunctionDecl funcDecl = call.getFunction();
        Expression arg = call.getArguments()[argIndex];
        Variable param = funcDecl.getParameters().get(argIndex);
        Modifiers paramModifiers = param.getModifiers();

        if (arg instanceof VariableReference && arg.getType().isOpaque()) {
            // Opaque handle (sampler/texture) arguments are always declared as pointers but never
            // stored in intermediates when calling user-defined functions.
            //
            // The case for intrinsics (which take opaque arguments by value) is handled above just like
            // regular pointers.
            //
            // See getFunctionParameterType for further explanation.
            Variable variable = ((VariableReference) arg).getVariable();

            int entry = mVariableTable.getInt(variable);
            assert entry != 0;
            argList.add(entry);
            return;
        }

        // ID of temporary variable that we will use to hold this argument, or 0 if it is being
        // passed directly
        int tmpVar;
        // if we need a temporary var to store this argument, this is the value to store in the var
        int tmpValueId = NONE_ID;

        if ((paramModifiers.flags() & Modifiers.kOut_Flag) != 0) {
            LValue lValue = writeLValue(arg, output);
            // We handle out params with a temp var that we copy back to the original variable at the
            // end of the call. GLSL guarantees that the original variable will be unchanged until the
            // end of the call, and also that out params are written back to their original variables in
            // a specific order (left-to-right), so it's unsafe to pass a pointer to the original value.
            if ((paramModifiers.flags() & Modifiers.kIn_Flag) != 0) {
                tmpValueId = lValue.load(this, output);
            }
            tmpVar = getUniqueId(arg.getType());
            tmpOutVars.add(new OutVar(tmpVar, arg.getType(), lValue));
        } else if (funcDecl.isIntrinsic()) {
            // Unlike user function calls, non-out intrinsic arguments don't need pointer parameters.
            tmpValueId = writeExpression(arg, output);
            argList.add(tmpValueId);
            return;
        } else {
            // We always use pointer parameters when calling user functions.
            // See getFunctionParameterType for further explanation.
            tmpValueId = writeExpression(arg, output);
            tmpVar = getUniqueId();
        }

        int ptrTypeId = writePointerType(arg.getType(), SpvStorageClassFunction);
        writeInstruction(SpvOpVariable,
                ptrTypeId,
                tmpVar,
                SpvStorageClassFunction,
                mVariableBuffer);
        if (tmpValueId != NONE_ID) {
            writeOpStore(SpvStorageClassFunction, tmpVar, tmpValueId, output);
        }
        argList.add(tmpVar);
    }

    private void copyBackOutArguments(ArrayList<OutVar> tmpOutVars, Output output) {
        for (OutVar outVar : tmpOutVars) {
            int loadId = getUniqueId(outVar.mType);
            int typeId = writeType(outVar.mType);
            writeInstruction(SpvOpLoad, typeId, loadId, outVar.mId, output);
            outVar.mLValue.store(this, loadId, output);
        }
    }

    private void writeGLSLExtendedInstruction(Type type, int id,
                                              int floatInst, int signedInst, int unsignedInst,
                                              IntArrayList args, Output output) {
        writeOpcode(SpvOpExtInst, 5 + args.size(), output);
        int typeId = writeType(type);
        output.writeWord(typeId);
        output.writeWord(id);
        output.writeWord(mGLSLExtendedInstructions);
        output.writeWord(select_by_component_type(type, floatInst, signedInst, unsignedInst, GLSLstd450Bad));
        output.writeWords(args.elements(), args.size());
    }

    private int writeSpecialIntrinsic(FunctionCall call, int kind, Output output) {
        Expression[] arguments = call.getArguments();
        IntArrayList argumentIds = obtainIdList();
        int resultId = getUniqueId();
        Type callType = call.getType();
        switch (kind) {
            case kAtan_SpecialIntrinsic -> {
                for (var arg : arguments) {
                    argumentIds.add(writeExpression(arg, output));
                }
                if (argumentIds.size() == 2) {
                    writeGLSLExtendedInstruction(callType, resultId,
                            GLSLstd450Atan2, GLSLstd450Bad, GLSLstd450Bad,
                            argumentIds, output);
                } else {
                    assert argumentIds.size() == 1;
                    writeGLSLExtendedInstruction(callType, resultId,
                            GLSLstd450Atan, GLSLstd450Bad, GLSLstd450Bad,
                            argumentIds, output);
                }
            }
            case kMod_SpecialIntrinsic -> {
                vectorize(arguments, argumentIds, output);
                assert argumentIds.size() == 2;
                Type operandType = arguments[0].getType();
                int op = select_by_component_type(operandType,
                        SpvOpFMod, SpvOpSMod, SpvOpUMod, SpvOpUndef);
                assert op != SpvOpUndef;
                int typeId = writeType(operandType);
                writeInstruction(op, typeId, resultId,
                        argumentIds.getInt(0),
                        argumentIds.getInt(1),
                        output);
            }
            case kMin_SpecialIntrinsic -> {
                vectorize(arguments, argumentIds, output);
                assert argumentIds.size() == 2;
                writeGLSLExtendedInstruction(callType, resultId,
                        GLSLstd450FMin, GLSLstd450SMin, GLSLstd450UMin,
                        argumentIds, output);
            }
            case kMax_SpecialIntrinsic -> {
                vectorize(arguments, argumentIds, output);
                assert argumentIds.size() == 2;
                writeGLSLExtendedInstruction(callType, resultId,
                        GLSLstd450FMax, GLSLstd450SMax, GLSLstd450UMax,
                        argumentIds, output);
            }
            case kClamp_SpecialIntrinsic -> {
                vectorize(arguments, argumentIds, output);
                assert argumentIds.size() == 3;
                writeGLSLExtendedInstruction(callType, resultId,
                        GLSLstd450FClamp, GLSLstd450SClamp, GLSLstd450UClamp,
                        argumentIds, output);
            }
            case kSaturate_SpecialIntrinsic -> {
                assert (arguments.length == 1);
                int vectorSize = arguments[0].getType().getRows();
                argumentIds.add(vectorize(arguments[0], vectorSize, output));
                argumentIds.add(vectorize(0.0f, getContext().getTypes().mFloat, vectorSize, output));
                argumentIds.add(vectorize(1.0f, getContext().getTypes().mFloat, vectorSize, output));
                writeGLSLExtendedInstruction(callType, resultId,
                        GLSLstd450FClamp, GLSLstd450SClamp, GLSLstd450UClamp,
                        argumentIds, output);
            }
            case kMix_SpecialIntrinsic -> {
                vectorize(arguments, argumentIds, output);
                assert argumentIds.size() == 3;
                if (arguments[2].getType().isBooleanOrCompound()) {
                    // Use OpSelect to implement Boolean mix().
                    int typeId = writeType(arguments[0].getType());
                    writeInstruction(SpvOpSelect, typeId, resultId,
                            argumentIds.getInt(2),
                            argumentIds.getInt(1),
                            argumentIds.getInt(0),
                            output);
                } else {
                    writeGLSLExtendedInstruction(callType, resultId,
                            GLSLstd450FMix, SpvOpUndef, SpvOpUndef,
                            argumentIds, output);
                }
            }
            case kStep_SpecialIntrinsic -> {
                vectorize(arguments, argumentIds, output);
                assert argumentIds.size() == 2;
                writeGLSLExtendedInstruction(callType, resultId,
                        GLSLstd450Step, SpvOpUndef, SpvOpUndef,
                        argumentIds, output);
            }
            case kSmoothStep_SpecialIntrinsic -> {
                vectorize(arguments, argumentIds, output);
                assert argumentIds.size() == 3;
                writeGLSLExtendedInstruction(callType, resultId,
                        GLSLstd450SmoothStep, GLSLstd450Bad, GLSLstd450Bad,
                        argumentIds, output);
            }
            case kTexture_SpecialIntrinsic -> {
                for (var arg : arguments) {
                    argumentIds.add(writeExpression(arg, output));
                }
                int typeId = writeType(callType);
                if (argumentIds.size() == 3) {
                    writeInstruction(SpvOpImageSampleImplicitLod,
                            typeId, resultId,
                            argumentIds.getInt(0),
                            argumentIds.getInt(1),
                            SpvImageOperandsBiasMask,
                            argumentIds.getInt(2),
                            output);
                } else {
                    assert argumentIds.size() == 2;
                    writeInstruction(SpvOpImageSampleImplicitLod,
                            typeId, resultId,
                            argumentIds.getInt(0),
                            argumentIds.getInt(1),
                            output);
                }
            }
            case kTextureFetch_SpecialIntrinsic -> {
                for (var arg : arguments) {
                    argumentIds.add(writeExpression(arg, output));
                }
                //TODO for 1D, 2D (non-MS), 3D, there are three args; but not for others
                assert argumentIds.size() == 3;
                if (arguments[0].getType().isCombinedSampler()) {
                    // extract the image type
                    int samplerId = argumentIds.getInt(0);
                    int imageId = getUniqueId();
                    int samplerTypeId = writeType(arguments[0].getType());
                    // OpTypeSampledImage ResultID ImageType
                    int imageTypeId = mSpvIdCache.get(samplerTypeId)
                            .mWords[1];
                    writeInstruction(SpvOpImage, imageTypeId, imageId,
                            samplerId, output);
                    argumentIds.set(0, imageId);
                }
                int typeId = writeType(callType);
                writeInstruction(SpvOpImageFetch,
                        typeId, resultId,
                        argumentIds.getInt(0),
                        argumentIds.getInt(1),
                        SpvImageOperandsLodMask,
                        argumentIds.getInt(2),
                        output);
            }
            default -> {
                assert false;
            }
        }
        releaseIdList(argumentIds);
        return resultId;
    }

    private int writeIntrinsicCall(FunctionCall call, Output output) {
        FunctionDecl funcDecl = call.getFunction();
        assert funcDecl.isIntrinsic();
        int dataIndex = funcDecl.getIntrinsicKind() * kIntrinsicDataColumn;
        if (sIntrinsicData[dataIndex] == kInvalid_IntrinsicOpcodeKind) {
            getContext().error(call.mPosition, "unsupported intrinsic '" +
                    funcDecl + "'");
            return NONE_ID;
        }

        Expression[] arguments = call.getArguments();
        int intrinsicId = sIntrinsicData[dataIndex + 1];
        if (arguments.length > 0) {
            Type type = arguments[0].getType();
            if (sIntrinsicData[dataIndex] != kSpecial_IntrinsicOpcodeKind) {
                intrinsicId = select_by_component_type(type,
                        sIntrinsicData[dataIndex + 1], sIntrinsicData[dataIndex + 2],
                        sIntrinsicData[dataIndex + 3], sIntrinsicData[dataIndex + 4]);
            }
            // else keep the default float op.
        }
        switch (sIntrinsicData[dataIndex]) {
            case kGLSLstd450_IntrinsicOpcodeKind -> {
                int resultId = getUniqueId(call.getType());
                int typeId = writeType(call.getType());
                IntArrayList argumentIds = obtainIdList();
                ArrayList<OutVar> tmpOutVars = new ArrayList<>();
                for (int i = 0; i < arguments.length; i++) {
                    writeFunctionCallArgument(argumentIds, call, i, tmpOutVars,
                            output);
                }
                writeOpcode(SpvOpExtInst, 5 + argumentIds.size(), output);
                output.writeWord(typeId);
                output.writeWord(resultId);
                output.writeWord(mGLSLExtendedInstructions);
                output.writeWord(intrinsicId);
                output.writeWords(argumentIds.elements(), argumentIds.size());
                copyBackOutArguments(tmpOutVars, output);
                releaseIdList(argumentIds);
                return resultId;
            }
            case kSPIRV_IntrinsicOpcodeKind -> {
                // GLSL supports dot(float, float), but SPIR-V does not. Convert it to FMul
                if (intrinsicId == SpvOpDot && arguments[0].getType().isScalar()) {
                    intrinsicId = SpvOpFMul;
                }
                int resultId = getUniqueId(call.getType());
                IntArrayList argumentIds = obtainIdList();
                ArrayList<OutVar> tmpOutVars = new ArrayList<>();
                for (int i = 0; i < arguments.length; i++) {
                    writeFunctionCallArgument(argumentIds, call, i, tmpOutVars,
                            output);
                }
                if (!call.getType().isVoid()) {
                    int typeId = writeType(call.getType());
                    writeOpcode(intrinsicId, 3 + arguments.length, output);
                    output.writeWord(typeId);
                    output.writeWord(resultId);
                } else {
                    writeOpcode(intrinsicId, 1 + arguments.length, output);
                }
                output.writeWords(argumentIds.elements(), argumentIds.size());
                copyBackOutArguments(tmpOutVars, output);
                releaseIdList(argumentIds);
                return resultId;
            }
            case kSpecial_IntrinsicOpcodeKind -> {
                return writeSpecialIntrinsic(call, intrinsicId, output);
            }
            default -> {
                getContext().error(call.mPosition, "unsupported intrinsic '" +
                        funcDecl + "'");
                return NONE_ID;
            }
        }
    }

    private int writeFunctionCall(FunctionCall call, Output output) {
        // Handle intrinsics.
        FunctionDecl funcDecl = call.getFunction();
        if (funcDecl.isIntrinsic() && funcDecl.getDefinition() == null) {
            return writeIntrinsicCall(call, output);
        }

        int entry = mFunctionTable.getInt(funcDecl);
        if (entry == 0) {
            getContext().error(call.mPosition, "function '" + funcDecl +
                    "' is not defined");
            return NONE_ID;
        }

        // Temp variables are used to write back out-parameters after the function call is complete.
        Expression[] arguments = call.getArguments();
        IntArrayList argumentIds = obtainIdList();
        ArrayList<OutVar> tmpOutVars = new ArrayList<>();
        for (int i = 0; i < arguments.length; i++) {
            writeFunctionCallArgument(argumentIds, call, i, tmpOutVars,
                    output);
        }
        int resultId = getUniqueId();
        int typeId = writeType(call.getType());
        writeOpcode(SpvOpFunctionCall, 4 + argumentIds.size(), output);
        output.writeWord(typeId);
        output.writeWord(resultId);
        output.writeWord(entry);
        output.writeWords(argumentIds.elements(), argumentIds.size());
        // Now that the call is complete, we copy temp out-variables back to their real lvalues.
        copyBackOutArguments(tmpOutVars, output);
        releaseIdList(argumentIds);
        return resultId;
    }

    // cast scalar to scalar, cast vector to vector
    private int writeConversion(int inputId,
                                Type inputType,
                                Type outputType,
                                Output output) {
        assert inputType.isScalar() || inputType.isVector();
        assert inputType.getTypeKind() == outputType.getTypeKind();
        assert inputType.getRows() == outputType.getRows();
        int vectorSize = inputType.getRows();

        if (outputType.isFloatOrCompound()) {
            // Currently we have no double type, casting a float to float is a no-op.
            if (inputType.isFloatOrCompound()) {
                assert inputType.getComponentType().getWidth() == outputType.getComponentType().getWidth();
                return inputId;
            }

            // Given the input type, generate the appropriate instruction to cast to float.
            int resultId = getUniqueId(outputType);
            int typeId = writeType(outputType);
            if (inputType.isBooleanOrCompound()) {
                // Use OpSelect to convert the boolean argument to a literal 1.0 or 0.0.
                int oneId = vectorize(1.0f, getContext().getTypes().mFloat, vectorSize, output);
                int zeroId = vectorize(0.0f, getContext().getTypes().mFloat, vectorSize, output);
                writeInstruction(SpvOpSelect, typeId, resultId,
                        inputId, oneId, zeroId, output);
            } else if (inputType.isSignedOrCompound()) {
                writeInstruction(SpvOpConvertSToF, typeId, resultId,
                        inputId, output);
            } else if (inputType.isUnsignedOrCompound()) {
                writeInstruction(SpvOpConvertUToF, typeId, resultId,
                        inputId, output);
            } else {
                assert false : ("unsupported type for float typecast: " + inputType);
                return NONE_ID;
            }
            return resultId;
        }

        if (outputType.isSignedOrCompound()) {
            // Currently we have no long type, casting a signed int to signed int is a no-op.
            if (inputType.isSignedOrCompound()) {
                assert inputType.getComponentType().getWidth() == outputType.getComponentType().getWidth();
                return inputId;
            }

            // Given the input type, generate the appropriate instruction to cast to signed int.
            int resultId = getUniqueId(outputType);
            int typeId = writeType(outputType);
            if (inputType.isBooleanOrCompound()) {
                // Use OpSelect to convert the boolean argument to a literal 1 or 0.
                int oneId = vectorize(1, getContext().getTypes().mInt, vectorSize, output);
                int zeroId = vectorize(0, getContext().getTypes().mInt, vectorSize, output);
                writeInstruction(SpvOpSelect, typeId, resultId,
                        inputId, oneId, zeroId, output);
            } else if (inputType.isFloatOrCompound()) {
                writeInstruction(SpvOpConvertFToS, typeId, resultId,
                        inputId, output);
            } else if (inputType.isUnsignedOrCompound()) {
                writeInstruction(SpvOpBitcast, typeId, resultId,
                        inputId, output);
            } else {
                assert false : ("unsupported type for signed int typecast: " + inputType);
                return NONE_ID;
            }
            return resultId;
        }

        if (outputType.isUnsignedOrCompound()) {
            // Currently we have no long type, casting a signed int to signed int is a no-op.
            if (inputType.isUnsignedOrCompound()) {
                assert inputType.getComponentType().getWidth() == outputType.getComponentType().getWidth();
                return inputId;
            }

            // Given the input type, generate the appropriate instruction to cast to unsigned int.
            int resultId = getUniqueId(outputType);
            int typeId = writeType(outputType);
            if (inputType.isBooleanOrCompound()) {
                // Use OpSelect to convert the boolean argument to a literal 1u or 0u.
                int oneId = vectorize(1, getContext().getTypes().mUInt, vectorSize, output);
                int zeroId = vectorize(0, getContext().getTypes().mUInt, vectorSize, output);
                writeInstruction(SpvOpSelect, typeId, resultId,
                        inputId, oneId, zeroId, output);
            } else if (inputType.isFloatOrCompound()) {
                writeInstruction(SpvOpConvertFToU, typeId, resultId,
                        inputId, output);
            } else if (inputType.isSignedOrCompound()) {
                writeInstruction(SpvOpBitcast, typeId, resultId,
                        inputId, output);
            } else {
                assert false : ("unsupported type for unsigned int typecast: " + inputType);
                return NONE_ID;
            }
            return resultId;
        }

        if (outputType.isBooleanOrCompound()) {
            // Casting a bool to bool is a no-op.
            if (inputType.isBooleanOrCompound()) {
                return inputId;
            }

            // Given the input type, generate the appropriate instruction to cast to bool.
            int resultId = getUniqueId();
            int typeId = writeType(outputType);
            if (inputType.isSignedOrCompound()) {
                // Synthesize a boolean result by comparing the input against a signed zero literal.
                int zeroId = vectorize(0, getContext().getTypes().mInt, vectorSize, output);
                writeInstruction(SpvOpINotEqual, typeId, resultId,
                        inputId, zeroId, output);
            } else if (inputType.isUnsignedOrCompound()) {
                // Synthesize a boolean result by comparing the input against an unsigned zero literal.
                int zeroId = vectorize(0, getContext().getTypes().mUInt, vectorSize, output);
                writeInstruction(SpvOpINotEqual, typeId, resultId,
                        inputId, zeroId, output);
            } else if (inputType.isFloatOrCompound()) {
                // Synthesize a boolean result by comparing the input against a floating-point zero literal.
                int zeroId = vectorize(0.0f, getContext().getTypes().mFloat, vectorSize, output);
                writeInstruction(SpvOpFUnordNotEqual, typeId, resultId,
                        inputId, zeroId, output);
            } else {
                assert false : ("unsupported type for boolean typecast: " + inputType);
                return NONE_ID;
            }
            return resultId;
        }

        getContext().error(Position.NO_POS, "unsupported cast: " + inputType + " to " +
                outputType);
        return inputId;
    }

    private int writeConstructorScalarCast(ConstructorScalarCast ctor, Output output) {
        Expression argument = ctor.getArgument();
        int argumentId = writeExpression(argument, output);
        return writeConversion(argumentId, argument.getType(), ctor.getType(), output);
    }

    private int writeConstructorCompoundCast(ConstructorCompoundCast ctor, Output output) {
        Type ctorType = ctor.getType();
        Type argType = ctor.getArgument().getType();
        assert (ctorType.isVector() || ctorType.isMatrix());

        // Write the composite that we are casting.
        int compositeId = writeExpression(ctor.getArgument(), output);

        if (ctorType.isMatrix()) {
            // we have only float and min16float matrix, no conversion needed
            assert ctorType.getComponentType().getWidth() == argType.getComponentType().getWidth();
            assert ctorType.getComponentType().getScalarKind() == argType.getComponentType().getScalarKind();
            return compositeId;
        }

        return writeConversion(compositeId, argType, ctorType, output);
    }

    private int writeConstructorVectorSplat(ConstructorVectorSplat ctor, Output output) {
        // Write the splat argument as a scalar, then broadcast it.
        int argument = writeExpression(ctor.getArgument(), output);
        return broadcast(ctor.getType(), argument, output);
    }

    private int writeConstructorDiagonalMatrix(ConstructorDiagonalMatrix ctor, Output output) {
        Type type = ctor.getType();
        assert (type.isMatrix());
        assert (ctor.getArgument().getType().isScalar());

        // Write out the scalar argument.
        int diagonal = writeExpression(ctor.getArgument(), output);

        // Build the diagonal matrix.
        int zeroId = writeScalarConstant(0.0f, getContext().getTypes().mFloat);

        Type columnType = type.getComponentType().toVector(getContext(), type.getRows());
        IntArrayList columnIds = obtainIdList();
        IntArrayList arguments = obtainIdList();
        for (int column = 0; column < type.getCols(); column++) {
            for (int row = 0; row < type.getRows(); row++) {
                arguments.add((row == column) ? diagonal : zeroId);
            }
            columnIds.add(writeOpCompositeConstruct(columnType, arguments, output));
            arguments.clear();
        }
        int resultId = writeOpCompositeConstruct(type, columnIds, output);
        releaseIdList(columnIds);
        releaseIdList(arguments);
        return resultId;
    }

    private int writeVectorConstructor(ConstructorCompound ctor, Output output) {
        Type type = ctor.getType();
        Type componentType = type.getComponentType();
        assert (type.isVector());

        IntArrayList argumentIds = obtainIdList();
        for (Expression arg : ctor.getArguments()) {
            Type argType = arg.getType();
            assert (componentType.getScalarKind() == argType.getComponentType().getScalarKind());

            int argId = writeExpression(arg, output);
            if (argType.isMatrix()) {
                // CompositeConstruct cannot take a 2x2 matrix as an input, so we need to extract out
                // each scalar separately.
                assert (argType.getRows() == 2);
                assert (argType.getCols() == 2);
                for (int j = 0; j < 4; ++j) {
                    argumentIds.add(writeOpCompositeExtract(componentType, argId,
                            j / 2, j % 2, output));
                }
            } else if (argType.isVector()) {
                // There's a bug in the Intel Vulkan driver where OpCompositeConstruct doesn't handle
                // vector arguments at all, so we always extract each vector component and pass them
                // into OpCompositeConstruct individually.
                for (int j = 0; j < argType.getRows(); j++) {
                    argumentIds.add(writeOpCompositeExtract(componentType, argId,
                            j, output));
                }
            } else {
                argumentIds.add(argId);
            }
        }

        int resultId = writeOpCompositeConstruct(type, argumentIds, output);
        releaseIdList(argumentIds);
        return resultId;
    }

    // append entry to the current column
    private void addColumnEntry(Type columnType,
                                IntArrayList currentColumn,
                                IntArrayList columnIds,
                                int rows,
                                int entry,
                                Output output) {
        assert (currentColumn.size() < rows);
        currentColumn.add(entry);
        if (currentColumn.size() == rows) {
            // Synthesize this column into a vector.
            int columnId = writeOpCompositeConstruct(columnType, currentColumn, output);
            columnIds.add(columnId);
            currentColumn.clear();
        }
    }

    private int writeMatrixConstructor(ConstructorCompound ctor, Output output) {
        Type type = ctor.getType();
        Type componentType = type.getComponentType();
        assert (type.isMatrix());

        Type arg0Type = ctor.getArguments()[0].getType();
        // go ahead and write the arguments so we don't try to write new instructions in the middle of
        // an instruction
        IntArrayList arguments = obtainIdList();
        for (Expression arg : ctor.getArguments()) {
            arguments.add(writeExpression(arg, output));
        }

        int rows = type.getRows();
        Type columnType = componentType.toVector(getContext(), rows);

        if (arguments.size() == 1 && arg0Type.isVector()) {
            // Special-case handling of float4 -> mat2x2.
            assert (type.getRows() == 2 && type.getCols() == 2);
            assert (arg0Type.getRows() == 4);
            int argId = arguments.getInt(0);
            arguments.clear();
            for (int i = 0; i < 2; i++) {
                arguments.add(writeOpCompositeExtract(componentType, argId, i, output));
            }
            int v0v1 = writeOpCompositeConstruct(columnType, arguments, output);
            arguments.clear();
            for (int i = 2; i < 4; i++) {
                arguments.add(writeOpCompositeExtract(componentType, argId, i, output));
            }
            int v2v3 = writeOpCompositeConstruct(columnType, arguments, output);
            arguments.clear();
            arguments.add(v0v1);
            arguments.add(v2v3);
            int resultId = writeOpCompositeConstruct(type, arguments, output);
            releaseIdList(arguments);
            return resultId;
        }

        // SpvIds of completed columns of the matrix.
        IntArrayList columnIds = obtainIdList();
        // SpvIds of scalars we have written to the current column so far.
        IntArrayList currentColumn = obtainIdList();
        for (int i = 0; i < arguments.size(); i++) {
            Type argType = ctor.getArguments()[i].getType();
            if (currentColumn.isEmpty() && argType.isVector() && argType.getRows() == rows) {
                // This vector is a complete matrix column by itself and can be used as-is.
                columnIds.add(arguments.getInt(i));
            } else if (argType.getRows() == 1) {
                // This argument is a lone scalar and can be added to the current column as-is.
                addColumnEntry(columnType, currentColumn, columnIds, rows, arguments.getInt(i), output);
            } else {
                // This argument needs to be decomposed into its constituent scalars.
                for (int j = 0; j < argType.getRows(); ++j) {
                    int swizzle = writeOpCompositeExtract(argType.getComponentType(),
                            arguments.getInt(i), j, output);
                    addColumnEntry(columnType, currentColumn, columnIds, rows, swizzle, output);
                }
            }
        }
        assert (columnIds.size() == type.getCols());
        int resultId = writeOpCompositeConstruct(type, columnIds, output);
        releaseIdList(columnIds);
        releaseIdList(currentColumn);
        return resultId;
    }

    private int writeConstructorCompound(ConstructorCompound ctor, Output output) {
        return ctor.getType().isMatrix()
                ? writeMatrixConstructor(ctor, output)
                : writeVectorConstructor(ctor, output);
    }

    private int writeCompositeConstructor(ConstructorCall ctor, Output output) {
        assert (ctor.getType().isArray() || ctor.getType().isStruct());

        IntArrayList argumentIds = obtainIdList();
        for (Expression arg : ctor.getArguments()) {
            argumentIds.add(writeExpression(arg, output));
        }
        int resultId = writeOpCompositeConstruct(ctor.getType(), argumentIds, output);
        releaseIdList(argumentIds);
        return resultId;
    }

    private void buildInstructions(@NonNull TranslationUnit translationUnit) {
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
        for (var it = mVariableTable.reference2IntEntrySet().fastIterator(); it.hasNext(); ) {
            var e = it.next();
            Variable variable = e.getKey();
            if (variable.getStorage() == Variable.kGlobal_Storage) {
                // Before version 1.4, the interface's storage classes are
                // limited to the Input and Output storage classes. Starting with
                // version 1.4, the interface's storage classes are all storage classes
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
                    Output output) {
        // Look for this pointer in our load-cache.
        //TODO find a way to eliminate store ops if we are using store cache
        /*int cachedOp = mStoreCache.get(pointer);
        if (cachedOp != 0) {
            return cachedOp;
        }*/

        // Write the requested OpLoad instruction.
        int resultId = getUniqueId(relaxedPrecision);
        writeInstruction(SpvOpLoad, type, resultId, pointer, output);
        return resultId;
    }

    void writeOpStore(int storageClass,
                      int pointer,
                      int rvalue,
                      Output output) {
        // Write the uncached SpvOpStore directly.
        writeInstruction(SpvOpStore, pointer, rvalue, output);

        if (storageClass == SpvStorageClassFunction) {
            // Insert a pointer-to-SpvId mapping into the load cache. A writeOpLoad to this pointer will
            // return the cached value as-is.
            mStoreCache.put(pointer, rvalue);
            mStoreOps.add(pointer);
        }
    }

    void writeOpcode(int opcode, int count, Output output) {
        if ((count & 0xFFFF0000) != 0) {
            getContext().error(Position.NO_POS, "too many words");
        }
        assert (opcode != SpvOpLoad || output != mConstantBuffer);
        assert (opcode != SpvOpUndef);
        boolean foundDeadCode = false;
        switch (opcode) {
            case SpvOpReturn:
            case SpvOpReturnValue:
            case SpvOpKill:
            case SpvOpSwitch:
            case SpvOpBranch:
            case SpvOpBranchConditional:
                // This instruction causes us to leave the current block.
                foundDeadCode = (mCurrentBlock == 0);
                mCurrentBlock = 0;
                break;
            default:
                break;
        }

        if (foundDeadCode) {
            // We just encountered dead code--an instruction that don't have an associated block.
            // Synthesize a label if this happens; this is necessary to satisfy the validator.
            writeLabel(getUniqueId(), output);
        }

        output.writeWord((count << 16) | opcode);
    }

    void writeInstruction(int opcode, Output output) {
        writeOpcode(opcode, 1, output);
    }

    void writeInstruction(int opcode, int word1, Output output) {
        writeOpcode(opcode, 2, output);
        output.writeWord(word1);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          Output output) {
        writeOpcode(opcode, 3, output);
        output.writeWord(word1);
        output.writeWord(word2);
    }

    private void writeInstruction(int opcode, int word1, String string,
                                  Output output) {
        writeOpcode(opcode, 2 + (string.length() + 4 >> 2), output);
        output.writeWord(word1);
        output.writeString8(getContext(), string);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          int word3, Output output) {
        writeOpcode(opcode, 4, output);
        output.writeWord(word1);
        output.writeWord(word2);
        output.writeWord(word3);
    }

    private void writeInstruction(int opcode, int word1, int word2,
                                  String string, Output output) {
        writeOpcode(opcode, 3 + (string.length() + 4 >> 2), output);
        output.writeWord(word1);
        output.writeWord(word2);
        output.writeString8(getContext(), string);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          int word3, int word4, Output output) {
        writeOpcode(opcode, 5, output);
        output.writeWord(word1);
        output.writeWord(word2);
        output.writeWord(word3);
        output.writeWord(word4);
    }

    void writeInstruction(int opcode, int word1, int word2,
                          int word3, int word4, int word5,
                          Output output) {
        writeOpcode(opcode, 6, output);
        output.writeWord(word1);
        output.writeWord(word2);
        output.writeWord(word3);
        output.writeWord(word4);
        output.writeWord(word5);
    }

    void writeInstruction(int opcode, int word1, int word2,
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

    void writeInstruction(int opcode, int word1, int word2,
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

    void writeInstruction(int opcode, int word1, int word2,
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

    /**
     * With bidirectional map.
     */
    private int writeInstructionWithCache(@NonNull InstructionBuilder key,
                                          @NonNull Output output) {
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
        writeOpcode(key.mOpcode, s + 1, output);
        for (int i = 0; i < s; i++) {
            if (Instruction.isResult(kinds[i])) {
                assert resultId != NONE_ID;
                output.writeWord(resultId);
            } else {
                output.writeWord(values[i]);
            }
        }

        releaseInstBuilder(key);
        // Return the result.
        return resultId;
    }

    private void releaseInstBuilder(@NonNull InstructionBuilder key) {
        if (mInstBuilderPoolSize == mInstBuilderPool.length) {
            return;
        }
        mInstBuilderPool[mInstBuilderPoolSize++] = key;
    }

    @NonNull
    private IntArrayList obtainIdList() {
        if (mIdListPoolSize == 0) {
            return new IntArrayList();
        }
        var r = mIdListPool[--mIdListPoolSize];
        r.clear();
        return r;
    }

    private void releaseIdList(@NonNull IntArrayList idList) {
        if (mIdListPoolSize == mIdListPool.length) {
            return;
        }
        mIdListPool[mIdListPoolSize++] = idList;
    }

    private static final int
            kInvalid_IntrinsicOpcodeKind = 0,
            kGLSLstd450_IntrinsicOpcodeKind = 1,
            kSPIRV_IntrinsicOpcodeKind = 2,
            kSpecial_IntrinsicOpcodeKind = 3;

    private static final int
            kAtan_SpecialIntrinsic = 0,
            kClamp_SpecialIntrinsic = 1,
            kMatrixCompMult_SpecialIntrinsic = 2,
            kMax_SpecialIntrinsic = 3,
            kMin_SpecialIntrinsic = 4,
            kMix_SpecialIntrinsic = 5,
            kMod_SpecialIntrinsic = 6,
            kSaturate_SpecialIntrinsic = 7,
            kSampledImage_SpecialIntrinsic = 8,
            kSmoothStep_SpecialIntrinsic = 9,
            kStep_SpecialIntrinsic = 10,
            kSubpassLoad_SpecialIntrinsic = 11,
            kTexture_SpecialIntrinsic = 12,
            kTextureGrad_SpecialIntrinsic = 13,
            kTextureLod_SpecialIntrinsic = 14,
            kTextureFetch_SpecialIntrinsic = 15,
            kTextureRead_SpecialIntrinsic = 16,
            kTextureWrite_SpecialIntrinsic = 17,
            kTextureWidth_SpecialIntrinsic = 18,
            kTextureHeight_SpecialIntrinsic = 19,
            kAtomicAdd_SpecialIntrinsic = 20,
            kAtomicLoad_SpecialIntrinsic = 21,
            kAtomicStore_SpecialIntrinsic = 22,
            kStorageBarrier_SpecialIntrinsic = 23,
            kWorkgroupBarrier_SpecialIntrinsic = 24;

    // flattened intrinsic data:
    //
    // struct Intrinsic {
    //     IntrinsicOpcodeKind opKind;
    //     int32_t floatOp;
    //     int32_t signedOp;
    //     int32_t unsignedOp;
    //     int32_t booleanOp;
    // };
    private static final int kIntrinsicDataColumn = 5;
    private static final int[] sIntrinsicData = new int[IntrinsicList.kCount * kIntrinsicDataColumn];

    private static void setIntrinsic(int intrinsic, int opKind,
                                     int floatOp, int signedOp, int unsignedOp, int booleanOp) {
        assert intrinsic >= 0 && intrinsic < IntrinsicList.kCount;
        int index = intrinsic * kIntrinsicDataColumn;
        assert sIntrinsicData[index] == kInvalid_IntrinsicOpcodeKind;
        sIntrinsicData[index] = opKind;
        sIntrinsicData[index + 1] = floatOp;
        sIntrinsicData[index + 2] = signedOp;
        sIntrinsicData[index + 3] = unsignedOp;
        sIntrinsicData[index + 4] = booleanOp;
    }

    // setup intrinsics
    static {
        setIntrinsic(IntrinsicList.kRound,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Round, GLSLstd450Round, GLSLstd450Round, GLSLstd450Round);
        setIntrinsic(IntrinsicList.kRoundEven,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450RoundEven, GLSLstd450RoundEven, GLSLstd450RoundEven, GLSLstd450RoundEven);
        setIntrinsic(IntrinsicList.kTrunc,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Trunc, GLSLstd450Trunc, GLSLstd450Trunc, GLSLstd450Trunc);
        setIntrinsic(IntrinsicList.kAbs,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450FAbs, GLSLstd450SAbs, GLSLstd450SAbs, GLSLstd450Bad);
        setIntrinsic(IntrinsicList.kSign,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450FSign, GLSLstd450SSign, GLSLstd450SSign, GLSLstd450Bad);
        setIntrinsic(IntrinsicList.kFloor,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Floor, GLSLstd450Floor, GLSLstd450Floor, GLSLstd450Floor);
        setIntrinsic(IntrinsicList.kCeil,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Ceil, GLSLstd450Ceil, GLSLstd450Ceil, GLSLstd450Ceil);
        setIntrinsic(IntrinsicList.kFract,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Fract, GLSLstd450Fract, GLSLstd450Fract, GLSLstd450Fract);
        setIntrinsic(IntrinsicList.kRadians,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Radians, GLSLstd450Radians, GLSLstd450Radians, GLSLstd450Radians);
        setIntrinsic(IntrinsicList.kDegrees,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Degrees, GLSLstd450Degrees, GLSLstd450Degrees, GLSLstd450Degrees);
        setIntrinsic(IntrinsicList.kSin,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Sin, GLSLstd450Sin, GLSLstd450Sin, GLSLstd450Sin);
        setIntrinsic(IntrinsicList.kCos,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Cos, GLSLstd450Cos, GLSLstd450Cos, GLSLstd450Cos);
        setIntrinsic(IntrinsicList.kTan,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Tan, GLSLstd450Tan, GLSLstd450Tan, GLSLstd450Tan);
        setIntrinsic(IntrinsicList.kAsin,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Asin, GLSLstd450Asin, GLSLstd450Asin, GLSLstd450Asin);
        setIntrinsic(IntrinsicList.kAcos,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Acos, GLSLstd450Acos, GLSLstd450Acos, GLSLstd450Acos);
        setIntrinsic(IntrinsicList.kAtan,
                kSpecial_IntrinsicOpcodeKind,
                kAtan_SpecialIntrinsic, kAtan_SpecialIntrinsic,
                kAtan_SpecialIntrinsic, kAtan_SpecialIntrinsic);
        setIntrinsic(IntrinsicList.kSinh,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Sinh, GLSLstd450Sinh, GLSLstd450Sinh, GLSLstd450Sinh);
        setIntrinsic(IntrinsicList.kCosh,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Cosh, GLSLstd450Cosh, GLSLstd450Cosh, GLSLstd450Cosh);
        setIntrinsic(IntrinsicList.kTanh,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Tanh, GLSLstd450Tanh, GLSLstd450Tanh, GLSLstd450Tanh);
        setIntrinsic(IntrinsicList.kAsinh,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Asinh, GLSLstd450Asinh, GLSLstd450Asinh, GLSLstd450Asinh);
        setIntrinsic(IntrinsicList.kAcosh,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Acosh, GLSLstd450Acosh, GLSLstd450Acosh, GLSLstd450Acosh);
        setIntrinsic(IntrinsicList.kAtanh,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Atanh, GLSLstd450Atanh, GLSLstd450Atanh, GLSLstd450Atanh);
        setIntrinsic(IntrinsicList.kPow,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Pow, GLSLstd450Pow, GLSLstd450Pow, GLSLstd450Pow);
        setIntrinsic(IntrinsicList.kExp,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Exp, GLSLstd450Exp, GLSLstd450Exp, GLSLstd450Exp);
        setIntrinsic(IntrinsicList.kLog,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Log, GLSLstd450Log, GLSLstd450Log, GLSLstd450Log);
        setIntrinsic(IntrinsicList.kExp2,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Exp2, GLSLstd450Exp2, GLSLstd450Exp2, GLSLstd450Exp2);
        setIntrinsic(IntrinsicList.kLog2,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Log2, GLSLstd450Log2, GLSLstd450Log2, GLSLstd450Log2);
        setIntrinsic(IntrinsicList.kSqrt,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Sqrt, GLSLstd450Sqrt, GLSLstd450Sqrt, GLSLstd450Sqrt);
        setIntrinsic(IntrinsicList.kMatrixInverse,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450MatrixInverse, GLSLstd450MatrixInverse,
                GLSLstd450MatrixInverse, GLSLstd450MatrixInverse);
        setIntrinsic(IntrinsicList.kOuterProduct,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpOuterProduct, SpvOpOuterProduct, SpvOpOuterProduct, SpvOpOuterProduct);
        setIntrinsic(IntrinsicList.kTranspose,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpTranspose, SpvOpTranspose, SpvOpTranspose, SpvOpTranspose);
        setIntrinsic(IntrinsicList.kIsInf,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpIsInf, SpvOpIsInf, SpvOpIsInf, SpvOpIsInf);
        setIntrinsic(IntrinsicList.kIsNan,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpIsNan, SpvOpIsNan, SpvOpIsNan, SpvOpIsNan);
        setIntrinsic(IntrinsicList.kInverseSqrt,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450InverseSqrt, GLSLstd450InverseSqrt,
                GLSLstd450InverseSqrt, GLSLstd450InverseSqrt);
        setIntrinsic(IntrinsicList.kDeterminant,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Determinant, GLSLstd450Determinant,
                GLSLstd450Determinant, GLSLstd450Determinant);

        setIntrinsic(IntrinsicList.kMod,
                kSpecial_IntrinsicOpcodeKind,
                kMod_SpecialIntrinsic, kMod_SpecialIntrinsic,
                kMod_SpecialIntrinsic, kMod_SpecialIntrinsic);
        setIntrinsic(IntrinsicList.kModf,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Modf, GLSLstd450Modf, GLSLstd450Modf, GLSLstd450Modf);
        setIntrinsic(IntrinsicList.kMin,
                kSpecial_IntrinsicOpcodeKind,
                kMin_SpecialIntrinsic, kMin_SpecialIntrinsic,
                kMin_SpecialIntrinsic, kMin_SpecialIntrinsic);
        setIntrinsic(IntrinsicList.kMax,
                kSpecial_IntrinsicOpcodeKind,
                kMax_SpecialIntrinsic, kMax_SpecialIntrinsic,
                kMax_SpecialIntrinsic, kMax_SpecialIntrinsic);
        setIntrinsic(IntrinsicList.kClamp,
                kSpecial_IntrinsicOpcodeKind,
                kClamp_SpecialIntrinsic, kClamp_SpecialIntrinsic,
                kClamp_SpecialIntrinsic, kClamp_SpecialIntrinsic);
        setIntrinsic(IntrinsicList.kSaturate,
                kSpecial_IntrinsicOpcodeKind,
                kSaturate_SpecialIntrinsic, kSaturate_SpecialIntrinsic,
                kSaturate_SpecialIntrinsic, kSaturate_SpecialIntrinsic);
        setIntrinsic(IntrinsicList.kDot,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpDot, SpvOpUndef, SpvOpUndef, SpvOpUndef);
        setIntrinsic(IntrinsicList.kMix,
                kSpecial_IntrinsicOpcodeKind,
                kMix_SpecialIntrinsic, kMix_SpecialIntrinsic,
                kMix_SpecialIntrinsic, kMix_SpecialIntrinsic);
        setIntrinsic(IntrinsicList.kStep,
                kSpecial_IntrinsicOpcodeKind,
                kStep_SpecialIntrinsic, kStep_SpecialIntrinsic,
                kStep_SpecialIntrinsic, kStep_SpecialIntrinsic);
        setIntrinsic(IntrinsicList.kSmoothStep,
                kSpecial_IntrinsicOpcodeKind,
                kSmoothStep_SpecialIntrinsic, kSmoothStep_SpecialIntrinsic,
                kSmoothStep_SpecialIntrinsic, kSmoothStep_SpecialIntrinsic);

        setIntrinsic(IntrinsicList.kLength,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Length, GLSLstd450Length, GLSLstd450Length, GLSLstd450Length);
        setIntrinsic(IntrinsicList.kDistance,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Distance, GLSLstd450Distance, GLSLstd450Distance, GLSLstd450Distance);
        setIntrinsic(IntrinsicList.kCross,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Cross, GLSLstd450Cross, GLSLstd450Cross, GLSLstd450Cross);
        setIntrinsic(IntrinsicList.kNormalize,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Normalize, GLSLstd450Normalize, GLSLstd450Normalize, GLSLstd450Normalize);
        setIntrinsic(IntrinsicList.kFaceForward,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450FaceForward, GLSLstd450FaceForward, GLSLstd450FaceForward, GLSLstd450FaceForward);
        setIntrinsic(IntrinsicList.kReflect,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Reflect, GLSLstd450Reflect, GLSLstd450Reflect, GLSLstd450Reflect);
        setIntrinsic(IntrinsicList.kRefract,
                kGLSLstd450_IntrinsicOpcodeKind,
                GLSLstd450Refract, GLSLstd450Refract, GLSLstd450Refract, GLSLstd450Refract);

        setIntrinsic(IntrinsicList.kDPdx,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpDPdx, SpvOpUndef, SpvOpUndef, SpvOpUndef);
        setIntrinsic(IntrinsicList.kDPdy,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpDPdy, SpvOpUndef, SpvOpUndef, SpvOpUndef);
        setIntrinsic(IntrinsicList.kFwidth,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpFwidth, SpvOpUndef, SpvOpUndef, SpvOpUndef);

        setIntrinsic(IntrinsicList.kTexture,
                kSpecial_IntrinsicOpcodeKind,
                kTexture_SpecialIntrinsic, kTexture_SpecialIntrinsic,
                kTexture_SpecialIntrinsic, kTexture_SpecialIntrinsic);
        setIntrinsic(IntrinsicList.kTextureFetch,
                kSpecial_IntrinsicOpcodeKind,
                kTextureFetch_SpecialIntrinsic, kTextureFetch_SpecialIntrinsic,
                kTextureFetch_SpecialIntrinsic, kTextureFetch_SpecialIntrinsic);

        setIntrinsic(IntrinsicList.kAny,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpUndef, SpvOpUndef, SpvOpUndef, SpvOpAny);
        setIntrinsic(IntrinsicList.kAll,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpUndef, SpvOpUndef, SpvOpUndef, SpvOpAll);
        setIntrinsic(IntrinsicList.kLogicalNot,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpUndef, SpvOpUndef, SpvOpUndef, SpvOpLogicalNot);
        setIntrinsic(IntrinsicList.kEqual,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpFOrdEqual,
                SpvOpIEqual,
                SpvOpIEqual,
                SpvOpLogicalEqual);
        setIntrinsic(IntrinsicList.kNotEqual,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpFUnordNotEqual,
                SpvOpINotEqual,
                SpvOpINotEqual,
                SpvOpLogicalNotEqual);
        setIntrinsic(IntrinsicList.kLessThan,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpFOrdLessThan,
                SpvOpSLessThan,
                SpvOpULessThan,
                SpvOpUndef);
        setIntrinsic(IntrinsicList.kLessThanEqual,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpFOrdLessThanEqual,
                SpvOpSLessThanEqual,
                SpvOpULessThanEqual,
                SpvOpUndef);
        setIntrinsic(IntrinsicList.kGreaterThan,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpFOrdGreaterThan,
                SpvOpSGreaterThan,
                SpvOpUGreaterThan,
                SpvOpUndef);
        setIntrinsic(IntrinsicList.kGreaterThanEqual,
                kSPIRV_IntrinsicOpcodeKind,
                SpvOpFOrdGreaterThanEqual,
                SpvOpSGreaterThanEqual,
                SpvOpUGreaterThanEqual,
                SpvOpUndef);
    }
}
