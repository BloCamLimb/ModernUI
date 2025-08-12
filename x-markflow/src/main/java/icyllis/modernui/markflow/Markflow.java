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

package icyllis.modernui.markflow;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.core.Context;
import icyllis.modernui.markflow.core.CorePlugin;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.widget.TextView;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.jetbrains.annotations.UnmodifiableView;

import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.function.Consumer;

/**
 * Entry point of Markdown API, it provides a context for parsing and rendering Markdown.
 * Markdown parsed and rendered by the same Markflow object is done in the same manner.
 * <p>
 * Note that Markflow does not provide any caching for parsing and rendering. Each call
 * to {@link #parse} and {@link #render} will create an instance for tracking internal
 * states. Therefore they are also thread-safe and can be done in any background thread.
 * <p>
 * To display a Markdown document on the screen, you need to follow the following steps:
 * <ol>
 *     <li>Call {@link #parse(CharSequence)} with the raw Markdown input (treated as plain text)
 *     to obtain a tree of nodes.</li>
 *     <li>Call {@link #render(Node)} with the tree of nodes to obtain a text with styling spans,
 *     which is also known as rendered Markdown.</li>
 *     <li>Call {@link #setRenderedMarkdown(TextView, Spanned)} to install the rendered Markdown
 *     to a TextView on UI thread. Then TextView will then be responsible for creating text layout
 *     and the render node for multi-frame rendering.</li>
 * </ol>
 * For convenience, {@link #convert(CharSequence)} can be used to parse & render Markdown,
 * this method can be called from any thread. {@link #setMarkdown(TextView, CharSequence)}
 * can be used to parse & render & install Markdown, but it can only be called from UI thread.
 * <p>
 * <b>Note that:</b> Once a TextView installs Markdown text through Markflow, it must also be
 * uninstalled using the same Markflow.
 *
 * @since 3.12
 */
@Immutable
public final class Markflow {

    /**
     * Create a minimal Markflow instance with the default theme and the
     * core plugin. Only core Markdown features are enabled, and images
     * will not be resolved.
     *
     * @param context view context
     * @return a new Markflow instance with only core plugin registered
     */
    @NonNull
    public static Markflow create(@NonNull Context context) {
        return builder(context, true).build();
    }

    /**
     * Create a new instance of {@link Builder} with core plugin registered.
     *
     * @param context view context
     * @return a new Builder instance with core plugin registered
     */
    @NonNull
    public static Builder builder(@NonNull Context context) {
        return builder(context, true);
    }

    /**
     * Create a new instance of {@link Builder}, optionally with core plugin registered.
     *
     * @param context  view context
     * @param withCore whether to register core plugin
     * @return a new Builder instance
     */
    @NonNull
    public static Builder builder(@NonNull Context context, boolean withCore) {
        var builder = new Builder(context);
        if (withCore) {
            builder.usePlugin(CorePlugin.create());
        }
        return builder;
    }

    private final Parser mParser;
    private final MarkflowConfig mConfig;

    private final MarkflowPlugin[] mPlugins;
    private final List<? extends MarkflowPlugin> mPluginsView;

    private final TextView.BufferType mBufferType;
    @Nullable
    private final TextSetter mTextSetter;

    private final Map<Class<? extends Node>, NodeVisitor<Node>> mVisitors;
    @Nullable
    private final BlockHandler mBlockHandler;

    private Markflow(@NonNull Parser parser,
                     @NonNull MarkflowConfig config,
                     @NonNull MarkflowPlugin[] plugins,
                     @NonNull TextView.BufferType bufferType,
                     @Nullable TextSetter textSetter,
                     @NonNull Map<Class<? extends Node>, NodeVisitor<Node>> visitors,
                     @Nullable BlockHandler blockHandler) {
        mParser = parser;
        mConfig = config;
        mPlugins = plugins;
        //noinspection Java9CollectionFactory
        mPluginsView = Collections.unmodifiableList(Arrays.asList(mPlugins)); // zero copy
        mBufferType = bufferType;
        mTextSetter = textSetter;
        mVisitors = visitors;
        mBlockHandler = blockHandler;
    }

    /**
     * Parse the input Markdown text into AST.
     * <p>
     * This method is thread-safe and allows multiple threads to start parsings
     * for different source text concurrently.
     *
     * @param input the Markdown text to parse
     * @return the root document, can be cast to {@link org.commonmark.node.Document}
     */
    @NonNull
    public Node parse(@NonNull CharSequence input) {
        for (MarkflowPlugin plugin : mPlugins) {
            input = plugin.processMarkdown(input);
        }
        return mParser.parse(input.toString());
    }

    /**
     * Render the tree of nodes to a Spanned text, which can be handled by framework's
     * text layout engine.
     * <p>
     * This method is thread-safe and allows multiple threads to start renderings
     * for different documents concurrently.
     * <p>
     * Note that returned Spanned has few limitations. For example, images, tables
     * and ordered lists require TextView to be properly displayed. Ordered list items
     * require text layout based on TextView's parameters. Whenever possible use
     * {@link #setMarkdown(TextView, CharSequence)} or {@link #setRenderedMarkdown(TextView, Spanned)}
     * as these methods will additionally call specific {@link MarkflowPlugin} methods
     * to <em>prepare</em> proper display.
     *
     * @param document the root node
     * @return the rendered Markdown
     */
    @NonNull
    public Spanned render(@NonNull Node document) {

        for (MarkflowPlugin plugin : mPlugins) {
            plugin.beforeRender(document);
        }

        MarkflowVisitor visitor = new MarkflowVisitor(mConfig, mVisitors, mBlockHandler);

        visitor.visit(document);

        for (MarkflowPlugin plugin : mPlugins) {
            plugin.afterRender(document, visitor);
        }

        return visitor.getSpannableBuilder();
    }

    /**
     * This is a helper method, calling this method is the same as calling
     * {@link #parse(CharSequence)} and then {@link #render(Node)}.
     * See {@link #render(Node)} for details.
     *
     * @param input the raw Markdown text
     * @return the rendered Markdown
     */
    @NonNull
    public Spanned convert(@NonNull CharSequence input) {
        return render(parse(input));
    }

    /**
     * This is a helper method, calling this method is the same as calling
     * {@link #parse(CharSequence)} then {@link #render(Node)}, and then
     * {@link #setRenderedMarkdown(TextView, Spanned)}.
     * <p>
     * In most cases, you just need this to display Markdown, when Markdown
     * is static and small. Note that you should not change TextView typography
     * attributes after calling this.
     * <p>
     * This method can be called only from UI thread.
     */
    @UiThread
    public void setMarkdown(@NonNull TextView textView, @NonNull CharSequence markdown) {
        setRenderedMarkdown(textView, convert(markdown));
    }

    /**
     * Install a rendered Markdown to the given {@link TextView}.
     * <p>
     * This method can be called only from UI thread.
     *
     * @param textView the target text view used to display the Markdown
     * @param markdown the rendered Markdown to display
     */
    @UiThread
    public void setRenderedMarkdown(@NonNull TextView textView, @NonNull Spanned markdown) {
        final MarkflowPlugin[] plugins = mPlugins;

        for (MarkflowPlugin plugin : plugins) {
            plugin.beforeSetText(textView, markdown);
        }

        if (mTextSetter != null) {
            mTextSetter.setText(textView, markdown, mBufferType, (survivingTextView) -> {
                // sync operation, we only capture plugin list
                // remember not to hold strong reference to the original TextView
                for (MarkflowPlugin plugin : plugins) {
                    plugin.afterSetText(survivingTextView);
                }
            });
        } else {
            textView.setText(markdown, mBufferType);

            for (MarkflowPlugin plugin : plugins) {
                plugin.afterSetText(textView);
            }
        }
    }

    /**
     * Requests information if certain plugin has been registered.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <P extends MarkflowPlugin> P getPlugin(@NonNull Class<P> type) {
        for (MarkflowPlugin plugin : mPlugins) {
            if (type.isAssignableFrom(plugin.getClass())) {
                return (P) plugin;
            }
        }
        return null;
    }

    /**
     * Requests information for certain plugin. Throws an exception if the plugin
     * is not registered.
     *
     * @see #getPlugin(Class)
     */
    @NonNull
    public <P extends MarkflowPlugin> P requirePlugin(@NonNull Class<P> type) {
        return Objects.requireNonNull(getPlugin(type));
    }

    /**
     * Returns an immutable view of all registered plugins, sorted by dependencies.
     */
    @UnmodifiableView
    @NonNull
    public List<? extends MarkflowPlugin> getAllPlugins() {
        return mPluginsView;
    }

    @NonNull
    public MarkflowConfig getConfig() {
        return mConfig;
    }

    public static final class Builder {

        private final Context mContext;
        private final LinkedHashSet<MarkflowPlugin> mPlugins = new LinkedHashSet<>(3);

        private TextView.BufferType mBufferType = TextView.BufferType.SPANNABLE;
        private TextSetter mTextSetter;

        Builder(@NonNull Context context) {
            mContext = Objects.requireNonNull(context);
        }

        /**
         * Specify the buffer type when applying text to a TextView.
         * The default is {@link TextView.BufferType#SPANNABLE}.
         * <p>
         * Note that if you insist on {@link TextView.BufferType#SPANNABLE} and your rendered
         * Markdown text contains too many spans, consider setting a factory that returns a
         * {@link icyllis.modernui.text.SpannableStringBuilder} via the {@link TextView#setSpannableFactory}
         * It uses interval tree and hash table internally, making it much more efficient than
         * {@link icyllis.modernui.text.SpannableString} (default factory) when processing long and complex text.
         * Although the text is editable, the TextView is not editable (bufferType is SPANNABLE).
         *
         * @return this
         */
        @NonNull
        public Builder bufferType(@NonNull TextView.BufferType bufferType) {
            mBufferType = Objects.requireNonNull(bufferType);
            return this;
        }

        /**
         * Specify a method to install a rendered Markdown to a TextView.
         * By default, {@link TextView#setText(CharSequence, TextView.BufferType)}
         * is called directly.
         *
         * @return this
         */
        @NonNull
        public Builder textSetter(@Nullable TextSetter textSetter) {
            mTextSetter = textSetter;
            return this;
        }

        /**
         * Register a plugin.
         * <p>
         * It's better to create a new Plugin object if it is mutable, depending
         * on the implementation.
         *
         * @return this
         */
        @NonNull
        public Builder usePlugin(@NonNull MarkflowPlugin plugin) {
            mPlugins.add(
                    Objects.requireNonNull(plugin)
            );
            return this;
        }

        /**
         * Build a Markflow instance. This builder can be reused and the state will be preserved.
         *
         * @throws IllegalStateException no plugins were added, or required plugins were not added,
         *                               or there were circular dependencies.
         */
        @NonNull
        public Markflow build() {
            if (mPlugins.isEmpty()) {
                throw new IllegalStateException("At least one plugin is required to build Markflow");
            }

            final MarkflowPlugin[] plugins = new Markflow.Registry(mPlugins)
                    .process();

            var parserBuilder = Parser.builder();
            var themeBuilder = MarkflowTheme.builderWithDefaults(mContext);
            var configBuilder = MarkflowConfig.builder();
            var visitorBuilder = MarkflowVisitor.builder();

            for (MarkflowPlugin plugin : plugins) {
                plugin.configureParser(parserBuilder);
                plugin.configureTheme(themeBuilder);
                plugin.configureConfig(configBuilder);
                plugin.configureVisitor(visitorBuilder);
            }

            return new Markflow(parserBuilder.build(),
                    configBuilder.build(themeBuilder.build()),
                    plugins, mBufferType, mTextSetter,
                    new HashMap<>(visitorBuilder.mVisitors),
                    visitorBuilder.mBlockHandler);
        }

    }

    static final class Registry implements MarkflowPlugin.Registry {

        private final LinkedHashSet<MarkflowPlugin> mAll;
        private final HashSet<MarkflowPlugin> mLoaded;
        private final HashSet<MarkflowPlugin> mVisited;

        private final ArrayList<MarkflowPlugin> mResults = new ArrayList<>();

        Registry(@NonNull LinkedHashSet<MarkflowPlugin> all) {
            mAll = all;
            mLoaded = new HashSet<>(all.size());
            mVisited = new HashSet<>();
        }

        @NonNull
        MarkflowPlugin[] process() {
            for (MarkflowPlugin plugin : mAll) {
                load(plugin);
            }
            return mResults.toArray(new MarkflowPlugin[0]);
        }

        @NonNull
        public <P extends MarkflowPlugin> P require(@NonNull Class<P> clazz) {
            return get(clazz);
        }

        @Override
        public <P extends MarkflowPlugin> void require(@NonNull Class<P> clazz,
                                                       @NonNull Consumer<? super P> action) {
            action.accept(get(clazz));
        }

        private void load(MarkflowPlugin plugin) {
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
        private <P extends MarkflowPlugin> P get(@NonNull Class<? extends P> clazz) {
            P plugin = find(mLoaded, clazz);

            if (plugin == null) {
                plugin = find(mAll, clazz);
                if (plugin == null) {
                    throw new IllegalStateException("Requested plugin is not added: " + clazz.getName() + ", " +
                            "plugins: " + mAll);
                }
                load(plugin);
            }
            return plugin;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        private <P extends MarkflowPlugin> P find(Set<MarkflowPlugin> set, @NonNull Class<? extends P> clazz) {
            for (MarkflowPlugin plugin : set) {
                if (clazz.isAssignableFrom(plugin.getClass())) {
                    return (P) plugin;
                }
            }
            return null;
        }
    }
}
