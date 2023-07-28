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
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.markdown.*;
import icyllis.modernui.markdown.core.spans.CodeBlockSpan;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.style.*;
import icyllis.modernui.util.DataSet;

public final class CorePlugin implements Plugin {

    private static final float[] HEADING_SIZES = {
            2.0f, 1.5f, 1.17f, 1.0f, 0.83f, 0.66f
    };

    @Override
    public void configure(@NonNull MarkdownConfig.Builder builder) {
        builder
                .addVisitor(Text.class, this::visitText)
                .addVisitor(StrongEmphasis.class, this::visitDelimited)
                .addVisitor(Emphasis.class, this::visitDelimited)

                .addVisitor(SoftLineBreak.class, this::visitSoftLineBreak)
                .addVisitor(HardLineBreak.class, this::visitHardLineBreak)

                .addVisitor(Heading.class, this::visitHeading)
                .addVisitor(Paragraph.class, this::visitParagraph)

                .addVisitor(BulletListItem.class, this::visitListItem)
                .addVisitor(OrderedListItem.class, this::visitListItem)

                .addVisitor(BulletList.class, this::visitSimpleBlock)
                .addVisitor(OrderedList.class, this::visitSimpleBlock)

                .addVisitor(BlockQuote.class, this::visitBlockQuote)

                .addVisitor(Code.class, this::visitCode)
                .addVisitor(FencedCodeBlock.class, this::visitFencedCodeBlock)
                .addVisitor(IndentedCodeBlock.class, this::visitIndentedCodeBlock);

        builder
                .appendSpanFactory(StrongEmphasis.class,
                        (config, node, props) -> new StyleSpan(TextPaint.BOLD))
                .appendSpanFactory(Emphasis.class,
                        (config, node, props) -> new StyleSpan(TextPaint.ITALIC))
                .appendSpanFactory(Heading.class,
                        (config, node, props) -> new RelativeSizeSpan(HEADING_SIZES[node.getLevel() - 1]))
                .appendSpanFactory(BulletListItem.class,
                        (config, node, props) -> {
                            int level = listLevel(node);
                            return new BulletSpan(48, 0, 0, level);
                        })
                .appendSpanFactory(BlockQuote.class,
                        (config, node, props) -> new QuoteSpan(48, 8, 0x40FFFFFF))
                .appendSpanFactory(Code.class, this::createCodeSpans)
                .appendSpanFactory(FencedCodeBlock.class,
                        (config, node, args) -> new CodeBlockSpan(config.theme()))
                .appendSpanFactory(IndentedCodeBlock.class,
                        (config, node, args) -> new CodeBlockSpan(config.theme()));
    }

    private void visitSimpleBlock(
            @NonNull MarkdownVisitor visitor,
            @NonNull Block block) {
        visitor.blockStart(block);
        int offset = visitor.length();
        var spans = visitor.preSetSpans(block, offset);
        visitor.visitChildren(block);
        visitor.postSetSpans(spans, offset);
        visitor.blockEnd(block);
    }

    private void visitCode(
            @NonNull MarkdownVisitor visitor,
            @NonNull Code code) {
        int offset = visitor.length();
        var spans = visitor.preSetSpans(code, offset);
        visitor
                .append('\u00a0')
                .append(code.getText())
                .append('\u00a0');
        visitor.postSetSpans(spans, offset);
    }

    private void visitFencedCodeBlock(
            @NonNull MarkdownVisitor visitor,
            @NonNull FencedCodeBlock fencedCodeBlock) {
        visitCodeBlock0(
                visitor,
                fencedCodeBlock.getInfo(),
                fencedCodeBlock.getContentChars(),
                fencedCodeBlock
        );
    }

    private void visitIndentedCodeBlock(
            @NonNull MarkdownVisitor visitor,
            @NonNull IndentedCodeBlock indentedCodeBlock) {
        visitCodeBlock0(
                visitor,
                null,
                indentedCodeBlock.getContentChars(),
                indentedCodeBlock
        );
    }

    private void visitCodeBlock0(
            @NonNull MarkdownVisitor visitor,
            @Nullable CharSequence info,
            @NonNull CharSequence code,
            @NonNull Block block) {
        visitor.blockStart(block);

        int offset = visitor.length();
        var spans = visitor.preSetSpans(block, offset);

        visitor.append('\u00a0').append('\n')
                .append(code);

        visitor.ensureNewLine();

        visitor.append('\u00a0');

        visitor.postSetSpans(spans, offset);

        visitor.blockEnd(block);
    }

    @NonNull
    private Object createCodeSpans(
            @NonNull MarkdownConfig config,
            @NonNull Code code,
            @NonNull DataSet args) {
        MarkdownTheme theme = config.theme();
        boolean applyTextColor = theme.getCodeTextColor() != 0;
        boolean applyBackgroundColor = theme.getCodeBackgroundColor() != 0;
        boolean applyTextSize = theme.getCodeTextSize() != 0;
        int extra = 0;
        if (applyTextColor) ++extra;
        if (applyBackgroundColor) ++extra;
        Object[] spans = new Object[extra + 2];
        spans[0] = new TypefaceSpan(theme.getCodeTypeface());
        if (applyTextSize) {
            spans[1] = new AbsoluteSizeSpan(theme.getCodeTextSize());
        } else {
            spans[1] = new RelativeSizeSpan(0.75F);
        }
        if (extra > 0) {
            extra = 2;
            if (applyTextColor) {
                spans[extra++] = new ForegroundColorSpan(theme.getCodeTextColor());
            }
            if (applyBackgroundColor) {
                spans[extra++] = new BackgroundColorSpan(theme.getCodeBackgroundColor());
            }
        }
        return spans;
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
        var spans = visitor.preSetSpans(node, offset);
        visitor.visitChildren(node);
        visitor.postSetSpans(spans, offset);
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
        var spans = visitor.preSetSpans(heading, offset);
        visitor.visitChildren(heading);
        visitor.postSetSpans(spans, offset);
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
        var spans = visitor.preSetSpans(paragraph, offset);
        visitor.visitChildren(paragraph);
        visitor.postSetSpans(spans, offset);

        if (!inTightList) {
            visitor.blockEnd(paragraph);
        }
    }

    private void visitBlockQuote(
            @NonNull MarkdownVisitor visitor,
            @NonNull BlockQuote blockQuote) {
        visitor.blockStart(blockQuote);

        int offset = visitor.length();

        var spans = visitor.preSetSpans(blockQuote, offset);

        visitor.visitChildren(blockQuote);
        visitor.postSetSpans(spans, offset);

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
        var spans = visitor.preSetSpans(listItem, offset);
        visitor.visitChildren(listItem);
        visitor.postSetSpans(spans, offset);

        if (visitor.hasNext(listItem)) {
            visitor.ensureNewLine();
        }
    }
}
