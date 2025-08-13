/*
 * Modern UI.
 * Copyright (C) 2023-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.markflow.core;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.markflow.MarkflowConfig;
import icyllis.modernui.markflow.MarkflowPlugin;
import icyllis.modernui.markflow.MarkflowTheme;
import icyllis.modernui.markflow.MarkflowVisitor;
import icyllis.modernui.markflow.core.style.CodeBlockSpan;
import icyllis.modernui.markflow.core.style.HeadingSpan;
import icyllis.modernui.markflow.core.style.NumberSpan;
import icyllis.modernui.markflow.core.style.ThematicBreakSpan;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.style.AbsoluteSizeSpan;
import icyllis.modernui.text.style.BackgroundColorSpan;
import icyllis.modernui.text.style.BulletSpan;
import icyllis.modernui.text.style.ForegroundColorSpan;
import icyllis.modernui.text.style.QuoteSpan;
import icyllis.modernui.text.style.RelativeSizeSpan;
import icyllis.modernui.text.style.StyleSpan;
import icyllis.modernui.text.style.TypefaceSpan;
import icyllis.modernui.text.style.URLSpan;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.widget.TextView;
import org.commonmark.node.*;

public final class CorePlugin implements MarkflowPlugin {

    public static final String CORE_ORDERED_LIST = "core:ordered_list";
    public static final String CORE_ORDERED_LIST_ITEM_NUMBER = "core:ordered_list_item_number";

    @NonNull
    public static CorePlugin create() {
        return new CorePlugin();
    }

    CorePlugin() {
    }

    @Override
    public void configureConfig(@NonNull MarkflowConfig.Builder builder) {
        builder
                .appendSpanFactory(StrongEmphasis.class,
                        (config, node, props) -> new StyleSpan(TextPaint.BOLD))
                .appendSpanFactory(Emphasis.class,
                        (config, node, props) -> new StyleSpan(TextPaint.ITALIC))
                .appendSpanFactory(Heading.class,
                        (config, node, props) -> new HeadingSpan(config.getTheme(), node.getLevel()))

                .appendSpanFactory(ListItem.class, this::createListItemSpans)

                .appendSpanFactory(BlockQuote.class,
                        (config, node, props) -> new QuoteSpan(
                                config.getTheme().getBlockQuoteMargin(),
                                config.getTheme().getBlockQuoteWidth(),
                                config.getTheme().getBlockQuoteColor()))

                .appendSpanFactory(Code.class, this::createCodeSpans)
                .appendSpanFactory(FencedCodeBlock.class,
                        (config, node, args) -> new CodeBlockSpan(config.getTheme()))
                .appendSpanFactory(IndentedCodeBlock.class,
                        (config, node, args) -> new CodeBlockSpan(config.getTheme()))

                .appendSpanFactory(Link.class,
                        (config, node, args) -> new URLSpan(node.getDestination()))
                .appendSpanFactory(ThematicBreak.class,
                        (config, node, args) -> new ThematicBreakSpan(config.getTheme()));
    }

    @Override
    public void configureVisitor(@NonNull MarkflowVisitor.Builder builder) {
        builder
                .on(Text.class, this::visitText)
                .on(StrongEmphasis.class, this::visitSimpleNode)
                .on(Emphasis.class, this::visitSimpleNode)

                .on(SoftLineBreak.class, this::visitSoftLineBreak)
                .on(HardLineBreak.class, this::visitHardLineBreak)

                .on(Heading.class, this::visitHeading)
                .on(Paragraph.class, this::visitParagraph)

                .on(ListItem.class, this::visitListItem)

                .on(BulletList.class, this::visitSimpleBlock)
                .on(OrderedList.class, this::visitOrderedList)

                .on(BlockQuote.class, this::visitBlockQuote)

                .on(Code.class, this::visitCode)
                .on(FencedCodeBlock.class, this::visitFencedCodeBlock)
                .on(IndentedCodeBlock.class, this::visitIndentedCodeBlock)

                .on(Link.class, this::visitSimpleNode)

                .on(ThematicBreak.class, this::visitThematicBreak);
    }

    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
        NumberSpan.measure(textView, markdown);
    }

    private void visitSimpleBlock(
            @NonNull MarkflowVisitor visitor,
            @NonNull Block block) {
        visitor.beforeBlock(block);
        int offset = visitor.length();
        var spans = visitor.preSetSpans(block, offset);
        visitor.visitChildren(block);
        visitor.postSetSpans(spans, offset);
        visitor.afterBlock(block);
    }

    private void visitOrderedList(
            @NonNull MarkflowVisitor visitor,
            @NonNull OrderedList orderedList) {
        visitor.beforeBlock(orderedList);

        int offset = visitor.length();
        var spans = visitor.preSetSpans(orderedList, offset);

        visitor.getArguments()
                .put(CORE_ORDERED_LIST_ITEM_NUMBER, orderedList.getMarkerStartNumber());

        visitor.visitChildren(orderedList);

        visitor.postSetSpans(spans, offset);

        visitor.afterBlock(orderedList);
    }

    private void visitCode(
            @NonNull MarkflowVisitor visitor,
            @NonNull Code code) {
        int offset = visitor.length();
        var spans = visitor.preSetSpans(code, offset);
        visitor
                .append('\u00a0')
                .append(code.getLiteral())
                .append('\u00a0');
        visitor.postSetSpans(spans, offset);
    }

    private void visitFencedCodeBlock(
            @NonNull MarkflowVisitor visitor,
            @NonNull FencedCodeBlock fencedCodeBlock) {
        visitCodeBlock(
                visitor,
                fencedCodeBlock.getInfo(),
                fencedCodeBlock.getLiteral(),
                fencedCodeBlock
        );
    }

    private void visitIndentedCodeBlock(
            @NonNull MarkflowVisitor visitor,
            @NonNull IndentedCodeBlock indentedCodeBlock) {
        visitCodeBlock(
                visitor,
                null,
                indentedCodeBlock.getLiteral(),
                indentedCodeBlock
        );
    }

    private void visitCodeBlock(
            @NonNull MarkflowVisitor visitor,
            @Nullable String info,
            @NonNull String code,
            @NonNull Block block) {
        visitor.beforeBlock(block);

        int offset = visitor.length();
        var spans = visitor.preSetSpans(block, offset);

        visitor.append('\u00a0').append('\n')
                .append(code);

        visitor.ensureNewLine();

        visitor.append('\u00a0');

        visitor.postSetSpans(spans, offset);

        visitor.afterBlock(block);
    }

    @NonNull
    private Object createCodeSpans(
            @NonNull MarkflowConfig config,
            @NonNull Code code,
            @NonNull DataSet args) {
        MarkflowTheme theme = config.getTheme();
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
            spans[1] = new RelativeSizeSpan(0.875F);
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

    @NonNull
    private Object createListItemSpans(
            @NonNull MarkflowConfig config,
            @NonNull ListItem listItem,
            @NonNull DataSet args) {
        if (args.getBoolean(CORE_ORDERED_LIST)) {
            String number = args.getInt(CORE_ORDERED_LIST_ITEM_NUMBER, 1) + ".\u00a0";
            return new NumberSpan(config.getTheme(), number);
        }
        int level = listLevel(listItem);
        return new BulletSpan(config.getTheme().getListItemMargin(),
                0, config.getTheme().getListItemColor(), level);
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

    private void visitText(@NonNull MarkflowVisitor visitor, @NonNull Text text) {
        visitor.append(text.getLiteral());
    }

    private void visitSimpleNode(@NonNull MarkflowVisitor visitor, @NonNull Node node) {
        int offset = visitor.length();
        var spans = visitor.preSetSpans(node, offset);
        visitor.visitChildren(node);
        visitor.postSetSpans(spans, offset);
    }

    private void visitSoftLineBreak(@NonNull MarkflowVisitor visitor, @NonNull SoftLineBreak softLineBreak) {
        visitor.append(" ");
    }

    private void visitHardLineBreak(@NonNull MarkflowVisitor visitor, @NonNull HardLineBreak softLineBreak) {
        visitor.ensureNewLine();
    }

    private void visitHeading(
            @NonNull MarkflowVisitor visitor,
            @NonNull Heading heading) {
        visitor.beforeBlock(heading);
        int offset = visitor.length();
        var spans = visitor.preSetSpans(heading, offset);
        visitor.visitChildren(heading);
        visitor.postSetSpans(spans, offset);
        visitor.afterBlock(heading);
    }

    private void visitParagraph(
            @NonNull MarkflowVisitor visitor,
            @NonNull Paragraph paragraph) {
        boolean inTightList = isInTightList(paragraph);

        if (!inTightList) {
            visitor.beforeBlock(paragraph);
        }

        int offset = visitor.length();
        var spans = visitor.preSetSpans(paragraph, offset);
        visitor.visitChildren(paragraph);
        visitor.postSetSpans(spans, offset);

        if (!inTightList) {
            visitor.afterBlock(paragraph);
        }
    }

    private void visitBlockQuote(
            @NonNull MarkflowVisitor visitor,
            @NonNull BlockQuote blockQuote) {
        visitor.beforeBlock(blockQuote);

        int offset = visitor.length();

        var spans = visitor.preSetSpans(blockQuote, offset);

        visitor.visitChildren(blockQuote);
        if (visitor.length() == offset) {
            // Add ZWSP to render it
            visitor.append('\u200b');
        }
        visitor.postSetSpans(spans, offset);

        visitor.afterBlock(blockQuote);
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
            @NonNull MarkflowVisitor visitor,
            @NonNull ListItem listItem) {
        boolean isOrderedList = listItem.getParent() instanceof OrderedList;
        visitor.getArguments()
                .putBoolean(CORE_ORDERED_LIST, isOrderedList);

        int offset = visitor.length();
        var spans = visitor.preSetSpans(listItem, offset);
        visitor.visitChildren(listItem);
        visitor.postSetSpans(spans, offset);

        if (isOrderedList) {
            int number = visitor.getArguments()
                    .getInt(CORE_ORDERED_LIST_ITEM_NUMBER);
            visitor.getArguments()
                    .putInt(CORE_ORDERED_LIST_ITEM_NUMBER, number + 1);
        }

        if (visitor.hasNext(listItem)) {
            visitor.ensureNewLine();
        }
    }

    private void visitThematicBreak(
            @NonNull MarkflowVisitor visitor,
            @NonNull ThematicBreak thematicBreak) {
        visitor.beforeBlock(thematicBreak);

        int offset = visitor.length();

        var spans = visitor.preSetSpans(thematicBreak, offset);
        // Add ZWSP to render it
        visitor.append('\u200b');
        visitor.postSetSpans(spans, offset);

        visitor.afterBlock(thematicBreak);
    }
}
