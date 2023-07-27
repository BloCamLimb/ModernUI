/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.markdown.core;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.Node;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.markdown.*;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.style.*;

public final class CorePlugin implements MarkdownPlugin {

    private static final float[] HEADING_SIZES = {
            2.0f, 1.5f, 1.17f, 1.0f, 0.83f, 0.66f
    };

    @Override
    public void configure(@NonNull MarkdownConfig.Builder builder) {
        builder.addVisitor(Text.class, this::visitText);
        builder.addVisitor(StrongEmphasis.class, this::visitDelimited);
        builder.addVisitor(Emphasis.class, this::visitDelimited);

        builder.addVisitor(SoftLineBreak.class, this::visitSoftLineBreak);
        builder.addVisitor(HardLineBreak.class, this::visitHardLineBreak);

        builder.addVisitor(Heading.class, this::visitHeading);
        builder.addVisitor(Paragraph.class, this::visitParagraph);

        builder.addVisitor(BulletListItem.class, this::visitListItem);
        builder.addVisitor(OrderedListItem.class, this::visitListItem);

        builder.addVisitor(BulletList.class, this::visitSimpleBlock);
        builder.addVisitor(OrderedList.class, this::visitSimpleBlock);

        builder.addVisitor(BlockQuote.class, this::visitBlockQuote);

        builder.appendSpanFactory(StrongEmphasis.class,
                (config, node) -> new StyleSpan(TextPaint.BOLD));
        builder.appendSpanFactory(Emphasis.class,
                (config, node) -> new StyleSpan(TextPaint.ITALIC));
        builder.appendSpanFactory(Heading.class,
                (config, node) -> new RelativeSizeSpan(HEADING_SIZES[node.getLevel() - 1]));
        builder.appendSpanFactory(BulletListItem.class,
                (config, node) -> {
                    int level = listLevel(node);
                    return new BulletSpan(48, 0, 0, level);
                });
        builder.appendSpanFactory(BlockQuote.class,
                (config, node) -> new QuoteSpan(48, 8, 0x40FFFFFF));
    }

    private void visitSimpleBlock(
            @NonNull MarkdownVisitor visitor,
            @NonNull Block block) {
        visitor.blockStart(block);
        int offset = visitor.length();
        var spans = visitor.populateSpans(block, false);
        visitor.visitChildren(block);
        visitor.adjustSpansOffset(spans, offset);
        visitor.blockEnd(block);
    }

    private static int listLevel(@NonNull Node node) {
        int level = 0;
        Node parent = node.getParent();
        while (parent != null) {
            if (parent instanceof ListItem) {
                level += 1;
            }
            parent = parent.getParent();
        }
        return level;
    }

    private void visitText(@NonNull MarkdownVisitor visitor, @NonNull Text text) {
        visitor.append(text.getChars());
    }

    private void visitDelimited(@NonNull MarkdownVisitor visitor, @NonNull Node node) {
        int offset = visitor.length();
        var spans = visitor.populateSpans(node, false);
        visitor.visitChildren(node);
        visitor.adjustSpansOffset(spans, offset);
    }

    private void visitSoftLineBreak(@NonNull MarkdownVisitor visitor, @NonNull SoftLineBreak softLineBreak) {
        visitor.append(" ");
    }

    private void visitHardLineBreak(@NonNull MarkdownVisitor visitor, @NonNull HardLineBreak softLineBreak) {
        visitor.ensureNewLine();
    }

    private void visitHeading(
            @NonNull MarkdownVisitor visitor,
            @NonNull Heading heading) {
        visitor.blockStart(heading);
        int offset = visitor.length();
        var spans = visitor.populateSpans(heading, false);
        visitor.visitChildren(heading);
        visitor.adjustSpansOffset(spans, offset);
        visitor.blockEnd(heading);
    }

    private void visitParagraph(
            @NonNull MarkdownVisitor visitor,
            @NonNull Paragraph paragraph) {
        boolean inTightList = isInTightList(paragraph);

        if (!inTightList) {
            visitor.blockStart(paragraph);
        }

        int offset = visitor.length();
        var spans = visitor.populateSpans(paragraph, false);
        visitor.visitChildren(paragraph);
        visitor.adjustSpansOffset(spans, offset);

        if (!inTightList) {
            visitor.blockEnd(paragraph);
        }
    }

    private void visitBlockQuote(
            @NonNull MarkdownVisitor visitor,
            @NonNull BlockQuote blockQuote) {
        visitor.blockStart(blockQuote);

        int offset = visitor.length();

        var spans = visitor.populateSpans(blockQuote, false);

        visitor.visitChildren(blockQuote);
        visitor.adjustSpansOffset(spans, offset);

        visitor.blockEnd(blockQuote);
    }

    private static boolean isInTightList(@NonNull Paragraph paragraph) {
        final Node parent = paragraph.getParent();
        if (parent != null) {
            if (parent.getParent() instanceof ListBlock list) {
                return list.isTight();
            }
        }
        return false;
    }

    private void visitListItem(
            @NonNull MarkdownVisitor visitor,
            @NonNull ListItem listItem) {
        int offset = visitor.length();
        var spans = visitor.populateSpans(listItem, false);
        visitor.visitChildren(listItem);
        visitor.adjustSpansOffset(spans, offset);

        if (visitor.hasNext(listItem)) {
            visitor.ensureNewLine();
        }
    }
}
