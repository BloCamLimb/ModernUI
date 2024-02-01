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

import icyllis.arc3d.compiler.Operator;
import icyllis.arc3d.compiler.ThreadContext;
import icyllis.arc3d.compiler.analysis.Analysis;
import icyllis.arc3d.compiler.analysis.NodeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * A function invocation: function_name( args, ... ).
 *
 * @see ConstructorCall
 */
public final class FunctionCall extends Expression {

    private final FunctionDecl mFunction;
    private final Expression[] mArguments;

    private FunctionCall(int position, Type type, FunctionDecl function,
                         Expression... arguments) {
        super(position, type);
        mFunction = function;
        mArguments = arguments;
    }

    private static String buildArgumentTypeList(List<Expression> arguments) {
        StringJoiner joiner = new StringJoiner(" ");
        for (Expression arg : arguments) {
            joiner.add(arg.getType().toString());
        }
        return joiner.toString();
    }

    private static FunctionDecl findFunctionOverload(FunctionDecl chain,
                                                     List<Expression> arguments) {
        if (chain.getNextOverload() == null) {
            return chain;
        }
        long bestCost = Type.CoercionCost.saturate();
        FunctionDecl best = null;
        for (FunctionDecl f = chain; f != null; f = f.getNextOverload()) {
            long cost;
            if (f.getParameters().size() != arguments.size()) {
                cost = Type.CoercionCost.saturate();
            } else {
                List<Type> paramTypes = new ArrayList<>();
                if (f.resolveParameterTypes(arguments, paramTypes) == null) {
                    cost = Type.CoercionCost.saturate();
                } else {
                    long total = Type.CoercionCost.free();
                    for (int i = 0; i < arguments.size(); i++) {
                        total = Type.CoercionCost.concat(
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
    public static Expression convert(int pos, @Nonnull Expression functionName,
                                     @Nonnull List<Expression> arguments) {
        return switch (functionName.getKind()) {
            case TYPE_REFERENCE -> {
                TypeReference ref = (TypeReference) functionName;
                yield ConstructorCall.convert(pos, ref.getValue(), arguments);
            }
            case FUNCTION_REFERENCE -> {
                FunctionReference ref = (FunctionReference) functionName;
                FunctionDecl best = findFunctionOverload(ref.getOverloadChain(), arguments);
                if (best == null) {
                    String msg = "no match for " + ref.getOverloadChain().getName() +
                            buildArgumentTypeList(arguments);
                    ThreadContext.getInstance().error(pos, msg);
                    yield null;
                }
                yield convert(pos, best, arguments);
            }
            case POISON -> {
                functionName.mPosition = pos;
                yield functionName;
            }
            default -> {
                ThreadContext.getInstance().error(pos, "not a function call");
                yield null;
            }
        };
    }

    @Nullable
    public static Expression convert(int pos, @Nonnull FunctionDecl function,
                                     @Nonnull List<Expression> arguments) {
        if (function.getParameters().size() != arguments.size()) {
            String msg = "call to '" + function.getName() + "' expected " +
                    function.getParameters().size() + " argument";
            if (function.getParameters().size() != 1) {
                msg += "s";
            }
            msg += ", but found " + arguments.size();
            ThreadContext.getInstance().error(pos, msg);
            return null;
        }

        List<Type> paramTypes = new ArrayList<>();
        Type returnType = function.resolveParameterTypes(arguments, paramTypes);
        if (returnType == null) {
            String msg = "no match for " + function.getName() +
                    buildArgumentTypeList(arguments);
            ThreadContext.getInstance().error(pos, msg);
            return null;
        }

        for (int i = 0; i < arguments.size(); i++) {
            // Coerce each argument to the proper type.
            arguments.set(i, paramTypes.get(i).coerceExpression(arguments.get(i)));
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
            ThreadContext.getInstance().error(pos, "call to entry point is not allowed");
            return null;
        }

        return make(pos, returnType, function, arguments);
    }

    public static Expression make(int pos, Type returnType,
                                  FunctionDecl function,
                                  List<Expression> arguments) {
        assert function.getParameters().size() == arguments.size();

        //TODO optimization

        return new FunctionCall(pos, returnType, function, arguments.toArray(new Expression[0]));
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.FUNCTION_CALL;
    }

    @Override
    public boolean accept(@Nonnull NodeVisitor visitor) {
        if (visitor.visitFunctionCall(this)) {
            return true;
        }
        for (Expression arg : mArguments) {
            if (arg.accept(visitor)) {
                return true;
            }
        }
        return false;
    }

    public FunctionDecl getFunction() {
        return mFunction;
    }

    public Expression[] getArguments() {
        return mArguments;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        Expression[] arguments = mArguments.clone();
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].clone();
        }
        return new FunctionCall(position, getType(), mFunction, arguments);
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Expression arg : mArguments) {
            joiner.add(arg.toString(Operator.PRECEDENCE_SEQUENCE));
        }
        return mFunction.getName() + "(" + joiner + ")";
    }
}
