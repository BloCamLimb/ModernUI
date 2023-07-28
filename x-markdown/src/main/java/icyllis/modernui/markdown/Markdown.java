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
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.markdown.core.CorePlugin;
import icyllis.modernui.text.Spanned;

import java.util.*;

/**
 * Entry point of Markdown API, it provides a context for parsing and rendering Markdown.
 */
public final class Markdown {

    private final Parser mParser;
    private final MarkdownConfig mConfig;

    private final List<Plugin> mPlugins;

    Markdown(Parser parser, MarkdownConfig config, List<Plugin> plugins) {
        mParser = parser;
        mConfig = config;
        mPlugins = plugins;
    }

    @NonNull
    public static Markdown create(@NonNull Context context) {
        Objects.requireNonNull(context);
        return new Markdown.Builder(context)
                .usePlugin(new CorePlugin())
                .build();
    }

    @NonNull
    public Document parse(@NonNull String input) {
        for (Plugin plugin : mPlugins) {
            input = plugin.preProcess(input);
        }
        return mParser.parse(input);
    }

    /**
     * Render the tree of nodes to Modern UI rich text.
     *
     * @param document the root node
     * @return the rendered Spanned
     */
    @NonNull
    public Spanned render(@NonNull Node document) {

        MarkdownVisitor visitor = new MarkdownVisitor(mConfig);

        visitor.visit(document);

        return visitor.builder();
    }

    @NonNull
    public Spanned convert(@NonNull String input) {
        return render(parse(input));
    }

    public static final class Builder {

        private final Context mContext;
        private final LinkedHashSet<Plugin> mExtensions = new LinkedHashSet<>();

        public Builder(Context context) {
            mContext = context;
        }

        @NonNull
        public Builder usePlugin(@NonNull Plugin extension) {
            Objects.requireNonNull(extension);
            mExtensions.add(extension);
            return this;
        }

        @NonNull
        public Markdown build() {

            MarkdownConfig.Builder builder = new MarkdownConfig.Builder();

            List<Plugin> plugins = new MarkdownPluginRegistry(mExtensions)
                    .process();

            for (Plugin plugin : plugins) {
                plugin.configure(builder);
            }

            return new Markdown(Parser.builder().build(),
                    builder.build(MarkdownTheme.create(mContext)),
                    Collections.unmodifiableList(plugins));
        }
    }
}
