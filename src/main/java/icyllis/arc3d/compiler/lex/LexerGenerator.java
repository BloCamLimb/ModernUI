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

package icyllis.arc3d.compiler.lex;

import icyllis.arc3d.core.MathUtil;
import it.unimi.dsi.fastutil.ints.*;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Generates the {@link Lexer} class.
 */
public class LexerGenerator {

    // literal, keyword, identifier, operator, comment
    public static final String LEXICON = """
            INTLITERAL     = ([1-9]\\d*|0[0-7]*|0[xX][\\da-fA-F]+)[uU]?
            FLOATLITERAL   = (\\d*\\.\\d+([eE][+-]?\\d+)?|\\d+\\.\\d*([eE][+-]?\\d+)?|\\d+([eE][+-]?\\d+))[fF]?
            STRINGLITERAL  = \\"[^\\"\\\\\\r\\n]*\\"
            TRUE           = "true"
            FALSE          = "false"
            BREAK          = "break"
            CONTINUE       = "continue"
            DO             = "do"
            FOR            = "for"
            WHILE          = "while"
            IF             = "if"
            ELSE           = "else"
            SWITCH         = "switch"
            CASE           = "case"
            DEFAULT        = "default"
            DISCARD        = "discard"
            RETURN         = "return"
            IN             = "in"
            OUT            = "out"
            INOUT          = "inout"
            CONST          = "const"
            UNIFORM        = "uniform"
            BUFFER         = "buffer"
            WORKGROUP      = "workgroup"
            SMOOTH         = "smooth"
            FLAT           = "flat"
            NOPERSPECTIVE  = "noperspective"
            COHERENT       = "coherent"
            VOLATILE       = "volatile"
            RESTRICT       = "restrict"
            READONLY       = "readonly"
            WRITEONLY      = "writeonly"
            SUBROUTINE     = "subroutine"
            LAYOUT         = "layout"
            STRUCT         = "struct"
            USING          = "using"
            INLINE         = "inline"
            NOINLINE       = "noinline"
            PURE           = "__pure"
            RESERVED       = shared|attribute|varying|atomic_uint|lowp|mediump|highp|precision|common|partition|active|asm|class|union|enum|typedef|template|this|resource|goto|public|static|extern|external|interface|long|double|fixed|unsigned|superp|input|output|hvec[234]|dvec[234]|fvec[234]|filter|sizeof|cast|namespace|[iu]?(sampler|image|texture)2DRect|sampler2DRectShadow|sampler3DRect
            IDENTIFIER     = [a-zA-Z_]\\w*
            HASH           = "#"
            LPAREN         = "("
            RPAREN         = ")"
            LBRACE         = "{"
            RBRACE         = "}"
            LBRACKET       = "["
            RBRACKET       = "]"
            DOT            = "."
            COMMA          = ","
            EQ             = "="
            LT             = "<"
            GT             = ">"
            BANG           = "!"
            TILDE          = "~"
            QUES           = "?"
            COLON          = ":"
            EQEQ           = "=="
            LTEQ           = "<="
            GTEQ           = ">="
            BANGEQ         = "!="
            PLUSPLUS       = "++"
            MINUSMINUS     = "--"
            PLUS           = "+"
            MINUS          = "-"
            STAR           = "*"
            SLASH          = "/"
            PERCENT        = "%"
            LTLT           = "<<"
            GTGT           = ">>"
            AMPAMP         = "&&"
            PIPEPIPE       = "||"
            CARETCARET     = "^^"
            AMP            = "&"
            PIPE           = "|"
            CARET          = "^"
            PLUSEQ         = "+="
            MINUSEQ        = "-="
            STAREQ         = "*="
            SLASHEQ        = "/="
            PERCENTEQ      = "%="
            LTLTEQ         = "<<="
            GTGTEQ         = ">>="
            AMPEQ          = "&="
            PIPEEQ         = "|="
            CARETEQ        = "^="
            SEMICOLON      = ";"
            NEWLINE        = [\\r\\n]+
            WHITESPACE     = [\\t\\013\\014 ]+
            LINE_COMMENT   = //.*
            BLOCK_COMMENT  = /\\*(/|\\**[^/*])*\\*+/
            INVALID        = .""";
    // skia's BLOCK_COMMENT does not match odd number of asterisks, e.g. /*****/, we fixed that

    // The number of bits to use per entry in our packed transition table. This is customizable:
    // - 1-bit: reasonable in theory. Doesn't actually pack many slices.
    // - 2-bit: best fit for our data. Packs extremely well.
    // - 4-bit: packs all but one slice, but doesn't save as much space overall.
    // - 8-bit: way too large (an 8-bit LUT plus an 8-bit data table is as big as a 16-bit table)
    // Other values don't divide cleanly into a byte and do not work.
    public static final int NUM_BITS = 2;

    // These values are derived from NUM_BITS and shouldn't need to change.
    public static final int NUM_VALUES = (1 << NUM_BITS) - 1;
    public static final int DATA_PER_BYTE = Byte.SIZE / NUM_BITS;
    public static final int DATA_PER_BYTE_SHIFT = Integer.numberOfTrailingZeros(DATA_PER_BYTE);

    @NonNull
    public static DFA process(PrintWriter pw) {
        NFA nfa = new NFA();
        List<String> tokens = new ArrayList<>();
        tokens.add("END_OF_FILE");
        RegexParser parser = new RegexParser();
        // the line separator in Java text block is always ('\n'), which is system-independent
        for (String line : LEXICON.split("\n")) {
            String[] split = line.split("=", 2);
            assert split.length == 2;
            String name = split[0].trim();
            String pattern = split[1].trim();
            assert !name.isEmpty();
            assert !pattern.isEmpty();
            tokens.add(name);
            if (pattern.startsWith("\"")) {
                assert (pattern.length() > 2 && pattern.endsWith("\""));
                RegexNode node = RegexNode.Char(pattern.charAt(1));
                for (int i = 2; i < pattern.length() - 1; ++i) {
                    node = RegexNode.Concat(node,
                            RegexNode.Char(pattern.charAt(i)));
                }
                nfa.add(node);
            } else {
                nfa.add(parser.parse(pattern));
            }
        }
        NFAtoDFA converter = new NFAtoDFA(nfa);
        DFA dfa = converter.convert();

        pw.println("public static final int");
        for (int i = 0; i < tokens.size(); i++) {
            pw.println("TK_" + tokens.get(i) + " = " + i + ",");
        }
        pw.println("TK_NONE = " + tokens.size() + ";");

        return dfa;
    }

    public static void writeTransitionTable(PrintWriter pw, DFA dfa, int states) {
        record MutablePackedEntry(IntList v, IntList data) {
            // v.size() == NUM_VALUES
        }

        int numTransitions = dfa.mTransitions.length;

        // Assemble our packed and full data tables, and an index into them.
        List<MutablePackedEntry> packedEntries = new ArrayList<>();
        List<IntList> fullEntries = new ArrayList<>();
        IntList indices = new IntArrayList();
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
                // Use a packed representation that squishes each value down to a few bits.
                // Create a packed entry with the unique values from the transition set,
                // padded out with zeros and sorted.
                var result = new MutablePackedEntry(new IntArrayList(NUM_VALUES), new IntArrayList());
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

                // Look for an existing entry that exactly matches this one.
                int index = packedEntries.indexOf(result);

                if (index == -1) {
                    // Add this as a new entry.
                    index = packedEntries.size();
                    packedEntries.add(result);
                }

                // Packed entries start at 0 and go up from there.
                indices.add(index);
            } else {
                // This table contained a large number of values. We can't pack it.
                // Create a full entry with this data.
                // Look for an existing entry that exactly matches this one.
                int index = fullEntries.indexOf(data);

                if (index == -1) {
                    // Add this as a new entry.
                    index = fullEntries.size();
                    fullEntries.add(data);
                }

                // Bit-not is used so that full entries start at -1 and go down from there.
                indices.add(~index);
            }
        }

        // Find the largest value for each packed-entry slot.
        int maxValue = 0;
        for (var entry : packedEntries) {
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
        for (int i = 0, end = fullEntries.size(); i < end; i++) {
            IntList data = fullEntries.get(i);
            assert (data.size() == numTransitions);
            pw.println("{");
            for (int j = 0; j < numTransitions; j++) {
                pw.print((short) data.getInt(j)); // unsigned
                if (j == numTransitions - 1) {
                    pw.println();
                } else if ((j % 9) == 8) {
                    pw.println(",");
                } else {
                    pw.print(", ");
                }
            }
            if (i == end - 1) {
                pw.println("}");
            } else {
                pw.println("},");
            }
        }
        pw.println("};");

        final int packedDataSize = MathUtil.alignTo(numTransitions, DATA_PER_BYTE);
        pw.println("public static final PackedEntry[] PACKED = {");
        for (int i = 0, end = packedEntries.size(); i < end; i++) {
            MutablePackedEntry entry = packedEntries.get(i);
            pw.print("new PackedEntry(");
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

            int j = 0, shiftBits = 0, combinedBits = 0;
            for (int index = 0; index < numTransitions; index++) {
                combinedBits |= entry.data.getInt(index) << shiftBits;
                shiftBits += NUM_BITS;
                // assert shiftBits <= 8;
                if (shiftBits == 8) {
                    pw.print((byte) combinedBits); // unsigned
                    if (j == packedDataSize - 1) {
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
                pw.println((byte) combinedBits);
            }
            if (i == end - 1) {
                pw.println("})");
            } else {
                pw.println("}),");
            }
        }
        pw.println("};");

        pw.println("public static final short[] INDICES = {");
        for (int index = 0, end = indices.size(); index < end; index++) {
            pw.print(indices.getInt(index));
            if (index == end - 1) {
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
        pw.println("final PackedEntry entry = PACKED[index];");
        pw.println("int v = entry.data[transition >> " + DATA_PER_BYTE_SHIFT + "] & 0xFF;");
        pw.println("v >>= " + NUM_BITS + " * (transition & " + (DATA_PER_BYTE - 1) + ");");
        pw.println("v &= " + NUM_VALUES + ";");
        pw.println("v *= " + bitsPerValue + ";");
        pw.println("return (entry.values >>> v) & " + maxValue + ";");
        pw.println("}");
    }

    /**
     * Generates the lexer source code.
     */
    public static void main(String[] args) {
        PrintWriter pw = new PrintWriter(System.out, false, StandardCharsets.UTF_8);

        DFA dfa = process(pw);

        { // Check the first character mapped in our DFA.
            int c = 0, length = dfa.mCharMappings.length;
            for (; c < length; ++c) {
                if (dfa.mCharMappings[c] != 0) {
                    break;
                }
            }
            assert c == NFAtoDFA.START_CHAR;
            assert length == (NFAtoDFA.END_CHAR + 1);
        }

        pw.println("public static final byte[] MAPPINGS = {");
        for (int i = 0; i <= NFAtoDFA.END_CHAR - NFAtoDFA.START_CHAR; ++i) {
            pw.print(dfa.mCharMappings[i + NFAtoDFA.START_CHAR]);
            if (i == NFAtoDFA.END_CHAR - NFAtoDFA.START_CHAR) {
                pw.println();
            } else if ((i % 9) == 8) {
                pw.println(",");
            } else {
                pw.print(", ");
            }
        }
        pw.println("};");

        int states = 0;
        for (int[] row : dfa.mTransitions) {
            states = Math.max(states, row.length);
        }
        writeTransitionTable(pw, dfa, states);

        pw.println("public static final byte[] ACCEPTS = {");
        for (int i = 0; i < states; i++) {
            if (i < dfa.mAccepts.length) {
                pw.print(dfa.mAccepts[i]);
            } else {
                pw.print(DFA.INVALID);
            }
            if (i == states - 1) {
                pw.println();
            } else if ((i % 9) == 8) {
                pw.println(",");
            } else {
                pw.print(", ");
            }
        }
        pw.println("};");

        pw.flush();
    }
}
