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
import icyllis.arc3d.compiler.Operator;
import icyllis.arc3d.compiler.analysis.Analysis;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * A function invocation: function_name( args, ... ).
 *
 * @see ConstructorCall
 */
public final class FunctionCall extends Expression {

    private final FunctionDeclaration mFunction;
    private final Expression[] mArguments;

    private FunctionCall(int position, Type type, FunctionDeclaration function,
                         Expression... arguments) {
        super(position, type);
        mFunction = function;
        mArguments = arguments;
    }

    private static String buildArgumentTypeList(List<Expression> arguments) {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (Expression arg : arguments) {
            joiner.add(arg.getType().toString());
        }
        return joiner.toString();
    }

    @Nullable
    private static FunctionDeclaration findBestCandidate(@NonNull FunctionDeclaration chain,
                                                         @NonNull List<Expression> arguments) {
        if (chain.getNextOverload() == null) {
            return chain;
        }
        long bestCost = Type.CoercionCost.saturate();
        FunctionDeclaration best = null;
        for (FunctionDeclaration f = chain; f != null; f = f.getNextOverload()) {
            final long cost;
            if (f.getParameters().size() != arguments.size()) {
                cost = Type.CoercionCost.saturate();
            } else {
                List<Type> paramTypes = new ArrayList<>();
                if (f.resolveParameterTypes(arguments, paramTypes) == null) {
                    cost = Type.CoercionCost.saturate();
                } else {
                    long total = Type.CoercionCost.free();
                    for (int i = 0; i < arguments.size(); i++) {
                        total = Type.CoercionCost.plus(
                                total,
                                arguments.get(i).getCoercionCost(paramTypes.get(i))
                        );
                    }
                    cost = total;
                }
            }
            if (Type.CoercionCost.compare(cost, bestCost) <= 0) {
                bestCost = cost;
                best = f;
            }
        }
        return Type.CoercionCost.accept(bestCost, false)
                ? best
                : null;
    }

    @Nullable
    public static Expression convert(@NonNull Context context,
                                     int pos, @NonNull Expression identifier,
                                     @NonNull List<Expression> arguments) {
        return switch (identifier.getKind()) {
            case TYPE_REFERENCE -> {
                TypeReference ref = (TypeReference) identifier;
                yield ConstructorCall.convert(context, pos, ref.getValue(), arguments);
            }
            case FUNCTION_REFERENCE -> {
                FunctionReference ref = (FunctionReference) identifier;
                FunctionDeclaration best = findBestCandidate(ref.getOverloadChain(), arguments);
                if (best != null) {
                    yield FunctionCall.convert(context, pos, best, arguments);
                }
                String msg = "no candidate found for function call " +
                        ref.getOverloadChain().getName() + buildArgumentTypeList(arguments);
                context.error(pos, msg);
                yield null;
            }
            case POISON -> {
                identifier.mPosition = pos;
                yield identifier;
            }
            default -> {
                context.error(pos, "not an invocation");
                yield null;
            }
        };
    }

    @Nullable
    public static Expression convert(@NonNull Context context,
                                     int pos, @NonNull FunctionDeclaration function,
                                     @NonNull List<Expression> arguments) {
        if (function.getParameters().size() != arguments.size()) {
            String msg = "call to '" + function.getName() + "' expected " +
                    function.getParameters().size() + " argument";
            if (function.getParameters().size() != 1) {
                msg += "s";
            }
            msg += ", but found " + arguments.size();
            context.error(pos, msg);
            return null;
        }

        List<Type> paramTypes = new ArrayList<>();
        Type returnType = function.resolveParameterTypes(arguments, paramTypes);
        if (returnType == null) {
            String msg = "no match for " + function.getName() +
                    buildArgumentTypeList(arguments);
            context.error(pos, msg);
            return null;
        }

        for (int i = 0; i < arguments.size(); i++) {
            // Coerce each argument to the proper type.
            arguments.set(i, paramTypes.get(i).coerceExpression(context, arguments.get(i)));
            if (arguments.get(i) == null) {
                return null;
            }
            // Update the refKind on out-parameters, and ensure that they are actually assignable.
            Modifiers paramFlags = function.getParameters().get(i).getModifiers();
            if ((paramFlags.flags() & Modifiers.kOut_Flag) != 0) {
                int refKind = (paramFlags.flags() & Modifiers.kIn_Flag) != 0
                        ? VariableReference.kReadWrite_ReferenceKind
                        : VariableReference.kPointer_ReferenceKind;
                if (!Analysis.updateVariableRefKind(arguments.get(i), refKind)) {
                    return null;
                }
            }
        }

        if (function.isEntryPoint()) {
            context.error(pos, "call to entry point is not allowed");
            return null;
        }

        return make(pos, returnType, function, arguments);
    }

    public static Expression make(int pos, Type returnType,
                                  FunctionDeclaration function,
                                  List<Expression> arguments) {
        assert function.getParameters().size() == arguments.size();

        //TODO optimization

        return new FunctionCall(pos, returnType, function, arguments.toArray(new Expression[0]));
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.FUNCTION_CALL;
    }

    public FunctionDeclaration getFunction() {
        return mFunction;
    }

    @Unmodifiable
    public Expression[] getArguments() {
        return mArguments;
    }

    @NonNull
    @Override
    public Expression copy(int position) {
        Expression[] arguments = mArguments.clone();
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].copy();
        }
        return new FunctionCall(position, getType(), mFunction, arguments);
    }

    @NonNull
    @Override
    public String toString(int parentPrecedence) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Expression arg : mArguments) {
            joiner.add(arg.toString(Operator.PRECEDENCE_SEQUENCE));
        }
        return mFunction.getName() + "(" + joiner + ")";
    }
}
