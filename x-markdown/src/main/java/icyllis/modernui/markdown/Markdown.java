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

package icyllis.modernui.markdown;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Context;
import icyllis.modernui.markdown.core.CorePlugin;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.widget.TextView;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.function.Consumer;

/**
 * Entry point of Markdown API, it provides a context for parsing and rendering Markdown.
 */
public final class Markdown {

    private final Parser mParser;
    private final MarkdownConfig mConfig;

    @UnmodifiableView
    private final List<MarkdownPlugin> mPlugins;

    private final TextView.BufferType mBufferType;

    @Nullable
    private final TextSetter mTextSetter;

    Markdown(Parser parser,
             MarkdownConfig config,
             List<MarkdownPlugin> plugins,
             TextView.BufferType bufferType,
             @Nullable TextSetter textSetter) {
        mParser = parser;
        mConfig = config;
        mPlugins = plugins;
        mBufferType = bufferType;
        mTextSetter = textSetter;
    }

    /**
     * Create with default theme and core plugin.
     *
     * @param context view context
     * @return markdown inflater
     */
    @NonNull
    public static Markdown create(@NonNull Context context) {
        return new Builder(context)
                .usePlugin(CorePlugin.create())
                .build();
    }

    @NonNull
    public static Builder builder(@NonNull Context context) {
        return new Builder(context);
    }

    /**
     * Parse markdown into AST.
     *
     * @param input Markdown text to parse
     * @return root document
     */
    @NonNull
    public Document parse(@NonNull String input) {
        for (MarkdownPlugin plugin : mPlugins) {
            input = plugin.processMarkdown(input);
        }
        return mParser.parse(input);
    }

    /**
     * Render the tree of nodes to Modern UI rich text.
     * <p>
     * Please note that returned Spanned has few limitations. For example, images, tables
     * and ordered lists require TextView to be properly displayed. This is why images and tables
     * most likely won\'t work in this case. Ordered lists might have mis-measurements. Whenever
     * possible use {@link #setMarkdown(TextView, String)} or {@link #setParsedMarkdown(TextView, Spanned)}
     * as these methods will additionally call specific {@link MarkdownPlugin} methods to <em>prepare</em>
     * proper display.
     *
     * @param document the root node
     * @return the rendered Spanned
     */
    @NonNull
    public Spanned render(@NonNull Node document) {

        for (MarkdownPlugin plugin : mPlugins) {
            plugin.beforeRender(document);
        }

        MarkdownVisitor visitor = new MarkdownVisitor(mConfig);

        visitor.visit(document);

        for (MarkdownPlugin plugin : mPlugins) {
            plugin.afterRender(document, visitor);
        }

        return visitor.builder();
    }

    /**
     * Parse Markdown text into AST and render it to Modern UI rich text.
     *
     * @param input the markdown text
     * @return the rendered Spanned
     */
    @NonNull
    public Spanned convert(@NonNull String input) {
        return render(parse(input));
    }

    /**
     * Parse Markdown text into AST, render it to Modern UI rich text,
     * and install it to the given {@link TextView}.
     * <p>
     * In most cases, you just need this to display Markdown. Note that
     * you should not change TextView typography attributes after calling this.
     */
    @UiThread
    public void setMarkdown(@NonNull TextView textView, @NonNull String markdown) {
        setParsedMarkdown(textView, convert(markdown));
    }

    @UiThread
    public void setParsedMarkdown(@NonNull TextView textView, @NonNull Spanned markdown) {
        for (MarkdownPlugin plugin : mPlugins) {
            plugin.beforeSetText(textView, markdown);
        }
        if (mTextSetter != null) {
            mTextSetter.setText(textView, markdown, mBufferType, () -> {
                // sync operation
                for (MarkdownPlugin plugin : mPlugins) {
                    plugin.afterSetText(textView);
                }
            });
        } else {
            textView.setText(markdown, mBufferType);
            for (MarkdownPlugin plugin : mPlugins) {
                plugin.afterSetText(textView);
            }
        }
    }

    @Nullable
    public <P extends MarkdownPlugin> P getPlugin(@NonNull Class<P> type) {
        MarkdownPlugin out = null;
        for (MarkdownPlugin plugin : mPlugins) {
            if (type.isAssignableFrom(plugin.getClass())) {
                out = plugin;
            }
        }
        //noinspection unchecked
        return (P) out;
    }

    @NonNull
    public <P extends MarkdownPlugin> P requirePlugin(@NonNull Class<P> type) {
        return Objects.requireNonNull(getPlugin(type));
    }

    @UnmodifiableView
    @NonNull
    public List<MarkdownPlugin> getPlugins() {
        return mPlugins;
    }

    @NonNull
    public MarkdownConfig getConfig() {
        return mConfig;
    }

    public static final class Builder {

        private final Context mContext;
        private final LinkedHashSet<MarkdownPlugin> mPlugins = new LinkedHashSet<>(3);

        private TextView.BufferType mBufferType = TextView.BufferType.EDITABLE;
        private TextSetter mTextSetter;

        Builder(Context context) {
            mContext = Objects.requireNonNull(context);
        }

        @NonNull
        public Builder setBufferType(@NonNull TextView.BufferType bufferType) {
            mBufferType = Objects.requireNonNull(bufferType);
            return this;
        }

        @NonNull
        public Builder setTextSetter(@Nullable TextSetter textSetter) {
            mTextSetter = textSetter;
            return this;
        }

        @NonNull
        public Builder usePlugin(@NonNull MarkdownPlugin plugin) {
            mPlugins.add(
                    Objects.requireNonNull(plugin)
            );
            return this;
        }

        @NonNull
        public Markdown build() {

            List<MarkdownPlugin> plugins = new Registry(mPlugins)
                    .process();

            var parserBuilder = Parser.builder();
            var themeBuilder = MarkdownTheme.builderWithDefaults(mContext);
            var configBuilder = MarkdownConfig.builder();

            for (MarkdownPlugin plugin : plugins) {
                plugin.configureParser(parserBuilder);
                plugin.configureTheme(themeBuilder);
                plugin.configureConfig(configBuilder);
            }

            return new Markdown(parserBuilder.build(),
                    configBuilder.build(themeBuilder.build()),
                    Collections.unmodifiableList(plugins),
                    mBufferType, mTextSetter);
        }

        static class Registry implements MarkdownPlugin.Registry {

            private final LinkedHashSet<MarkdownPlugin> mAll;
            private final HashSet<MarkdownPlugin> mLoaded;
            private final HashSet<MarkdownPlugin> mVisited;

            private final ArrayList<MarkdownPlugin> mResults = new ArrayList<>();

            Registry(LinkedHashSet<MarkdownPlugin> all) {
                mAll = all;
                mLoaded = new HashSet<>(all.size());
                mVisited = new HashSet<>();
            }

            ArrayList<MarkdownPlugin> process() {
                for (MarkdownPlugin plugin : mAll) {
                    load(plugin);
                }
                mResults.trimToSize();
                return mResults;
            }

            @NonNull
            public <P extends MarkdownPlugin> P require(@NonNull Class<P> clazz) {
                return get(clazz);
            }

            @Override
            public <P extends MarkdownPlugin> void require(@NonNull Class<P> clazz,
                                                           @NonNull Consumer<? super P> action) {
                action.accept(get(clazz));
            }

            private void load(MarkdownPlugin plugin) {
                if (!mLoaded.contains(plugin)) {
                    if (!mVisited.add(plugin)) {
                        throw new IllegalStateException("Cyclic dependency chain found: " + mVisited);
                    }
                    plugin.configure(this);
                    mVisited.remove(plugin);
                    if (mLoaded.add(plugin)) {
                        if (plugin.getClass() == CorePlugin.class) {
                            mResults.add(0, plugin);
                        } else {
                            mResults.add(plugin);
                        }
                    }
                }
            }

            @NonNull
            private <P extends MarkdownPlugin> P get(@NonNull Class<? extends P> clazz) {
                P plugin = find(mLoaded, clazz);

                if (plugin == null) {
                    plugin = find(mAll, clazz);
                    if (plugin == null) {
                        throw new IllegalStateException("Requested plugin is not added: " +
                                "" + clazz.getName() + ", plugins: " + mAll);
                    }
                    load(plugin);
                }
                return plugin;
            }

            @Nullable
            private <P extends MarkdownPlugin> P find(Set<MarkdownPlugin> set, @NonNull Class<? extends P> clazz) {
                for (MarkdownPlugin plugin : set) {
                    if (clazz.isAssignableFrom(plugin.getClass())) {
                        return (P) plugin;
                    }
                }
                return null;
            }
        }
    }
}
