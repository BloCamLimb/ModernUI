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

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Generated SkSL lexicon.
 */
public class Main {

    /**
     * Modified SkSL lexicon.
     * <p>
     * Added: smooth
     * <p>
     * Removed: static if, static switch, highp, mediump, lowp, es3
     */
    public static final String LEXICON = """
            FLOAT_LITERAL  = [0-9]*\\.[0-9]+([eE][+-]?[0-9]+)?|[0-9]+\\.[0-9]*([eE][+-]?[0-9]+)?|[0-9]+([eE][+-]?[0-9]+)
            INT_LITERAL    = ([1-9][0-9]*|0[0-7]*|0[xX][0-9a-fA-F]+)[uU]?
            BAD_OCTAL      = (0[0-9]+)[uU]?
            TRUE_LITERAL   = "true"
            FALSE_LITERAL  = "false"
            IF             = "if"
            ELSE           = "else"
            FOR            = "for"
            WHILE          = "while"
            DO             = "do"
            SWITCH         = "switch"
            CASE           = "case"
            DEFAULT        = "default"
            BREAK          = "break"
            CONTINUE       = "continue"
            DISCARD        = "discard"
            RETURN         = "return"
            IN             = "in"
            OUT            = "out"
            INOUT          = "inout"
            UNIFORM        = "uniform"
            CONST          = "const"
            SMOOTH         = "smooth"
            FLAT           = "flat"
            NOPERSPECTIVE  = "noperspective"
            INLINE         = "inline"
            NOINLINE       = "noinline"
            HASSIDEEFFECTS = "sk_has_side_effects"
            STRUCT         = "struct"
            LAYOUT         = "layout"
            THREADGROUP    = "threadgroup"
            RESERVED       = attribute|varying|precision|invariant|asm|class|union|enum|typedef|template|this|packed|goto|volatile|public|static|extern|external|interface|long|double|fixed|unsigned|superp|input|output|hvec[234]|dvec[234]|fvec[234]|sampler[12]DShadow|sampler3DRect|sampler2DRectShadow|samplerCube|sizeof|cast|namespace|using|gl_[0-9a-zA-Z_]*
            IDENTIFIER     = [a-zA-Z_$][0-9a-zA-Z_]*
            DIRECTIVE      = #[a-zA-Z_][0-9a-zA-Z_]*
            LPAREN         = "("
            RPAREN         = ")"
            LBRACE         = "{"
            RBRACE         = "}"
            LBRACKET       = "["
            RBRACKET       = "]"
            DOT            = "."
            COMMA          = ","
            PLUSPLUS       = "++"
            MINUSMINUS     = "--"
            PLUS           = "+"
            MINUS          = "-"
            STAR           = "*"
            SLASH          = "/"
            PERCENT        = "%"
            SHL            = "<<"
            SHR            = ">>"
            BITWISEOR      = "|"
            BITWISEXOR     = "^"
            BITWISEAND     = "&"
            BITWISENOT     = "~"
            LOGICALOR      = "||"
            LOGICALXOR     = "^^"
            LOGICALAND     = "&&"
            LOGICALNOT     = "!"
            QUESTION       = "?"
            COLON          = ":"
            EQ             = "="
            EQEQ           = "=="
            NEQ            = "!="
            GT             = ">"
            LT             = "<"
            GTEQ           = ">="
            LTEQ           = "<="
            PLUSEQ         = "+="
            MINUSEQ        = "-="
            STAREQ         = "*="
            SLASHEQ        = "/="
            PERCENTEQ      = "%="
            SHLEQ          = "<<="
            SHREQ          = ">>="
            BITWISEOREQ    = "|="
            BITWISEXOREQ   = "^="
            BITWISEANDEQ   = "&="
            SEMICOLON      = ";"
            WHITESPACE     = \\s+
            LINE_COMMENT   = //.*
            BLOCK_COMMENT  = /\\*([^*]|\\*[^/])*\\*/
            INVALID        = .""";

    // The number of bits to use per entry in our compact transition table. This is customizable:
    // - 1-bit: reasonable in theory. Doesn't actually pack many slices.
    // - 2-bit: best fit for our data. Packs extremely well.
    // - 4-bit: packs all but one slice, but doesn't save as much space overall.
    // - 8-bit: way too large (an 8-bit LUT plus an 8-bit data table is as big as a 16-bit table)
    // Other values don't divide cleanly into a byte and do not work.
    public static final int NUM_BITS = 2;

    // These values are derived from NUM_BITS and shouldn't need to change.
    public static final int NUM_VALUES = (1 << NUM_BITS) - 1;
    public static final int DATA_PER_BYTE = Byte.SIZE / NUM_BITS;
    public static final int DATA_PER_BYTE_SHIFT = Integer.SIZE - Integer.numberOfLeadingZeros(DATA_PER_BYTE - 1);

    /**
     * Generates the lexicon texts.
     *
     * @param args no args required
     */
    public static void main(String[] args) {
        final PrintWriter pw = new PrintWriter(System.out, false, StandardCharsets.UTF_8);

        final NFA nfa = new NFA();
        final ArrayList<String> tokens = new ArrayList<>();
        tokens.add("END_OF_FILE");
        final RegexParser regexParser = new RegexParser();
        for (var line : LEXICON.split("\n")) {
            String[] split = line.split("\\s+");
            assert split.length == 3;
            String name = split[0];
            String delimiter = split[1];
            String pattern = split[2];
            assert !name.isEmpty();
            assert delimiter.equals("=");
            assert !pattern.isEmpty();
            tokens.add(name);
            if (pattern.startsWith("\"")) {
                assert (pattern.length()) > 2 && pattern.endsWith("\"");
                var node = new RegexNode(RegexNode.kChar_Kind, pattern.charAt(1));
                for (int i = 2; i < pattern.length() - 1; ++i) {
                    node = new RegexNode(RegexNode.kConcat_Kind, node,
                            new RegexNode(RegexNode.kChar_Kind, pattern.charAt(i)));
                }
                nfa.addRegex(node);
            } else {
                nfa.addRegex(regexParser.parse(pattern));
            }
        }
        final NFAtoDFA converter = new NFAtoDFA(nfa);
        final DFA dfa = converter.convert();

        pw.println("public static final int");
        for (int i = 0, e = tokens.size(); i < e; i++) {
            pw.println("TK_" + tokens.get(i) + " = " + i + ",");
        }
        pw.println("TK_NONE = DFA.INVALID;");

        int states = 0;
        for (var row : dfa.mTransitions) {
            states = Math.max(states, row.length);
        }

        // Find the first character mapped in our DFA.
        int startChar = 0;
        for (; startChar < dfa.mCharMappings.length; ++startChar) {
            if (dfa.mCharMappings[startChar] != 0) {
                break;
            }
        }

        assert startChar == NFAtoDFA.START_CHAR;
        pw.println("public static final byte[] MAPPINGS = {");
        for (int index = 0, end = dfa.mCharMappings.length - startChar; index < end; index++) {
            pw.print(String.valueOf((byte) dfa.mCharMappings[index + startChar]));
            if (index == end - 1) {
                pw.println();
            } else if ((index % 9) == 8) {
                pw.println(",");
            } else {
                pw.print(", ");
            }
        }
        pw.println("};");

        record WorkingCompactEntry(IntArrayList v, IntArrayList data) {
        }

        final int numTransitions = dfa.mTransitions.length;

        // Assemble our compact and full data tables, and an index into them.
        final ArrayList<WorkingCompactEntry> compactEntries = new ArrayList<>();
        final ArrayList<IntArrayList> fullEntries = new ArrayList<>();
        final IntArrayList indices = new IntArrayList();
        for (int s = 0; s < states; ++s) {
            // Copy all the transitions for this state into a flat array, and into a histogram (counting
            // the number of unique state-transition values). Most states only transition to a few
            // possible new states.
            var transitionSet = new IntArraySet();
            var data = new IntArrayList(numTransitions);
            data.size(numTransitions);
            for (int t = 0; t < numTransitions; ++t) {
                if (s < dfa.mTransitions[t].length) {
                    int value = dfa.mTransitions[t][s];
                    assert (value >= 0 && value < states);
                    data.set(t, value);
                    transitionSet.add(value);
                }
            }

            transitionSet.remove(0);
            if (transitionSet.size() <= NUM_VALUES) {
                // This table only contained a small number of unique nonzero values.
                // Use a compact representation that squishes each value down to a few bits.
                // Create a compact entry with the unique values from the transition set, padded out with zeros
                // and sorted.
                var result = new WorkingCompactEntry(new IntArrayList(NUM_VALUES), new IntArrayList());
                result.v.addAll(transitionSet);
                result.v.size(NUM_VALUES);
                result.v.sort(IntComparators.OPPOSITE_COMPARATOR);

                // Create a mapping from real values to small values.
                var translationTable = new Int2IntArrayMap();
                for (int index = 0; index < result.v.size(); ++index) {
                    translationTable.put(result.v.getInt(index), index);
                }
                translationTable.put(0, result.v.size());

                // Convert the real values into small values.
                for (int index = 0; index < data.size(); ++index) {
                    int value = data.getInt(index);
                    assert (translationTable.containsKey(value));
                    result.data.add(translationTable.get(value));
                }

                int index = -1;
                // Look for an existing entry that exactly matches this one.
                for (int i = 0; i < compactEntries.size(); ++i) {
                    if (compactEntries.get(i).equals(result)) {
                        index = i;
                        break;
                    }
                }

                if (index == -1) {
                    // Add this as a new entry.
                    index = compactEntries.size();
                    compactEntries.add(result);
                }
                // Compact entries start at 0 and go up from there.
                indices.add(index);
            } else {
                // Create a full entry with this data.
                int index = -1;
                // Look for an existing entry that exactly matches this one.
                for (int i = 0; i < fullEntries.size(); ++i) {
                    if (fullEntries.get(i).equals(data)) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    // Add this as a new entry.
                    index = fullEntries.size();
                    fullEntries.add(data);
                }
                // Bit-not is used so that full entries start at -1 and go down from there.
                indices.add(~index);
            }
        }

        // Find the largest value for each compact-entry slot.
        int maxValue = 0;
        for (var entry : compactEntries) {
            for (int index = 0; index < NUM_VALUES; ++index) {
                maxValue = Math.max(maxValue, entry.v.getInt(index));
            }
        }

        // Figure out how many bits we need to store our max value.
        int bitsPerValue = Integer.SIZE - Integer.numberOfLeadingZeros(maxValue - 1);
        maxValue = (1 << bitsPerValue) - 1;

        // If we exceed 10 bits per value, three values would overflow 32 bits. If this happens, we'll
        // need to pack our values another way.
        assert (bitsPerValue <= Integer.SIZE / NUM_VALUES);

        pw.println("public static final short[][] FULL = {");
        for (int index = 0, end = fullEntries.size(); index < end; index++) {
            var data = fullEntries.get(index);
            assert (data.size() == numTransitions);
            pw.println("{");
            for (int j = 0; j < numTransitions; j++) {
                pw.print(String.valueOf((short) data.getInt(j)));
                if (j == numTransitions - 1) {
                    pw.println();
                } else if ((j % 9) == 8) {
                    pw.println(",");
                } else {
                    pw.print(", ");
                }
            }
            if (index == end - 1) {
                pw.println("}");
            } else {
                pw.println("},");
            }
        }
        pw.println("};");

        final int compactDataSize = (int) Math.ceil((double) numTransitions / DATA_PER_BYTE);
        pw.println("public static final CompactEntry[] COMPACT = {");
        for (int i = 0, e = compactEntries.size(); i < e; i++) {
            var entry = compactEntries.get(i);
            pw.print("new CompactEntry(");
            // We pack all three values into `v`. If NUM_BITS were to change, we would need to adjust
            // this packing algorithm.
            assert (entry.v.size() == NUM_VALUES);
            pw.print(entry.v.getInt(0));
            if (entry.v.getInt(1) != 0) {
                pw.print(" | (" + entry.v.getInt(1) + " << " + bitsPerValue + ")");
            }
            if (entry.v.getInt(2) != 0) {
                pw.print(" | (" + entry.v.getInt(2) + " << " + (2 * bitsPerValue) + ")");
            }
            pw.println(",");
            pw.println("new byte[]{");

            int shiftBits = 0, combinedBits = 0, j = 0;
            for (int index = 0; index < numTransitions; index++) {
                combinedBits |= entry.data.getInt(index) << shiftBits;
                shiftBits += NUM_BITS;
                if (shiftBits == 8) {
                    pw.print(String.valueOf((byte) combinedBits));
                    if (j == compactDataSize - 1) {
                        pw.println();
                    } else if ((j % 4) == 3) {
                        pw.println(",");
                    } else {
                        pw.print(", ");
                    }
                    shiftBits = 0;
                    combinedBits = 0;
                    j++;
                }
            }
            if (shiftBits > 0) {
                // Flush any partial values.
                pw.println(String.valueOf((byte) combinedBits));
            }
            if (i == e - 1) {
                pw.println("})");
            } else {
                pw.println("}),");
            }
        }
        pw.println("};");

        pw.println("public static final short[] INDICES = {");
        for (int index = 0, end = indices.size(); index < end; index++) {
            pw.print(String.valueOf((short) indices.getInt(index)));
            if (index == end - 1) {
                pw.println();
            } else if ((index % 9) == 8) {
                pw.println(",");
            } else {
                pw.print(", ");
            }
        }
        pw.println("};");

        pw.println("public static final byte[] ACCEPTS = {");
        for (int index = 0; index < states; index++) {
            if (index < dfa.mAccepts.length) {
                pw.print(String.valueOf((byte) dfa.mAccepts[index]));
            } else {
                pw.print(DFA.INVALID);
            }
            if (index == states - 1) {
                pw.println();
            } else if ((index % 9) == 8) {
                pw.println(",");
            } else {
                pw.print(", ");
            }
        }
        pw.println("};");

        pw.println("public static int getTransition(int transition, int state) {");
        pw.println("short index = INDICES[state];");
        pw.println("if (index < 0) return FULL[~index][transition] & 0xFFFF;");
        pw.println("final CompactEntry entry = COMPACT[index];");
        pw.print("int v = entry.data[transition >> ");
        pw.print(DATA_PER_BYTE_SHIFT);
        pw.println("] & 0xFF;");
        pw.print("v >>= ");
        pw.print(NUM_BITS);
        pw.print(" * (transition & ");
        pw.print(DATA_PER_BYTE - 1);
        pw.println(");");
        pw.print("v &= ");
        pw.print(NUM_VALUES);
        pw.println(";");
        pw.print("v *= ");
        pw.print(bitsPerValue);
        pw.println(";");
        pw.print("return (entry.values >>> v) & ");
        pw.print(maxValue);
        pw.println(";");
        pw.println("}");

        pw.flush();
    }
}
