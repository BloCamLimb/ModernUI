/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.sksl.lex;

import it.unimi.dsi.fastutil.ints.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the parse tree of a regular expression.
 */
public class RegexNode {

    public static final int
            kChar_Kind = 0,
            kCharset_Kind = 1,
            kConcat_Kind = 2,
            kDot_Kind = 3,
            kOr_Kind = 4,
            kPlus_Kind = 5,
            kRange_Kind = 6,
            kQuestion_Kind = 7,
            kStar_Kind = 8;

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
            mChildren.add(new RegexNode(kChar_Kind, children.charAt(i)));
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
            case kChar_Kind -> IntLists.singleton(nfa.addState(new NFAState(mPayload, accept)));
            case kCharset_Kind -> {
                var chars = new IntArrayList();
                for (var child : mChildren) {
                    if (child.mKind == kChar_Kind) {
                        while (chars.size() <= child.mPayload) {
                            chars.add(0);
                        }
                        chars.set(child.mPayload, 1);
                    } else {
                        assert (child.mKind == kRange_Kind);
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
            case kConcat_Kind -> {
                var right = mChildren.get(1).createStates(nfa, accept);
                yield mChildren.get(0).createStates(nfa, right);
            }
            case kDot_Kind -> IntLists.singleton(nfa.addState(new NFAState(NFAState.Kind_Dot, accept)));
            case kOr_Kind -> {
                var result = new IntArrayList(mChildren.get(0).createStates(nfa, accept));
                result.addAll(mChildren.get(1).createStates(nfa, accept));
                yield result;
            }
            case kPlus_Kind -> {
                var next = new IntArrayList(accept);
                int id = nfa.addState(NFAState.PLACEHOLDER);
                next.add(id);
                var result = mChildren.get(0).createStates(nfa, next);
                nfa.mStates.set(id, new NFAState(result));
                yield result;
            }
            case kQuestion_Kind -> {
                var result = new IntArrayList(mChildren.get(0).createStates(nfa, accept));
                result.addAll(accept);
                yield result;
            }
            case kStar_Kind -> {
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
