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

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.IntrinsicList;
import icyllis.arc3d.compiler.ThreadContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    public static FunctionDecl convert(int pos,
                                       Modifiers modifiers,
                                       String name,
                                       List<Variable> parameters,
                                       Type returnType) {
        ThreadContext context = ThreadContext.getInstance();

        int intrinsicKind = context.isBuiltin()
                ? IntrinsicList.findIntrinsicKind(name)
                : IntrinsicList.kNotIntrinsic;
        boolean isEntryPoint = "main".equals(name);

        return context.getSymbolTable().insert(
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

    @Nonnull
    @Override
    public SymbolKind getKind() {
        return SymbolKind.FUNCTION_DECL;
    }

    @Nonnull
    @Override
    public Type getType() {
        return mReturnType;
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

    @Nonnull
    public String getMangledName() {
        StringBuilder mangledName = new StringBuilder(getName());
        mangledName.append('(');
        for (Variable p : mParameters) {
            mangledName.append(p.getType().getDesc()).append(';');
        }
        return  mangledName.toString();
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

    private static int find_generic_index(Type concreteType,
                                              Type genericType,
                                              boolean allowNarrowing) {
        Type[] genericTypes = genericType.getCoercibleTypes();
        for (int index = 0; index < genericTypes.length; index++) {
            if (concreteType.canCoerceTo(genericTypes[index], allowNarrowing)) {
                return index;
            }
        }
        return -1;
    }

    public boolean isBuiltin() {
        return mBuiltin;
    }

    public boolean isEntryPoint() {
        return mEntryPoint;
    }

    // returns returnType
    @Nullable
    public Type resolveParameterTypes(@Nonnull List<Expression> arguments,
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
            // e.g. if we find `float3` here, all `$genType`s will be assumed to be `float3`.
            if (genericIndex == -1) {
                genericIndex = find_generic_index(arguments.get(i).getType(), parameterType,
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

    @Nonnull
    @Override
    public String toString() {
        String result =
                (mModifiers.flags() != 0 ? Modifiers.describeFlags(mModifiers.flags()) + " " : "") +
                        mReturnType.getName() + " " + getName() + "(";
        StringJoiner joiner = new StringJoiner(", ");
        for (Variable p : mParameters) {
            String s = "";
            if (p.getModifiers().flags() != 0) {
                s += Modifiers.describeFlags(p.getModifiers().flags()) + " ";
            }
            s += p.getType().getName();
            s += " ";
            s += p.getName();
            joiner.add(s);
        }
        return result + joiner + ")";
    }
}
