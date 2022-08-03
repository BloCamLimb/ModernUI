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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Runtime-generated SkSL lexicon on static initializing.
 */
@SuppressWarnings("AssertWithSideEffects")
public class Lexer {

    /**
     * Modified SkSL lexicon.
     * <p>
     * Added: smooth
     * <p>
     * Removed: static if, static switch, highp, mediump, lowp, es3
     * <p>
     * Renamed: threadgroup -> shared
     */
    public static final String LEXICON = """
            FLOAT_LITERAL  = [0-9]*\\.[0-9]+([eE][+-]?[0-9]+)?|[0-9]+\\.[0-9]*([eE][+-]?[0-9]+)?|[0-9]+([eE][+-]?[0-9]+)
            INT_LITERAL    = ([1-9][0-9]*|0[0-7]*|0[xX][0-9a-fA-F]+)[uU]?
            BAD_OCTAL      = (0[0-9]+)[uU]?
            TRUE_LITERAL   = "true"
            FALSE_LITERAL  = "false"
            IF             = "if"
            STATIC_IF      = "@if"
            ELSE           = "else"
            FOR            = "for"
            WHILE          = "while"
            DO             = "do"
            SWITCH         = "switch"
            STATIC_SWITCH  = "@switch"
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
            FLAT           = "flat"
            NOPERSPECTIVE  = "noperspective"
            INLINE         = "inline"
            NOINLINE       = "noinline"
            HASSIDEEFFECTS = "sk_has_side_effects"
            STRUCT         = "struct"
            LAYOUT         = "layout"
            HIGHP          = "highp"
            MEDIUMP        = "mediump"
            LOWP           = "lowp"
            ES3            = "$es3"
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

    /**
     * 0 is reserved for END_OF_FILE, others are sequential and match with lexicon.
     */
    public static final int
            TK_END_OF_FILE = 0,
            TK_FLOAT_LITERAL = 1,
            TK_INT_LITERAL = 2,
            TK_BAD_OCTAL = 3,
            TK_TRUE_LITERAL = 4,
            TK_FALSE_LITERAL = 5,
            TK_IF = 6,
            TK_STATIC_IF = 7,
            TK_ELSE = 8,
            TK_FOR = 9,
            TK_WHILE = 10,
            TK_DO = 11,
            TK_SWITCH = 12,
            TK_STATIC_SWITCH = 13,
            TK_CASE = 14,
            TK_DEFAULT = 15,
            TK_BREAK = 16,
            TK_CONTINUE = 17,
            TK_DISCARD = 18,
            TK_RETURN = 19,
            TK_IN = 20,
            TK_OUT = 21,
            TK_INOUT = 22,
            TK_UNIFORM = 23,
            TK_CONST = 24,
            TK_FLAT = 25,
            TK_NOPERSPECTIVE = 26,
            TK_INLINE = 27,
            TK_NOINLINE = 28,
            TK_HASSIDEEFFECTS = 29,
            TK_STRUCT = 30,
            TK_LAYOUT = 31,
            TK_HIGHP = 32,
            TK_MEDIUMP = 33,
            TK_LOWP = 34,
            TK_ES3 = 35,
            TK_THREADGROUP = 36,
            TK_RESERVED = 37,
            TK_IDENTIFIER = 38,
            TK_DIRECTIVE = 39,
            TK_LPAREN = 40,
            TK_RPAREN = 41,
            TK_LBRACE = 42,
            TK_RBRACE = 43,
            TK_LBRACKET = 44,
            TK_RBRACKET = 45,
            TK_DOT = 46,
            TK_COMMA = 47,
            TK_PLUSPLUS = 48,
            TK_MINUSMINUS = 49,
            TK_PLUS = 50,
            TK_MINUS = 51,
            TK_STAR = 52,
            TK_SLASH = 53,
            TK_PERCENT = 54,
            TK_SHL = 55,
            TK_SHR = 56,
            TK_BITWISEOR = 57,
            TK_BITWISEXOR = 58,
            TK_BITWISEAND = 59,
            TK_BITWISENOT = 60,
            TK_LOGICALOR = 61,
            TK_LOGICALXOR = 62,
            TK_LOGICALAND = 63,
            TK_LOGICALNOT = 64,
            TK_QUESTION = 65,
            TK_COLON = 66,
            TK_EQ = 67,
            TK_EQEQ = 68,
            TK_NEQ = 69,
            TK_GT = 70,
            TK_LT = 71,
            TK_GTEQ = 72,
            TK_LTEQ = 73,
            TK_PLUSEQ = 74,
            TK_MINUSEQ = 75,
            TK_STAREQ = 76,
            TK_SLASHEQ = 77,
            TK_PERCENTEQ = 78,
            TK_SHLEQ = 79,
            TK_SHREQ = 80,
            TK_BITWISEOREQ = 81,
            TK_BITWISEXOREQ = 82,
            TK_BITWISEANDEQ = 83,
            TK_SEMICOLON = 84,
            TK_WHITESPACE = 85,
            TK_LINE_COMMENT = 86,
            TK_BLOCK_COMMENT = 87,
            TK_INVALID = 88,
            TK_NONE = DFA.INVALID;

    // The number of bits to use per entry in our compact transition table. This is customizable:
    // - 1-bit: reasonable in theory. Doesn't actually pack many slices.
    // - 2-bit: best fit for our data. Packs extremely well.
    // - 4-bit: packs all but one slice, but doesn't save as much space overall.
    // - 8-bit: way too large (an 8-bit LUT plus an 8-bit data table is as big as a 16-bit table)
    // Other values don't divide cleanly into a byte and do not work.
    public static final int NUM_BITS = 2;

    // These values are derived from kNumBits and shouldn't need to change.
    public static final int NUM_VALUES = (1 << NUM_BITS) - 1;
    public static final int DATA_PER_BYTE = Byte.SIZE / NUM_BITS;
    public static final int DATA_PER_BYTE_SHIFT = Integer.SIZE - Integer.numberOfLeadingZeros(DATA_PER_BYTE);

    // IndexEntry = int16_t; = short
    // FullEntry = uint16_t[numTransitions]; = short[]
    // CompactEntry = uint32_t values; uint8_t data[ceil((float) numTransitions / DATA_PER_BYTE)];
    public record CompactEntry(int values, byte[] data) {

        @Nonnull
        @Override
        public String toString() {
            return "CompactEntry{" +
                    "values=" + values +
                    ", data=" + Arrays.toString(data) +
                    '}';
        }
    }

    // Arbitrarily-chosen character which is greater than startChar, and should not appear in actual
    // input.
    public static final char INVALID_CHAR = 18;

    public static final byte[] MAPPINGS;
    public static final short[][] FULL;
    public static final CompactEntry[] COMPACT;
    public static final short[] INDICES;
    public static final byte[] ACCEPTS;

    public static final int BITS_PER_VALUE;
    public static final int MAX_VALUE;

    static {
        final NFA nfa = new NFA();
        final RegexParser regexParser = new RegexParser();
        int token = 0;
        for (var line : LEXICON.split("\n")) {
            String[] split = line.split("\\s+");
            assert split.length == 3;
            String name = split[0];
            String delimiter = split[1];
            String pattern = split[2];
            assert !name.isEmpty();
            assert delimiter.equals("=");
            assert !pattern.isEmpty();
            try {
                // we reserve token 0 for END_OF_FILE, so this starts at 1
                assert Lexer.class.getField("TK_" + name).getInt(null) == ++token;
            } catch (Exception e) {
                assert false;
            }
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
        MAPPINGS = new byte[dfa.mCharMappings.length - startChar];
        for (int index = 0; index < MAPPINGS.length; ++index) {
            MAPPINGS[index] = (byte) dfa.mCharMappings[index + startChar];
        }

        record WorkingCompactEntry(IntArrayList v, IntArrayList data) {
        }

        int numTransitions = dfa.mTransitions.length;

        // Assemble our compact and full data tables, and an index into them.
        ArrayList<WorkingCompactEntry> compactEntries = new ArrayList<>();
        ArrayList<IntArrayList> fullEntries = new ArrayList<>();
        IntArrayList indices = new IntArrayList();
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
        MAX_VALUE = (1 << bitsPerValue) - 1;

        // If we exceed 10 bits per value, three values would overflow 32 bits. If this happens, we'll
        // need to pack our values another way.
        assert (bitsPerValue <= Integer.SIZE / NUM_VALUES);
        BITS_PER_VALUE = bitsPerValue;

        FULL = new short[fullEntries.size()][];
        for (int i = 0; i < fullEntries.size(); ++i) {
            var data = fullEntries.get(i);
            assert (data.size() == numTransitions);
            short[] full = new short[numTransitions];
            for (int j = 0; j < data.size(); j++) {
                full[j] = (short) data.getInt(j);
            }
            FULL[i] = full;
        }

        int compactDataBytes = (int) Math.ceil((float) numTransitions / DATA_PER_BYTE);
        COMPACT = new CompactEntry[compactEntries.size()];
        for (int i = 0; i < compactEntries.size(); ++i) {
            var entry = compactEntries.get(i);
            // We pack all three values into `v`. If NUM_BITS were to change, we would need to adjust
            // this packing algorithm.
            assert (entry.v.size() == NUM_VALUES);
            int values = entry.v.getInt(0) |
                    (entry.v.getInt(1) << bitsPerValue) |
                    (entry.v.getInt(2) << (2 * bitsPerValue));

            byte[] compact = new byte[compactDataBytes];
            COMPACT[i] = new CompactEntry(values, compact);

            int shiftBits = 0, combinedBits = 0, j = 0;
            for (int index = 0; index < numTransitions; index++) {
                combinedBits |= entry.data.getInt(index) << shiftBits;
                shiftBits += NUM_BITS;
                if (shiftBits == 8) {
                    compact[j++] = (byte) combinedBits;
                    shiftBits = 0;
                    combinedBits = 0;
                }
            }
            if (shiftBits > 0) {
                // Flush any partial values.
                compact[j] = (byte) combinedBits;
            }
        }

        INDICES = new short[indices.size()];
        for (int i = 0; i < indices.size(); ++i) {
            INDICES[i] = (short) indices.getInt(i);
        }

        ACCEPTS = new byte[states];
        for (int i = 0; i < states; ++i) {
            if (i < dfa.mAccepts.length) {
                ACCEPTS[i] = (byte) dfa.mAccepts[i];
            } else {
                ACCEPTS[i] = DFA.INVALID;
            }
        }
    }

    public static int getTransition(int transition, int state) {
        short index = INDICES[state];
        if (index < 0) {
            // XXX: the maximum value does not exceed 32767, so it does not need to be converted to unsigned short
            return FULL[~index][transition];
        }
        CompactEntry entry = COMPACT[index];
        int v = entry.data[transition >> DATA_PER_BYTE_SHIFT] & 0xFF;
        v >>= NUM_BITS * (transition & (DATA_PER_BYTE - 1));
        v &= NUM_VALUES;
        v *= BITS_PER_VALUE;
        return (entry.values >>> v) & MAX_VALUE;
    }
}
