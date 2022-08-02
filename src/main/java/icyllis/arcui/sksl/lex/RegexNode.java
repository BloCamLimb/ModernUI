/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.sksl.lex;

import it.unimi.dsi.fastutil.ints.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the parse tree of a regular expression.
 */
public class RegexNode {

    public static final int Kind_Char = 0;
    public static final int Kind_Charset = 1;
    public static final int Kind_Concat = 2;
    public static final int Kind_Dot = 3;
    public static final int Kind_Or = 4;
    public static final int Kind_Plus = 5;
    public static final int Kind_Range = 6;
    public static final int Kind_Question = 7;
    public static final int Kind_Star = 8;

    public int mKind;
    public char mPayload;
    public final List<RegexNode> mChildren = new ArrayList<>();

    public RegexNode(int kind) {
        mKind = kind;
    }

    public RegexNode(int kind, char payload) {
        mKind = kind;
        mPayload = payload;
    }

    public RegexNode(int kind, String children) {
        mKind = kind;
        mPayload = 0;
        for (int i = 0; i < children.length(); i++) {
            mChildren.add(new RegexNode(Kind_Char, children.charAt(i)));
        }
    }

    public RegexNode(int kind, RegexNode child) {
        mKind = kind;
        mChildren.add(child);
    }

    public RegexNode(int kind, RegexNode child1, RegexNode child2) {
        mKind = kind;
        mChildren.add(child1);
        mChildren.add(child2);
    }

    /**
     * Creates NFA states for this node, with a successful match against this node resulting in a
     * transition to all of the states in the accept vector.
     */
    public IntList createStates(NFA nfa, IntList accept) {
        return switch (mKind) {
            case Kind_Char -> IntLists.singleton(nfa.addState(new NFAState(mPayload, accept)));
            case Kind_Charset -> {
                var chars = new IntArrayList();
                for (var child : mChildren) {
                    if (child.mKind == Kind_Char) {
                        while (chars.size() <= child.mPayload) {
                            chars.add(0);
                        }
                        chars.set(child.mPayload, 1);
                    } else {
                        assert (child.mKind == Kind_Range);
                        while (chars.size() <= child.mChildren.get(1).mPayload) {
                            chars.add(0);
                        }
                        for (char c = child.mChildren.get(0).mPayload;
                             c <= child.mChildren.get(1).mPayload;
                             ++c) {
                            chars.set(c, 1);
                        }
                    }
                }
                yield IntLists.singleton(nfa.addState(new NFAState(mPayload != 0, chars, accept)));
            }
            case Kind_Concat -> {
                var right = mChildren.get(1).createStates(nfa, accept);
                yield mChildren.get(0).createStates(nfa, right);
            }
            case Kind_Dot -> IntLists.singleton(nfa.addState(new NFAState(NFAState.Kind_Dot, accept)));
            case Kind_Or -> {
                var result = new IntArrayList(mChildren.get(0).createStates(nfa, accept));
                result.addAll(mChildren.get(1).createStates(nfa, accept));
                yield result;
            }
            case Kind_Plus -> {
                var next = new IntArrayList(accept);
                int id = nfa.addState(NFAState.PLACEHOLDER);
                next.add(id);
                var result = mChildren.get(0).createStates(nfa, next);
                nfa.mStates.set(id, new NFAState(result));
                yield result;
            }
            case Kind_Question -> {
                var result = new IntArrayList(mChildren.get(0).createStates(nfa, accept));
                result.addAll(accept);
                yield result;
            }
            case Kind_Star -> {
                var next = new IntArrayList(accept);
                int id = nfa.addState(NFAState.PLACEHOLDER);
                next.add(id);
                var result = new IntArrayList(mChildren.get(0).createStates(nfa, next));
                result.addAll(accept);
                nfa.mStates.set(id, new NFAState(result));
                yield result;
            }
            default -> throw new IllegalStateException();
        };
    }
}
