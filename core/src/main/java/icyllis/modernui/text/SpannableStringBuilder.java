/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.text;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.util.GrowingArrayUtils;
import icyllis.modernui.util.Log;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * This is the class for text whose content and markup can both be changed.
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class SpannableStringBuilder implements Editable, Spannable, GetChars, Appendable {

    public static final Marker MARKER = MarkerFactory.getMarker("SpannableStringBuilder");

    private static final Pools.Pool<IntArrayList> sIntBufferPool = Pools.newSynchronizedPool(2);

    //TODO These value are tightly related to the public SPAN_MARK/POINT values in {@link Spanned}
    private static final int MARK = 1;
    private static final int POINT = 2;
    private static final int PARAGRAPH = 3;

    private static final int START_MASK = 0xF0;
    private static final int END_MASK = 0x0F;
    private static final int START_SHIFT = 4;

    // These bits are not (currently) used by SPANNED flags
    private static final int SPAN_ADDED = 0x800;
    private static final int SPAN_START_AT_START = 0x1000;
    private static final int SPAN_START_AT_END = 0x2000;
    private static final int SPAN_END_AT_START = 0x4000;
    private static final int SPAN_END_AT_END = 0x8000;
    private static final int SPAN_START_END_MASK = 0xF000;

    private static final InputFilter[] NO_FILTERS = {};

    @NonNull
    private InputFilter[] mFilters = NO_FILTERS;

    private char[] mText;
    private int mGapStart;
    private int mGapLength;

    private Object[] mSpans;
    private int[] mSpanStarts;
    private int[] mSpanEnds;
    private int[] mSpanMax;  // see calcMax() for an explanation of what this array stores
    private int[] mSpanFlags;
    private int[] mSpanOrder;  // store the order of span insertion
    private int mSpanInsertCount;  // counter for the span insertion

    private int mSpanCount;
    @Nullable
    private IdentityHashMap<Object, Integer> mIndexOfSpan;
    private int mLowWaterMark;  // indices below this have not been touched

    // TextWatcher callbacks may trigger changes that trigger more callbacks. This keeps track of
    // how deep the callbacks go.
    private int mTextWatcherDepth;

    /**
     * Create a new SpannableStringBuilder with empty contents
     */
    public SpannableStringBuilder() {
        this("");
    }

    /**
     * Create a new SpannableStringBuilder containing a copy of the
     * specified text, including its spans if any.
     */
    public SpannableStringBuilder(@NonNull CharSequence text) {
        this(text, 0, text.length());
    }

    /**
     * Create a new SpannableStringBuilder containing a copy of the
     * specified slice of the specified text, including its spans if any.
     */
    public SpannableStringBuilder(@NonNull CharSequence text, int start, int end) {
        final int srcLen = end - start;
        if (srcLen < 0) {
            throw new StringIndexOutOfBoundsException();
        }

        mText = new char[GrowingArrayUtils.growSize(srcLen)];
        mGapStart = srcLen;
        mGapLength = mText.length - srcLen;

        TextUtils.getChars(text, start, end, mText, 0);

        mSpanCount = 0;
        mSpanInsertCount = 0;
        mSpans = ObjectArrays.EMPTY_ARRAY;
        mSpanStarts = IntArrays.EMPTY_ARRAY;
        mSpanEnds = IntArrays.EMPTY_ARRAY;
        mSpanFlags = IntArrays.EMPTY_ARRAY;
        mSpanMax = IntArrays.EMPTY_ARRAY;
        mSpanOrder = IntArrays.EMPTY_ARRAY;

        if (text instanceof Spanned sp) {
            //TODO optimize if text is SpannableStringInternal
            List<?> spans = sp.getSpans(start, end, Object.class);

            for (int i = 0; i < spans.size(); i++) {
                Object span = spans.get(i);
                if (span instanceof NoCopySpan) {
                    continue;
                }

                int st = sp.getSpanStart(span) - start;
                int en = sp.getSpanEnd(span) - start;
                int fl = sp.getSpanFlags(span);

                if (st < 0)
                    st = 0;
                if (st > end - start)
                    st = end - start;

                if (en < 0)
                    en = 0;
                if (en > end - start)
                    en = end - start;

                setSpan(false, span, st, en, fl, false/*enforceParagraph*/);
            }
            restoreInvariants();
        }
    }

    @NonNull
    public static SpannableStringBuilder valueOf(@NonNull CharSequence source) {
        if (source instanceof SpannableStringBuilder) {
            return (SpannableStringBuilder) source;
        } else {
            return new SpannableStringBuilder(source);
        }
    }

    /**
     * Return the char at the specified offset within the buffer.
     */
    @Override
    public char charAt(int where) {
        int len = length();
        if (where < 0) {
            throw new IndexOutOfBoundsException("charAt: " + where + " < 0");
        } else if (where >= len) {
            throw new IndexOutOfBoundsException("charAt: " + where + " >= length " + len);
        }

        if (where >= mGapStart)
            return mText[where + mGapLength];
        else
            return mText[where];
    }

    /**
     * Return the number of chars in the buffer.
     */
    @Override
    public int length() {
        return mText.length - mGapLength;
    }

    private void resizeFor(int size) {
        final int oldLength = mText.length;
        if (size + 1 <= oldLength) {
            return;
        }

        char[] newText = new char[GrowingArrayUtils.growSize(size)];
        System.arraycopy(mText, 0, newText, 0, mGapStart);
        final int newLength = newText.length;
        final int delta = newLength - oldLength;
        final int after = oldLength - (mGapStart + mGapLength);
        System.arraycopy(mText, oldLength - after, newText, newLength - after, after);
        mText = newText;

        mGapLength += delta;
        if (mGapLength < 1)
            new Exception("mGapLength < 1").printStackTrace();

        if (mSpanCount != 0) {
            for (int i = 0; i < mSpanCount; i++) {
                if (mSpanStarts[i] > mGapStart) mSpanStarts[i] += delta;
                if (mSpanEnds[i] > mGapStart) mSpanEnds[i] += delta;
            }
            calcMax(treeRoot());
        }
    }

    private void moveGapTo(int where) {
        if (where == mGapStart)
            return;

        boolean atEnd = (where == length());

        if (where < mGapStart) {
            int overlap = mGapStart - where;
            System.arraycopy(mText, where, mText, mGapStart + mGapLength - overlap, overlap);
        } else /* where > mGapStart */ {
            int overlap = where - mGapStart;
            System.arraycopy(mText, where + mGapLength - overlap, mText, mGapStart, overlap);
        }

        // TODO: be more clever (although the win really isn't that big)
        if (mSpanCount != 0) {
            for (int i = 0; i < mSpanCount; i++) {
                int start = mSpanStarts[i];
                int end = mSpanEnds[i];

                if (start > mGapStart)
                    start -= mGapLength;
                if (start > where)
                    start += mGapLength;
                else if (start == where) {
                    int flag = (mSpanFlags[i] & START_MASK) >> START_SHIFT;

                    if (flag == POINT || (atEnd && flag == PARAGRAPH))
                        start += mGapLength;
                }

                if (end > mGapStart)
                    end -= mGapLength;
                if (end > where)
                    end += mGapLength;
                else if (end == where) {
                    int flag = (mSpanFlags[i] & END_MASK);

                    if (flag == POINT || (atEnd && flag == PARAGRAPH))
                        end += mGapLength;
                }

                mSpanStarts[i] = start;
                mSpanEnds[i] = end;
            }
            calcMax(treeRoot());
        }

        mGapStart = where;
    }

    /**
     * Convenience for replace(where, where, text, start, end)
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    public SpannableStringBuilder insert(int where, @NonNull CharSequence tb, int start, int end) {
        return replace(where, where, tb, start, end);
    }

    /**
     * Convenience for replace(where, where, text, 0, text.length());
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    public SpannableStringBuilder insert(int where, @NonNull CharSequence tb) {
        return replace(where, where, tb, 0, tb.length());
    }

    /**
     * Convenience for replace(st, en, "", 0, 0)
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    public SpannableStringBuilder delete(int start, int end) {
        SpannableStringBuilder ret = replace(start, end, "", 0, 0);

        if (mGapLength > 2 * length()) {
            resizeFor(length());
        }
        return ret; // == this
    }

    /**
     * Convenience for replace(0, length(), "", 0, 0).
     * Note that this clears the text, not the spans;
     * use {@link #clearSpans} if you need that.
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    public void clear() {
        replace(0, length(), "", 0, 0);
        mSpanInsertCount = 0;
    }

    /**
     * Removes all spans from the Editable, as if by calling
     * {@link #removeSpan} on each of them.
     */
    @Override
    public void clearSpans() {
        for (int i = mSpanCount - 1; i >= 0; i--) {
            Object what = mSpans[i];
            int ostart = mSpanStarts[i];
            int oend = mSpanEnds[i];

            if (ostart > mGapStart)
                ostart -= mGapLength;
            if (oend > mGapStart)
                oend -= mGapLength;

            mSpanCount = i;
            mSpans[i] = null;

            sendSpanRemoved(what, ostart, oend);
        }
        if (mIndexOfSpan != null) {
            mIndexOfSpan.clear();
        }
        mSpanInsertCount = 0;
    }

    /**
     * Convenience for replace(length(), length(), text, 0, text.length())
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    public SpannableStringBuilder append(@NonNull CharSequence text) {
        int length = length();
        return replace(length, length, text, 0, text.length());
    }

    /**
     * Appends the character sequence {@code text} and spans {@code what} over the appended part.
     * See {@link Spanned} for an explanation of what the flags mean.
     *
     * @param text  the character sequence to append.
     * @param what  the object to be spanned over the appended text.
     * @param flags see {@link Spanned}.
     * @return this {@code SpannableStringBuilder}.
     */
    public SpannableStringBuilder append(@NonNull CharSequence text, @NonNull Object what, int flags) {
        int start = length();
        append(text);
        setSpan(what, start, length(), flags);
        return this;
    }

    /**
     * Convenience for replace(length(), length(), text, start, end)
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    public SpannableStringBuilder append(@NonNull CharSequence text, int start, int end) {
        int length = length();
        return replace(length, length, text, start, end);
    }

    /**
     * Convenience for append(String.valueOf(text)).
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    public SpannableStringBuilder append(char text) {
        return append(String.valueOf(text));
    }

    // Returns true if a node was removed (so we can restart search from root)
    private boolean removeSpansForChange(int start, int end, boolean textIsRemoved, int i) {
        if ((i & 1) != 0) {
            // internal tree node
            if (resolveGap(mSpanMax[i]) >= start &&
                    removeSpansForChange(start, end, textIsRemoved, leftChild(i))) {
                return true;
            }
        }
        if (i < mSpanCount) {
            if ((mSpanFlags[i] & Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) ==
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE &&
                    mSpanStarts[i] >= start && mSpanStarts[i] < mGapStart + mGapLength &&
                    mSpanEnds[i] >= start && mSpanEnds[i] < mGapStart + mGapLength &&
                    // The following condition indicates that the span would become empty
                    (textIsRemoved || mSpanStarts[i] > start || mSpanEnds[i] < mGapStart)) {
                assert mIndexOfSpan != null;
                mIndexOfSpan.remove(mSpans[i]);
                removeSpan(i, 0 /* flags */);
                return true;
            }
            return resolveGap(mSpanStarts[i]) <= end && (i & 1) != 0 &&
                    removeSpansForChange(start, end, textIsRemoved, rightChild(i));
        }
        return false;
    }

    private void change(int start, int end, @NonNull CharSequence cs, int csStart, int csEnd) {
        // Can be negative
        final int replacedLength = end - start;
        final int replacementLength = csEnd - csStart;
        final int nbNewChars = replacementLength - replacedLength;

        boolean changed = false;
        for (int i = mSpanCount - 1; i >= 0; i--) {
            int spanStart = mSpanStarts[i];
            if (spanStart > mGapStart)
                spanStart -= mGapLength;

            int spanEnd = mSpanEnds[i];
            if (spanEnd > mGapStart)
                spanEnd -= mGapLength;

            if ((mSpanFlags[i] & SPAN_PARAGRAPH) == SPAN_PARAGRAPH) {
                int ost = spanStart;
                int oen = spanEnd;
                int clen = length();

                if (spanStart > start && spanStart <= end) {
                    for (spanStart = end; spanStart < clen; spanStart++)
                        if (spanStart > end && charAt(spanStart - 1) == '\n')
                            break;
                }

                if (spanEnd > start && spanEnd <= end) {
                    for (spanEnd = end; spanEnd < clen; spanEnd++)
                        if (spanEnd > end && charAt(spanEnd - 1) == '\n')
                            break;
                }

                if (spanStart != ost || spanEnd != oen) {
                    setSpan(false, mSpans[i], spanStart, spanEnd, mSpanFlags[i],
                            true/*enforceParagraph*/);
                    changed = true;
                }
            }

            int flags = 0;
            if (spanStart == start) flags |= SPAN_START_AT_START;
            else if (spanStart == end + nbNewChars) flags |= SPAN_START_AT_END;
            if (spanEnd == start) flags |= SPAN_END_AT_START;
            else if (spanEnd == end + nbNewChars) flags |= SPAN_END_AT_END;
            mSpanFlags[i] |= flags;
        }
        if (changed) {
            restoreInvariants();
        }

        moveGapTo(end);

        if (nbNewChars >= mGapLength) {
            resizeFor(mText.length + nbNewChars - mGapLength);
        }

        final boolean textIsRemoved = replacementLength == 0;
        // The removal pass needs to be done before the gap is updated in order to broadcast the
        // correct previous positions to the correct intersecting SpanWatchers
        if (replacedLength > 0) { // no need for span fixup on pure insertion
            //noinspection StatementWithEmptyBody
            while (mSpanCount > 0 &&
                    removeSpansForChange(start, end, textIsRemoved, treeRoot())) {
                // keep deleting spans as needed, and restart from root after every deletion
                // because deletion can invalidate an index.
            }
        }

        mGapStart += nbNewChars;
        mGapLength -= nbNewChars;

        if (mGapLength < 1)
            new Exception("mGapLength < 1").printStackTrace();

        TextUtils.getChars(cs, csStart, csEnd, mText, start);

        if (replacedLength > 0) { // no need for span fixup on pure insertion
            // TODO potential optimization: only update bounds on intersecting spans
            final boolean atEnd = (mGapStart + mGapLength == mText.length);

            for (int i = 0; i < mSpanCount; i++) {
                final int startFlag = (mSpanFlags[i] & START_MASK) >> START_SHIFT;
                mSpanStarts[i] = updatedIntervalBound(mSpanStarts[i], start, nbNewChars, startFlag,
                        atEnd, textIsRemoved);

                final int endFlag = (mSpanFlags[i] & END_MASK);
                mSpanEnds[i] = updatedIntervalBound(mSpanEnds[i], start, nbNewChars, endFlag,
                        atEnd, textIsRemoved);
            }
            // TODO potential optimization: only fix up invariants when bounds actually changed
            restoreInvariants();
        }

        if (cs instanceof Spanned sp) {
            //TODO optimize if cs is SpannableStringInternal
            List<?> spans = sp.getSpans(csStart, csEnd, Object.class);

            for (int i = 0; i < spans.size(); i++) {
                Object span = spans.get(i);
                int st = sp.getSpanStart(span);
                int en = sp.getSpanEnd(span);

                if (st < csStart) st = csStart;
                if (en > csEnd) en = csEnd;

                // Add span only if this object is not yet used as a span in this string
                if (getSpanStart(span) < 0) {
                    int copySpanStart = st - csStart + start;
                    int copySpanEnd = en - csStart + start;
                    int copySpanFlags = sp.getSpanFlags(span) | SPAN_ADDED;

                    setSpan(false, span, copySpanStart, copySpanEnd, copySpanFlags,
                            false/*enforceParagraph*/);
                }
            }
            restoreInvariants();
        }
    }

    private int updatedIntervalBound(int offset, int start, int nbNewChars, int flag, boolean atEnd,
                                     boolean textIsRemoved) {
        if (offset >= start && offset < mGapStart + mGapLength) {
            if (flag == POINT) {
                // A POINT located inside the replaced range should be moved to the end of the
                // replaced text.
                // The exception is when the point is at the start of the range and we are doing a
                // text replacement (as opposed to a deletion): the point stays there.
                if (textIsRemoved || offset > start) {
                    return mGapStart + mGapLength;
                }
            } else {
                if (flag == PARAGRAPH) {
                    if (atEnd) {
                        return mGapStart + mGapLength;
                    }
                } else { // MARK
                    // MARKs should be moved to the start, with the exception of a mark located at
                    // the end of the range (which will be < mGapStart + mGapLength since mGapLength
                    // is > 0, which should stay 'unchanged' at the end of the replaced text.
                    if (textIsRemoved || offset < mGapStart - nbNewChars) {
                        return start;
                    } else {
                        // Move to the end of replaced text (needed if nbNewChars != 0)
                        return mGapStart;
                    }
                }
            }
        }
        return offset;
    }

    // Note: caller is responsible for removing the mIndexOfSpan entry.
    private void removeSpan(int i, int flags) {
        Object object = mSpans[i];

        int start = mSpanStarts[i];
        int end = mSpanEnds[i];

        if (start > mGapStart) start -= mGapLength;
        if (end > mGapStart) end -= mGapLength;

        int count = mSpanCount - (i + 1);
        System.arraycopy(mSpans, i + 1, mSpans, i, count);
        System.arraycopy(mSpanStarts, i + 1, mSpanStarts, i, count);
        System.arraycopy(mSpanEnds, i + 1, mSpanEnds, i, count);
        System.arraycopy(mSpanFlags, i + 1, mSpanFlags, i, count);
        System.arraycopy(mSpanOrder, i + 1, mSpanOrder, i, count);

        mSpanCount--;

        invalidateIndex(i);
        mSpans[mSpanCount] = null;

        // Invariants must be restored before sending span removed notifications.
        restoreInvariants();

        if ((flags & Spanned.SPAN_INTERMEDIATE) == 0) {
            sendSpanRemoved(object, start, end);
        }
    }

    /**
     * Convenience for replace(st, en, text, 0, text.length())
     *
     * @see #replace(int, int, CharSequence, int, int)
     */
    @Override
    public SpannableStringBuilder replace(int start, int end, @NonNull CharSequence tb) {
        return replace(start, end, tb, 0, tb.length());
    }

    /**
     * Replaces the specified range (<code>start&hellip;end</code>) of text in this
     * Editable with a copy of the slice <code>tbstart&hellip;tbend</code> from
     * <code>tb</code>.  The destination slice may be empty, in which case
     * the operation is an insertion, or the source slice may be empty,
     * in which case the operation is a deletion.
     * <p>
     * Before the change is committed, each filter that was set with
     * {@link #setFilters} is given the opportunity to modify the
     * <code>source</code> text.
     * <p>
     * If <code>source</code>
     * is Spanned, the spans from it are preserved into the Editable.
     * Existing spans within the Editable that entirely cover the replaced
     * range are retained, but any that were strictly within the range
     * that was replaced are removed. If the <code>source</code> contains a span
     * with {@link Spanned#SPAN_PARAGRAPH} flag, and it does not satisfy the
     * paragraph boundary constraint, it is not retained. As a special case, the
     * cursor position is preserved even when the entire range where it is located
     * is replaced.
     *
     * @return a reference to this object.
     * @see Spanned#SPAN_PARAGRAPH
     */
    @Override
    public SpannableStringBuilder replace(final int start, final int end,
                                          @NonNull CharSequence tb, int tbstart, int tbend) {
        checkRange("replace", start, end);

        for (InputFilter filter : mFilters) {
            CharSequence repl = filter.filter(tb, tbstart, tbend, this, start, end);

            if (repl != null) {
                tb = repl;
                tbstart = 0;
                tbend = repl.length();
            }
        }

        final int origLen = end - start;
        final int newLen = tbend - tbstart;

        if (origLen == 0 && newLen == 0 && !hasNonExclusiveExclusiveSpanAt(tb, tbstart)) {
            // This is a no-op iif there are no spans in tb that would be added (with a 0-length)
            // Early exit so that the text watchers do not get notified
            return this;
        }

        List<TextWatcher> textWatchers = getSpans(start, end, TextWatcher.class);
        if (!textWatchers.isEmpty()) {
            sendBeforeTextChanged(textWatchers, start, origLen, newLen);
        }

        // Try to keep the cursor / selection at the same relative position during
        // a text replacement. If replaced or replacement text length is zero, this
        // is already taken care of.
        if (origLen != 0 && newLen != 0) {
            int selectionStart = Selection.getSelectionStart(this);
            int selectionEnd = Selection.getSelectionEnd(this);

            change(start, end, tb, tbstart, tbend);

            boolean changed = false;
            if (selectionStart > start && selectionStart < end) {
                final long diff = selectionStart - start;
                final int offset = Math.toIntExact(diff * newLen / origLen);
                selectionStart = start + offset;

                changed = true;
                setSpan(false, Selection.SELECTION_START, selectionStart, selectionStart,
                        Spanned.SPAN_POINT_POINT, true/*enforceParagraph*/);
            }
            if (selectionEnd > start && selectionEnd < end) {
                final long diff = selectionEnd - start;
                final int offset = Math.toIntExact(diff * newLen / origLen);
                selectionEnd = start + offset;

                changed = true;
                setSpan(false, Selection.SELECTION_END, selectionEnd, selectionEnd,
                        Spanned.SPAN_POINT_POINT, true/*enforceParagraph*/);
            }
            if (changed) {
                restoreInvariants();
            }
        } else {
            change(start, end, tb, tbstart, tbend);
        }

        if (!textWatchers.isEmpty()) {
            sendTextChanged(textWatchers, start, origLen, newLen);
            sendAfterTextChanged(textWatchers);
        }

        // Span watchers need to be called after text watchers, which may update the layout
        sendToSpanWatchers(start, end, newLen - origLen);

        return this;
    }

    private static boolean hasNonExclusiveExclusiveSpanAt(CharSequence text, int offset) {
        if (text instanceof Spanned spanned) {
            List<?> spans = spanned.getSpans(offset, offset, Object.class);
            for (int i = 0; i < spans.size(); i++) {
                Object span = spans.get(i);
                int flags = spanned.getSpanFlags(span);
                if (flags != Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) return true;
            }
        }
        return false;
    }

    private void sendToSpanWatchers(int replaceStart, int replaceEnd, int nbNewChars) {
        for (int i = 0; i < mSpanCount; i++) {
            int spanFlags = mSpanFlags[i];

            // This loop handles only modified (not added) spans.
            if ((spanFlags & SPAN_ADDED) != 0) continue;
            int spanStart = mSpanStarts[i];
            int spanEnd = mSpanEnds[i];
            if (spanStart > mGapStart) spanStart -= mGapLength;
            if (spanEnd > mGapStart) spanEnd -= mGapLength;

            int newReplaceEnd = replaceEnd + nbNewChars;
            boolean spanChanged = false;

            int previousSpanStart = spanStart;
            if (spanStart > newReplaceEnd) {
                if (nbNewChars != 0) {
                    previousSpanStart -= nbNewChars;
                    spanChanged = true;
                }
            } else if (spanStart >= replaceStart) {
                // No change if span start was already at replace interval boundaries before replace
                if ((spanStart != replaceStart ||
                        ((spanFlags & SPAN_START_AT_START) != SPAN_START_AT_START)) &&
                        (spanStart != newReplaceEnd ||
                                ((spanFlags & SPAN_START_AT_END) != SPAN_START_AT_END))) {
                    // TODO A correct previousSpanStart cannot be computed at this point.
                    // It would require to save all the previous spans' positions before the replace
                    // Using an invalid -1 value to convey this would break the broacast range
                    spanChanged = true;
                }
            }

            int previousSpanEnd = spanEnd;
            if (spanEnd > newReplaceEnd) {
                if (nbNewChars != 0) {
                    previousSpanEnd -= nbNewChars;
                    spanChanged = true;
                }
            } else if (spanEnd >= replaceStart) {
                // No change if span start was already at replace interval boundaries before replace
                if ((spanEnd != replaceStart ||
                        ((spanFlags & SPAN_END_AT_START) != SPAN_END_AT_START)) &&
                        (spanEnd != newReplaceEnd ||
                                ((spanFlags & SPAN_END_AT_END) != SPAN_END_AT_END))) {
                    // TODO same as above for previousSpanEnd
                    spanChanged = true;
                }
            }

            if (spanChanged) {
                sendSpanChanged(mSpans[i], previousSpanStart, previousSpanEnd, spanStart, spanEnd);
            }
            mSpanFlags[i] &= ~SPAN_START_END_MASK;
        }

        // Handle added spans
        for (int i = 0; i < mSpanCount; i++) {
            int spanFlags = mSpanFlags[i];
            if ((spanFlags & SPAN_ADDED) != 0) {
                mSpanFlags[i] &= ~SPAN_ADDED;
                int spanStart = mSpanStarts[i];
                int spanEnd = mSpanEnds[i];
                if (spanStart > mGapStart) spanStart -= mGapLength;
                if (spanEnd > mGapStart) spanEnd -= mGapLength;
                sendSpanAdded(mSpans[i], spanStart, spanEnd);
            }
        }
    }

    /**
     * Mark the specified range of text with the specified object.
     * The flags determine how the span will behave when text is
     * inserted at the start or end of the span's range.
     */
    @Override
    public void setSpan(@NonNull Object what, int start, int end, int flags) {
        setSpan(true, what, start, end, flags, true/*enforceParagraph*/);
    }

    // Note: if send is false, then it is the caller's responsibility to restore
    // invariants. If send is false and the span already exists, then this method
    // will not change the index of any spans.
    private void setSpan(boolean send, @NonNull Object what, int start, int end, int flags,
                         boolean enforceParagraph) {
        checkRange("setSpan", start, end);

        int flagsStart = (flags & START_MASK) >> START_SHIFT;
        if (isInvalidParagraph(start, flagsStart)) {
            if (!enforceParagraph) {
                // do not set the span
                return;
            }
            throw new RuntimeException("PARAGRAPH span must start at paragraph boundary"
                    + " (" + start + " follows " + charAt(start - 1) + ")");
        }

        int flagsEnd = flags & END_MASK;
        if (isInvalidParagraph(end, flagsEnd)) {
            if (!enforceParagraph) {
                // do not set the span
                return;
            }
            throw new RuntimeException("PARAGRAPH span must end at paragraph boundary"
                    + " (" + end + " follows " + charAt(end - 1) + ")");
        }

        // 0-length Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        if (flagsStart == POINT && flagsEnd == MARK && start == end) {
            if (send) {
                Log.LOGGER.error(MARKER, "SPAN_EXCLUSIVE_EXCLUSIVE spans cannot have a zero length");
            }
            // Silently ignore invalid spans when they are created from this class.
            // This avoids the duplication of the above test code before all the
            // calls to setSpan that are done in this class
            return;
        }

        int nstart = start;
        int nend = end;

        if (start > mGapStart) {
            start += mGapLength;
        } else if (start == mGapStart) {
            if (flagsStart == POINT || (flagsStart == PARAGRAPH && start == length()))
                start += mGapLength;
        }

        if (end > mGapStart) {
            end += mGapLength;
        } else if (end == mGapStart) {
            if (flagsEnd == POINT || (flagsEnd == PARAGRAPH && end == length()))
                end += mGapLength;
        }

        if (mIndexOfSpan != null) {
            Integer index = mIndexOfSpan.get(what);
            if (index != null) {
                int i = index;
                int ostart = mSpanStarts[i];
                int oend = mSpanEnds[i];

                if (ostart > mGapStart)
                    ostart -= mGapLength;
                if (oend > mGapStart)
                    oend -= mGapLength;

                mSpanStarts[i] = start;
                mSpanEnds[i] = end;
                mSpanFlags[i] = flags;

                if (send) {
                    restoreInvariants();
                    sendSpanChanged(what, ostart, oend, nstart, nend);
                }

                return;
            }
        }

        mSpans = GrowingArrayUtils.append(mSpans, mSpanCount, what);
        mSpanStarts = GrowingArrayUtils.append(mSpanStarts, mSpanCount, start);
        mSpanEnds = GrowingArrayUtils.append(mSpanEnds, mSpanCount, end);
        mSpanFlags = GrowingArrayUtils.append(mSpanFlags, mSpanCount, flags);
        mSpanOrder = GrowingArrayUtils.append(mSpanOrder, mSpanCount, mSpanInsertCount);
        invalidateIndex(mSpanCount);
        mSpanCount++;
        mSpanInsertCount++;
        // Make sure there is enough room for empty interior nodes.
        // This magic formula computes the size of the smallest perfect binary
        // tree no smaller than mSpanCount.
        int sizeOfMax = 2 * treeRoot() + 1;
        if (mSpanMax.length < sizeOfMax) {
            mSpanMax = new int[sizeOfMax];
        }

        if (send) {
            restoreInvariants();
            sendSpanAdded(what, nstart, nend);
        }
    }

    private boolean isInvalidParagraph(int index, int flag) {
        return flag == PARAGRAPH && index != 0 && index != length() && charAt(index - 1) != '\n';
    }

    /**
     * Remove the specified markup object from the buffer.
     */
    @Override
    public void removeSpan(@NonNull Object what) {
        removeSpan(what, 0 /* flags */);
    }

    /**
     * Remove the specified markup object from the buffer.
     */
    @Override
    public void removeSpan(@NonNull Object what, int flags) {
        if (mIndexOfSpan == null)
            return;
        Integer i = mIndexOfSpan.remove(what);
        if (i != null) {
            removeSpan(i.intValue(), flags);
        }
    }

    /**
     * Return externally visible offset given offset into gapped buffer.
     */
    private int resolveGap(int i) {
        return i > mGapStart ? i - mGapLength : i;
    }

    /**
     * Return the buffer offset of the beginning of the specified
     * markup object, or -1 if it is not attached to this buffer.
     */
    @Override
    public int getSpanStart(@NonNull Object what) {
        if (mIndexOfSpan == null)
            return -1;
        Integer i = mIndexOfSpan.get(what);
        return i == null ? -1 : resolveGap(mSpanStarts[i]);
    }

    /**
     * Return the buffer offset of the end of the specified
     * markup object, or -1 if it is not attached to this buffer.
     */
    @Override
    public int getSpanEnd(@NonNull Object what) {
        if (mIndexOfSpan == null)
            return -1;
        Integer i = mIndexOfSpan.get(what);
        return i == null ? -1 : resolveGap(mSpanEnds[i]);
    }

    /**
     * Return the flags of the end of the specified
     * markup object, or 0 if it is not attached to this buffer.
     */
    @Override
    public int getSpanFlags(@NonNull Object what) {
        if (mIndexOfSpan == null)
            return 0;
        Integer i = mIndexOfSpan.get(what);
        return i == null ? 0 : mSpanFlags[i];
    }

    /**
     * Return an array of the spans of the specified type that overlap
     * the specified range of the buffer.  The kind may be Object.class to get
     * a list of all the spans regardless of type.
     */
    @NonNull
    @Override
    public <T> List<T> getSpans(int queryStart, int queryEnd, @Nullable Class<? extends T> kind,
                                @Nullable List<T> dest) {
        return getSpans(queryStart, queryEnd, kind, true, dest);
    }

    /**
     * Return an array of the spans of the specified type that overlap
     * the specified range of the buffer.  The kind may be Object.class to get
     * a list of all the spans regardless of type.
     *
     * @param queryStart           Start index.
     * @param queryEnd             End index.
     * @param kind                 Class type to search for.
     * @param sortByInsertionOrder If true the results are sorted by the insertion order.
     * @param <T>                  Markup type.
     * @return List of the spans.
     * @hidden
     */
    @ApiStatus.Internal
    @SuppressWarnings({"unchecked"})
    @NonNull
    public <T> List<T> getSpans(int queryStart, int queryEnd, @Nullable Class<? extends T> kind,
                                boolean sortByInsertionOrder, @Nullable List<T> dest) {
        if (dest != null) {
            dest.clear();
        }
        if (mSpanCount == 0) {
            return dest != null ? dest : Collections.emptyList();
        }
        if (kind == null) {
            kind = (Class<? extends T>) Object.class;
        }
        if (dest == null) {
            dest = new ArrayList<>();
        }
        final IntArrayList prioSortBuffer = sortByInsertionOrder ? obtain() : null;
        final IntArrayList orderSortBuffer = sortByInsertionOrder ? obtain() : null;
        int count = getSpansRec(queryStart, queryEnd, kind, treeRoot(), (List<Object>) dest, prioSortBuffer,
                orderSortBuffer, 0, sortByInsertionOrder);
        assert count == dest.size();
        if (sortByInsertionOrder) {
            if (count != 0) {
                sort(dest, prioSortBuffer.elements(), orderSortBuffer.elements());
            }
            recycle(prioSortBuffer);
            recycle(orderSortBuffer);
        }
        return dest;
    }

    private int countSpans(int queryStart, int queryEnd, @NonNull Class<?> kind, int i) {
        int count = 0;
        if ((i & 1) != 0) {
            // internal tree node
            int left = leftChild(i);
            int spanMax = mSpanMax[left];
            if (spanMax > mGapStart) {
                spanMax -= mGapLength;
            }
            if (spanMax >= queryStart) {
                count = countSpans(queryStart, queryEnd, kind, left);
            }
        }
        if (i < mSpanCount) {
            int spanStart = mSpanStarts[i];
            if (spanStart > mGapStart) {
                spanStart -= mGapLength;
            }
            if (spanStart <= queryEnd) {
                int spanEnd = mSpanEnds[i];
                if (spanEnd > mGapStart) {
                    spanEnd -= mGapLength;
                }
                if (spanEnd >= queryStart &&
                        (spanStart == spanEnd || queryStart == queryEnd ||
                                (spanStart != queryEnd && spanEnd != queryStart)) &&
                        (Object.class == kind || kind.isInstance(mSpans[i]))) {
                    count++;
                }
                if ((i & 1) != 0) {
                    count += countSpans(queryStart, queryEnd, kind, rightChild(i));
                }
            }
        }
        return count;
    }

    /**
     * Fills the result array with the spans found under the current interval tree node.
     *
     * @param queryStart     Start index for the interval query.
     * @param queryEnd       End index for the interval query.
     * @param kind           Class type to search for.
     * @param i              Index of the current tree node.
     * @param ret            List to be filled with results.
     * @param priority       Buffer to keep record of the priorities of spans found.
     * @param insertionOrder Buffer to keep record of the insertion orders of spans found.
     * @param count          The number of found spans.
     * @param sort           Flag to fill the priority and insertion order buffers. If false then
     *                       the spans with priority flag will be sorted in the result array.
     * @return The total number of spans found.
     */
    private int getSpansRec(int queryStart, int queryEnd, @NonNull Class<?> kind, int i,
                            @NonNull List<Object> ret, IntArrayList priority, IntArrayList insertionOrder,
                            int count, boolean sort) {
        if ((i & 1) != 0) {
            // internal tree node
            int left = leftChild(i);
            int spanMax = mSpanMax[left];
            if (spanMax > mGapStart) {
                spanMax -= mGapLength;
            }
            if (spanMax >= queryStart) {
                count = getSpansRec(queryStart, queryEnd, kind, left, ret, priority,
                        insertionOrder, count, sort);
            }
        }
        if (i >= mSpanCount) return count;
        int spanStart = mSpanStarts[i];
        if (spanStart > mGapStart) {
            spanStart -= mGapLength;
        }
        if (spanStart <= queryEnd) {
            int spanEnd = mSpanEnds[i];
            if (spanEnd > mGapStart) {
                spanEnd -= mGapLength;
            }
            if (spanEnd >= queryStart &&
                    (spanStart == spanEnd || queryStart == queryEnd ||
                            (spanStart != queryEnd && spanEnd != queryStart)) &&
                    (Object.class == kind || kind.isInstance(mSpans[i]))) {
                int spanPriority = mSpanFlags[i] & SPAN_PRIORITY;
                if (sort) {
                    priority.add(spanPriority);
                    insertionOrder.add(mSpanOrder[i]);
                    ret.add(mSpans[i]);
                } else if (spanPriority != 0) {
                    //insertion sort for elements with priority
                    int j = 0;
                    for (; j < count; j++) {
                        int p = getSpanFlags(ret.get(j)) & SPAN_PRIORITY;
                        if (spanPriority > p) break;
                    }
                    ret.add(j, mSpans[i]);
                } else {
                    ret.add(mSpans[i]);
                }
                count++;
            }
            if ((i & 1) != 0) {
                count = getSpansRec(queryStart, queryEnd, kind, rightChild(i), ret, priority,
                        insertionOrder, count, sort);
            }
        }
        return count;
    }

    /**
     * Obtain a temporary sort buffer.
     *
     * @return an int[] with elementCount length
     */
    @NonNull
    private static IntArrayList obtain() {
        IntArrayList result = sIntBufferPool.acquire();
        if (result == null) {
            result = new IntArrayList();
        }
        return result;
    }

    /**
     * Recycle sort buffer.
     *
     * @param buffer buffer to be recycled
     */
    private static void recycle(@NonNull IntArrayList buffer) {
        buffer.clear();
        sIntBufferPool.release(buffer);
    }

    /**
     * An iterative heap sort implementation. It will sort the spans using first their priority
     * then insertion order. A span with higher priority will be before a span with lower
     * priority. If priorities are the same, the spans will be sorted with insertion order. A
     * span with a lower insertion order will be before a span with a higher insertion order.
     *
     * @param list           Span list to be sorted.
     * @param priority       Priorities of the spans
     * @param insertionOrder Insertion orders of the spans
     * @param <T>            Span object type.
     */
    private static <T> void sort(@NonNull List<T> list, int[] priority, int[] insertionOrder) {
        int size = list.size();
        for (int i = size / 2 - 1; i >= 0; i--) {
            siftDown(i, list, size, priority, insertionOrder);
        }

        for (int i = size - 1; i > 0; i--) {
            list.set(i, list.set(0, list.get(i)));

            final int tmpPriority = priority[0];
            priority[0] = priority[i];
            priority[i] = tmpPriority;

            final int tmpOrder = insertionOrder[0];
            insertionOrder[0] = insertionOrder[i];
            insertionOrder[i] = tmpOrder;

            siftDown(0, list, i, priority, insertionOrder);
        }
    }

    /**
     * Helper function for heap sort.
     *
     * @param index          Index of the element to sift down.
     * @param list           Span list to be sorted.
     * @param size           Current heap size.
     * @param priority       Priorities of the spans
     * @param insertionOrder Insertion orders of the spans
     * @param <T>            Span object type.
     */
    private static <T> void siftDown(int index, @NonNull List<T> list, int size, @NonNull int[] priority,
                                     @NonNull int[] insertionOrder) {
        int left = 2 * index + 1;
        while (left < size) {
            if (left < size - 1 && compareSpans(left, left + 1, priority, insertionOrder) < 0) {
                left++;
            }
            if (compareSpans(index, left, priority, insertionOrder) >= 0) {
                break;
            }

            list.set(left, list.set(index, list.get(left)));

            final int tmpPriority = priority[index];
            priority[index] = priority[left];
            priority[left] = tmpPriority;

            final int tmpOrder = insertionOrder[index];
            insertionOrder[index] = insertionOrder[left];
            insertionOrder[left] = tmpOrder;

            index = left;
            left = 2 * index + 1;
        }
    }

    /**
     * Compare two span elements in an array. Comparison is based first on the priority flag of
     * the span, and then the insertion order of the span.
     *
     * @param left           Index of the element to compare.
     * @param right          Index of the other element to compare.
     * @param priority       Priorities of the spans
     * @param insertionOrder Insertion orders of the spans
     * @return comparing result
     */
    private static int compareSpans(int left, int right, @NonNull int[] priority,
                                    @NonNull int[] insertionOrder) {
        int priority1 = priority[left];
        int priority2 = priority[right];
        if (priority1 == priority2) {
            return Integer.compare(insertionOrder[left], insertionOrder[right]);
        }
        // since high priority has to be before a lower priority, the arguments to compare are
        // opposite of the insertion order check.
        return Integer.compare(priority2, priority1);
    }

    /**
     * Return the next offset after <code>start</code> but less than or
     * equal to <code>limit</code> where a span of the specified type
     * begins or ends.
     */
    @Override
    public int nextSpanTransition(int start, int limit, @Nullable Class<?> kind) {
        if (mSpanCount == 0) return limit;
        if (kind == null) {
            kind = Object.class;
        }
        return nextSpanTransitionRec(start, limit, kind, treeRoot());
    }

    private int nextSpanTransitionRec(int start, int limit, @NonNull Class<?> kind, int i) {
        if ((i & 1) != 0) {
            // internal tree node
            int left = leftChild(i);
            if (resolveGap(mSpanMax[left]) > start) {
                limit = nextSpanTransitionRec(start, limit, kind, left);
            }
        }
        if (i < mSpanCount) {
            int st = resolveGap(mSpanStarts[i]);
            int en = resolveGap(mSpanEnds[i]);
            if (st > start && st < limit && (kind == Object.class || kind.isInstance(mSpans[i])))
                limit = st;
            if (en > start && en < limit && (kind == Object.class || kind.isInstance(mSpans[i])))
                limit = en;
            if (st < limit && (i & 1) != 0) {
                limit = nextSpanTransitionRec(start, limit, kind, rightChild(i));
            }
        }

        return limit;
    }

    /**
     * Return a new CharSequence containing a copy of the specified
     * range of this buffer, including the overlapping spans.
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return new SpannableStringBuilder(this, start, end);
    }

    /**
     * Copy the specified range of chars from this buffer into the
     * specified array, beginning at the specified offset.
     */
    @Override
    public void getChars(int start, int end, char[] dest, int destoff) {
        checkRange("getChars", start, end);

        if (end <= mGapStart) {
            System.arraycopy(mText, start, dest, destoff, end - start);
        } else if (start >= mGapStart) {
            System.arraycopy(mText, start + mGapLength, dest, destoff, end - start);
        } else {
            System.arraycopy(mText, start, dest, destoff, mGapStart - start);
            System.arraycopy(mText, mGapStart + mGapLength,
                    dest, destoff + (mGapStart - start),
                    end - mGapStart);
        }
    }

    /**
     * Return a String containing a copy of the chars in this buffer.
     */
    @Override
    public String toString() {
        int len = length();
        char[] buf = new char[len];

        getChars(0, len, buf, 0);
        return new String(buf);
    }

    /**
     * Return a String containing a copy of the chars in this buffer, limited to the
     * [start, end) range.
     */
    public String substring(int start, int end) {
        char[] buf = new char[end - start];
        getChars(start, end, buf, 0);
        return new String(buf);
    }

    /**
     * Returns the depth of TextWatcher callbacks. Returns 0 when the object is not handling
     * TextWatchers. A return value greater than 1 implies that a TextWatcher caused a change that
     * recursively triggered a TextWatcher.
     */
    public int getTextWatcherDepth() {
        return mTextWatcherDepth;
    }

    private void sendBeforeTextChanged(@NonNull List<TextWatcher> watchers, int start, int before, int after) {
        mTextWatcherDepth++;
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).beforeTextChanged(this, start, before, after);
        }
        mTextWatcherDepth--;
    }

    private void sendTextChanged(@NonNull List<TextWatcher> watchers, int start, int before, int after) {
        mTextWatcherDepth++;
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).onTextChanged(this, start, before, after);
        }
        mTextWatcherDepth--;
    }

    private void sendAfterTextChanged(@NonNull List<TextWatcher> watchers) {
        mTextWatcherDepth++;
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).afterTextChanged(this);
        }
        mTextWatcherDepth--;
    }

    private void sendSpanAdded(Object what, int start, int end) {
        List<SpanWatcher> watchers = getSpans(start, end, SpanWatcher.class);
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).onSpanAdded(this, what, start, end);
        }
    }

    private void sendSpanRemoved(Object what, int start, int end) {
        List<SpanWatcher> watchers = getSpans(start, end, SpanWatcher.class);
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).onSpanRemoved(this, what, start, end);
        }
    }

    private void sendSpanChanged(Object what, int oldStart, int oldEnd, int start, int end) {
        // The bounds of a possible SpanWatcher are guaranteed to be set before this method is
        // called, so that the order of the span does not affect this broadcast.
        List<SpanWatcher> watchers = getSpans(Math.min(oldStart, start),
                Math.min(Math.max(oldEnd, end), length()), SpanWatcher.class);
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).onSpanChanged(this, what, oldStart, oldEnd, start, end);
        }
    }

    @NonNull
    private static String region(int start, int end) {
        return "(" + start + " ... " + end + ")";
    }

    private void checkRange(final String operation, int start, int end) {
        if (end < start) {
            throw new IndexOutOfBoundsException(operation + " " +
                    region(start, end) + " has end before start");
        }

        int len = length();

        if (start > len || end > len) {
            throw new IndexOutOfBoundsException(operation + " " +
                    region(start, end) + " ends beyond length " + len);
        }

        if (start < 0 || end < 0) {
            throw new IndexOutOfBoundsException(operation + " " +
                    region(start, end) + " starts before 0");
        }
    }

    /*
    private boolean isprint(char c) { // XXX
        if (c >= ' ' && c <= '~')
            return true;
        else
            return false;
    }

    private static final int startFlag(int flag) {
        return (flag >> 4) & 0x0F;
    }

    private static final int endFlag(int flag) {
        return flag & 0x0F;
    }

    public void dump() { // XXX
        for (int i = 0; i < mGapStart; i++) {
            System.out.print('|');
            System.out.print(' ');
            System.out.print(isprint(mText[i]) ? mText[i] : '.');
            System.out.print(' ');
        }

        for (int i = mGapStart; i < mGapStart + mGapLength; i++) {
            System.out.print('|');
            System.out.print('(');
            System.out.print(isprint(mText[i]) ? mText[i] : '.');
            System.out.print(')');
        }

        for (int i = mGapStart + mGapLength; i < mText.length; i++) {
            System.out.print('|');
            System.out.print(' ');
            System.out.print(isprint(mText[i]) ? mText[i] : '.');
            System.out.print(' ');
        }

        System.out.print('\n');

        for (int i = 0; i < mText.length + 1; i++) {
            int found = 0;
            int wfound = 0;

            for (int j = 0; j < mSpanCount; j++) {
                if (mSpanStarts[j] == i) {
                    found = 1;
                    wfound = j;
                    break;
                }

                if (mSpanEnds[j] == i) {
                    found = 2;
                    wfound = j;
                    break;
                }
            }

            if (found == 1) {
                if (startFlag(mSpanFlags[wfound]) == MARK)
                    System.out.print("(   ");
                if (startFlag(mSpanFlags[wfound]) == PARAGRAPH)
                    System.out.print("<   ");
                else
                    System.out.print("[   ");
            } else if (found == 2) {
                if (endFlag(mSpanFlags[wfound]) == POINT)
                    System.out.print(")   ");
                if (endFlag(mSpanFlags[wfound]) == PARAGRAPH)
                    System.out.print(">   ");
                else
                    System.out.print("]   ");
            } else {
                System.out.print("    ");
            }
        }

        System.out.print("\n");
    }
    */

    /**
     * Sets the series of filters that will be called in succession
     * whenever the text of this Editable is changed, each of which has
     * the opportunity to limit or transform the text that is being inserted.
     */
    @Override
    public void setFilters(@NonNull InputFilter[] filters) {
        mFilters = filters;
    }

    /**
     * Returns the array of input filters that are currently applied
     * to changes to this Editable.
     */
    @NonNull
    @Override
    public InputFilter[] getFilters() {
        return mFilters;
    }

    // Same as SpannableStringInternal
    @Override
    public boolean equals(Object o) {
        if (o instanceof final Spanned other &&
                toString().equals(o.toString())) {
            // Check span data
            final List<?> otherSpans = other.getSpans(0, other.length(), Object.class);
            final List<?> spans = getSpans(0, length(), Object.class);
            if (otherSpans.isEmpty() && spans.isEmpty()) {
                return true;
            } else if (!otherSpans.isEmpty() && !spans.isEmpty() &&
                    otherSpans.size() == spans.size()) {
                // Do not check mSpanCount anymore for safety
                for (int i = 0; i < spans.size(); ++i) {
                    final Object span = spans.get(i);
                    final Object otherSpan = otherSpans.get(i);
                    if (span == this) {
                        if (other != otherSpan ||
                                getSpanStart(span) != other.getSpanStart(otherSpan) ||
                                getSpanEnd(span) != other.getSpanEnd(otherSpan) ||
                                getSpanFlags(span) != other.getSpanFlags(otherSpan)) {
                            return false;
                        }
                    } else if (!span.equals(otherSpan) ||
                            getSpanStart(span) != other.getSpanStart(otherSpan) ||
                            getSpanEnd(span) != other.getSpanEnd(otherSpan) ||
                            getSpanFlags(span) != other.getSpanFlags(otherSpan)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    // Same as SpannableStringInternal
    @Override
    public int hashCode() {
        int hash = toString().hashCode();
        hash = hash * 31 + mSpanCount;
        for (int i = 0; i < mSpanCount; ++i) {
            Object span = mSpans[i];
            if (span != this) {
                hash = hash * 31 + span.hashCode();
            }
            hash = hash * 31 + getSpanStart(span);
            hash = hash * 31 + getSpanEnd(span);
            hash = hash * 31 + getSpanFlags(span);
        }
        return hash;
    }

    // Primitives for treating span list as binary tree

    // The spans (along with start and end offsets and flags) are stored in linear arrays sorted
    // by start offset. For fast searching, there is a binary search structure imposed over these
    // arrays. This structure is inorder traversal of a perfect binary tree, a slightly unusual
    // but advantageous approach.

    // The value-containing nodes are indexed 0 <= i < n (where n = mSpanCount), thus preserving
    // logic that accesses the values as a contiguous array. Other balanced binary tree approaches
    // (such as a complete binary tree) would require some shuffling of node indices.

    // Basic properties of this structure: For a perfect binary tree of height m:
    // The tree has 2^(m+1) - 1 total nodes.
    // The root of the tree has index 2^m - 1.
    // All leaf nodes have even index, all interior nodes odd.
    // The height of a node of index i is the number of trailing ones in i's binary representation.
    // The left child of a node i of height h is i - 2^(h - 1).
    // The right child of a node i of height h is i + 2^(h - 1).

    // Note that for arbitrary n, interior nodes of this tree may be >= n. Thus, the general
    // structure of a recursive traversal of node i is:
    // * traverse left child if i is an interior node
    // * process i if i < n
    // * traverse right child if i is an interior node and i < n

    private int treeRoot() {
        return Integer.highestOneBit(mSpanCount) - 1;
    }

    // (i+1) & ~i is equal to 2^(the number of trailing ones in i)
    private static int leftChild(int i) {
        return i - (((i + 1) & ~i) >> 1);
    }

    private static int rightChild(int i) {
        return i + (((i + 1) & ~i) >> 1);
    }

    // The span arrays are also augmented by an mSpanMax[] array that represents an interval tree
    // over the binary tree structure described above. For each node, the mSpanMax[] array contains
    // the maximum value of mSpanEnds of that node and its descendants. Thus, traversals can
    // easily reject subtrees that contain no spans overlapping the area of interest.

    // Note that mSpanMax[] also has a valid valuefor interior nodes of index >= n, but which have
    // descendants of index < n. In these cases, it simply represents the maximum span end of its
    // descendants. This is a consequence of the perfect binary tree structure.
    private int calcMax(int i) {
        int max = 0;
        if ((i & 1) != 0) {
            // internal tree node
            max = calcMax(leftChild(i));
        }
        if (i < mSpanCount) {
            max = Math.max(max, mSpanEnds[i]);
            if ((i & 1) != 0) {
                max = Math.max(max, calcMax(rightChild(i)));
            }
        }
        mSpanMax[i] = max;
        return max;
    }

    // restores binary interval tree invariants after any mutation of span structure
    private void restoreInvariants() {
        if (mSpanCount == 0) return;

        // invariant 1: span starts are nondecreasing

        // This is a simple insertion sort because we expect it to be mostly sorted.
        for (int i = 1; i < mSpanCount; i++) {
            if (mSpanStarts[i] < mSpanStarts[i - 1]) {
                Object span = mSpans[i];
                int start = mSpanStarts[i];
                int end = mSpanEnds[i];
                int flags = mSpanFlags[i];
                int insertionOrder = mSpanOrder[i];
                int j = i;
                do {
                    mSpans[j] = mSpans[j - 1];
                    mSpanStarts[j] = mSpanStarts[j - 1];
                    mSpanEnds[j] = mSpanEnds[j - 1];
                    mSpanFlags[j] = mSpanFlags[j - 1];
                    mSpanOrder[j] = mSpanOrder[j - 1];
                    j--;
                } while (j > 0 && start < mSpanStarts[j - 1]);
                mSpans[j] = span;
                mSpanStarts[j] = start;
                mSpanEnds[j] = end;
                mSpanFlags[j] = flags;
                mSpanOrder[j] = insertionOrder;
                invalidateIndex(j);
            }
        }

        // invariant 2: max is max span end for each node and its descendants
        calcMax(treeRoot());

        // invariant 3: mIndexOfSpan maps spans back to indices
        if (mIndexOfSpan == null) {
            mIndexOfSpan = new IdentityHashMap<>();
        }
        for (int i = mLowWaterMark; i < mSpanCount; i++) {
            Integer existing = mIndexOfSpan.get(mSpans[i]);
            if (existing == null || existing != i) {
                mIndexOfSpan.put(mSpans[i], i);
            }
        }
        mLowWaterMark = Integer.MAX_VALUE;
    }

    // Call this on any update to mSpans[], so that mIndexOfSpan can be updated
    private void invalidateIndex(int i) {
        mLowWaterMark = Math.min(i, mLowWaterMark);
    }
}
