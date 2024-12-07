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
import icyllis.arc3d.compiler.IntrinsicList;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.StringJoiner;

/**
 * Represents a function declaration (function symbol). If the function is overloaded,
 * it will serve as a singly linked list.
 */
public final class FunctionDecl extends Symbol {

    private final Modifiers mModifiers;
    private final List<Variable> mParameters;
    private final Type mReturnType;

    private boolean mBuiltin;
    private boolean mEntryPoint;
    private int mIntrinsicKind;

    private FunctionDecl mNextOverload;
    private FunctionDefinition mDefinition;

    public FunctionDecl(int position, Modifiers modifiers, String name,
                        List<Variable> parameters, Type returnType,
                        boolean builtin, boolean entryPoint, int intrinsicKind) {
        super(position, name);
        mModifiers = modifiers;
        mParameters = parameters;
        mReturnType = returnType;
        mBuiltin = builtin;
        mEntryPoint = entryPoint;
        mIntrinsicKind = intrinsicKind;
    }

    private static boolean checkModifiers(@NonNull Context context,
                                          @NonNull Modifiers modifiers) {
        // No layout flag is permissible on a function.
        boolean success = modifiers.checkLayoutFlags(context, 0);
        int permittedFlags = Modifiers.kInline_Flag |
                Modifiers.kNoInline_Flag |
                (context.isModule() ? Modifiers.kPure_Flag
                        : 0);
        success &= modifiers.checkFlags(context, permittedFlags);
        if ((modifiers.flags() & (Modifiers.kInline_Flag | Modifiers.kNoInline_Flag)) ==
                (Modifiers.kInline_Flag | Modifiers.kNoInline_Flag)) {
            context.error(modifiers.mPosition, "functions cannot be both 'inline' and 'noinline'");
            return false;
        }
        return success;
    }

    private static boolean checkReturnType(@NonNull Context context,
                                           int pos, @NonNull Type returnType) {
        if (returnType.isOpaque()) {
            context.error(pos, "functions may not return opaque type '" +
                    returnType.getName() + "'");
            return false;
        }
        if (returnType.isUnsizedArray()) {
            context.error(pos, "functions may not return unsized array type '" +
                    returnType.getName() + "'");
            return false;
        }
        return true;
    }

    private static boolean checkParameters(@NonNull Context context,
                                           @NonNull List<Variable> parameters,
                                           @NonNull Modifiers modifiers) {
        boolean success = true;
        for (var param : parameters) {
            Type type = param.getType();
            int permittedFlags = Modifiers.kConst_Flag | Modifiers.kIn_Flag;
            int permittedLayoutFlags = 0;
            if (!type.isOpaque()) {
                permittedFlags |= Modifiers.kOut_Flag;
            } else if (type.isStorageImage()) { // opaque
                permittedFlags |= Modifiers.kMemory_Flags;
            }
            success &= param.getModifiers().checkFlags(context, permittedFlags);
            success &= param.getModifiers().checkLayoutFlags(context, permittedLayoutFlags);

            // Pure functions should not change any state, and should be safe to eliminate if their
            // result is not used; this is incompatible with out-parameters, so we forbid it here.
            // (We don't exhaustively guard against pure functions changing global state in other ways,
            // though, since they aren't allowed in user code.)
            if ((modifiers.flags() & Modifiers.kPure_Flag) != 0 &&
                    (param.getModifiers().flags() & Modifiers.kOut_Flag) != 0) {
                context.error(param.getModifiers().mPosition,
                        "pure functions cannot have out parameters");
                success = false;
            }
        }
        return success;
    }

    private static boolean checkEntryPointSignature(@NonNull Context context,
                                                    int pos,
                                                    @NonNull Type returnType,
                                                    @NonNull List<Variable> parameters) {
        switch (context.getKind()) {
            case VERTEX, FRAGMENT, COMPUTE -> {
                if (!returnType.matches(context.getTypes().mVoid)) {
                    context.error(pos, "entry point must return 'void'");
                    return false;
                }
                if (!parameters.isEmpty()) {
                    context.error(pos, "entry point must have zero parameters");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Given a concrete type (`float3`) and a generic type (`__genFType`), returns the index of the
     * concrete type within the generic type's typelist. Returns -1 if there is no match.
     */
    private static int findGenericIndex(@NonNull Type concreteType,
                                        @NonNull Type genericType,
                                        boolean allowNarrowing) {
        Type[] types = genericType.getCoercibleTypes();
        for (int index = 0; index < types.length; index++) {
            if (concreteType.canCoerceTo(types[index], allowNarrowing)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Returns true if the types match, or if `concreteType` can be found in `maybeGenericType`.
     */
    private static boolean typeMatches(@NonNull Type concreteType,
                                       @NonNull Type maybeGenericType) {
        return maybeGenericType.isGeneric()
                ? findGenericIndex(concreteType, maybeGenericType, false) != -1
                : concreteType.matches(maybeGenericType);
    }

    /**
     * Checks a parameter list (params) against the parameters of a function that was declared earlier
     * (otherParams). Returns true if they match, even if the parameters in `otherParams` contain
     * generic types.
     */
    private static boolean parametersMatch(@NonNull List<Variable> params,
                                           @NonNull List<Variable> otherParams) {
        // If the param lists are different lengths, they're definitely not a match.
        if (params.size() != otherParams.size()) {
            return false;
        }

        // Figure out a consistent generic index (or bail if we find a contradiction).
        int genericIndex = -1;
        for (int i = 0; i < params.size(); ++i) {
            Type paramType = params.get(i).getType();
            Type otherParamType = otherParams.get(i).getType();

            if (otherParamType.isGeneric()) {
                int genericIndexForThisParam = findGenericIndex(paramType, otherParamType,
                        /*allowNarrowing=*/false);
                if (genericIndexForThisParam == -1) {
                    // The type wasn't a match for this generic at all; these params can't be a match.
                    return false;
                }
                if (genericIndex != -1 && genericIndex != genericIndexForThisParam) {
                    // The generic index mismatches from what we determined on a previous parameter.
                    return false;
                }
                genericIndex = genericIndexForThisParam;
            }
        }

        // Now that we've determined a generic index (if we needed one), do a parameter check.
        for (int i = 0; i < params.size(); i++) {
            Type paramType = params.get(i).getType();
            Type otherParamType = otherParams.get(i).getType();

            // Make generic types concrete.
            if (otherParamType.isGeneric()) {
                otherParamType = otherParamType.getCoercibleTypes()[genericIndex];
            }
            // Detect type mismatches.
            if (!paramType.matches(otherParamType)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static FunctionDecl convert(@NonNull Context context,
                                       int pos,
                                       @NonNull Modifiers modifiers,
                                       @NonNull String name,
                                       @NonNull List<Variable> parameters,
                                       @NonNull Type returnType) {
        int intrinsicKind = context.isBuiltin()
                ? IntrinsicList.findIntrinsicKind(name)
                : IntrinsicList.kNotIntrinsic;
        boolean isEntryPoint = name.equals(context.getOptions().mEntryPointName);

        if (!checkModifiers(context, modifiers)) {
            return null;
        }
        if (!checkReturnType(context, pos, returnType)) {
            return null;
        }
        if (!checkParameters(context, parameters, modifiers)) {
            return null;
        }
        if (isEntryPoint && !checkEntryPointSignature(context, pos, returnType, parameters)) {
            return null;
        }

        Symbol entry = context.getSymbolTable().find(name);
        if (entry != null) {
            if (!(entry instanceof FunctionDecl chain)) {
                context.error(pos, "symbol '" + name + "' was already defined");
                return null;
            }
            FunctionDecl existingDecl = null;
            for (FunctionDecl other = chain;
                 other != null;
                 other = other.getNextOverload()) {
                if (!parametersMatch(parameters, other.getParameters())) {
                    continue;
                }
                if (!typeMatches(returnType, other.getReturnType())) {
                    var invalidDecl = new FunctionDecl(pos, modifiers, name, parameters, returnType,
                            context.isBuiltin(), isEntryPoint, intrinsicKind);
                    context.error(pos, "functions '" + invalidDecl + "' and '" +
                            other + "' differ only in return type");
                    return null;
                }
                for (int i = 0; i < parameters.size(); i++) {
                    if (!parameters.get(i).getModifiers().equals(
                            other.getParameters().get(i).getModifiers())) {
                        context.error(parameters.get(i).mPosition,
                                "modifiers on parameter " + (i + 1) +
                                        " differ between declaration and definition");
                        return null;
                    }
                }
                if (other.getDefinition() != null || other.isIntrinsic() ||
                        !modifiers.equals(other.getModifiers())) {
                    var invalidDecl = new FunctionDecl(pos, modifiers, name, parameters, returnType,
                            context.isBuiltin(), isEntryPoint, intrinsicKind);
                    context.error(pos, "redefinition of '" + invalidDecl + "'");
                    return null;
                }
                existingDecl = other;
                break;
            }
            if (existingDecl == null && chain.isEntryPoint()) {
                context.error(pos, "redefinition of entry point");
                return null;
            }
            if (existingDecl != null) {
                return existingDecl;
            }
        }

        return context.getSymbolTable().insert(
                context,
                new FunctionDecl(
                        pos,
                        modifiers,
                        name,
                        parameters,
                        returnType,
                        context.isBuiltin(),
                        isEntryPoint,
                        intrinsicKind
                )
        );
    }

    @NonNull
    @Override
    public SymbolKind getKind() {
        return SymbolKind.FUNCTION_DECL;
    }

    @NonNull
    @Override
    public Type getType() {
        throw new AssertionError();
    }

    @NonNull
    public Type getReturnType() {
        return mReturnType;
    }

    public int getIntrinsicKind() {
        return mIntrinsicKind;
    }

    public boolean isIntrinsic() {
        return mIntrinsicKind != IntrinsicList.kNotIntrinsic;
    }

    public FunctionDefinition getDefinition() {
        return mDefinition;
    }

    public void setDefinition(FunctionDefinition definition) {
        mDefinition = definition;
    }

    public List<Variable> getParameters() {
        return mParameters;
    }

    @NonNull
    public String getMangledName() {
        if (isIntrinsic() || isEntryPoint()) {
            return getName();
        }
        StringBuilder mangledName = new StringBuilder(getName());
        for (Variable p : mParameters) {
            mangledName.append('_').append(p.getType().getDesc());
        }
        return mangledName.toString();
    }

    @Nullable
    public FunctionDecl getNextOverload() {
        return mNextOverload;
    }

    /**
     * Sets the previously defined function symbol with the same function name.
     */
    public void setNextOverload(FunctionDecl overload) {
        assert (overload == null || overload.getName().equals(getName()));
        mNextOverload = overload;
    }

    public Modifiers getModifiers() {
        return mModifiers;
    }

    public boolean isBuiltin() {
        return mBuiltin;
    }

    public boolean isEntryPoint() {
        return mEntryPoint;
    }

    // returns returnType
    @Nullable
    public Type resolveParameterTypes(@NonNull List<Expression> arguments,
                                      List<Type> outParameterTypes) {
        List<Variable> parameters = mParameters;
        assert parameters.size() == arguments.size();

        int genericIndex = -1;
        for (int i = 0; i < arguments.size(); i++) {
            // Non-generic parameters are final as-is.
            Type parameterType = parameters.get(i).getType();
            if (!parameterType.isGeneric()) {
                outParameterTypes.add(parameterType);
                continue;
            }
            // We use the first generic parameter we find to lock in the generic index;
            // e.g. if we find `float3` here, all `__genFType`s will be assumed to be `float3`.
            if (genericIndex == -1) {
                genericIndex = findGenericIndex(arguments.get(i).getType(), parameterType,
                        /*allowNarrowing=*/true);
                if (genericIndex == -1) {
                    // The passed-in type wasn't a match for ANY of the generic possibilities.
                    // This function isn't a match at all.
                    return null;
                }
            }
            outParameterTypes.add(parameterType.getCoercibleTypes()[genericIndex]);
        }
        // Apply the generic index to our return type.
        Type returnType = mReturnType;
        if (returnType.isGeneric()) {
            if (genericIndex == -1) {
                // We don't support functions with a generic return type and no other generics.
                return null;
            }
            return returnType.getCoercibleTypes()[genericIndex];
        } else {
            return returnType;
        }
    }

    @NonNull
    @Override
    public String toString() {
        String header = mModifiers.toString() +
                mReturnType.getName() + " " + getName() + "(";
        StringJoiner joiner = new StringJoiner(", ");
        for (Variable p : mParameters) {
            joiner.add(p.toString());
        }
        return header + joiner + ")";
    }
}
