/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.text.style;

import icyllis.modernui.core.Core;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.Objects;

/**
 * Implementation of the {@link ClickableSpan} that allows setting a url string. When
 * selecting and clicking on the text to which the span is attached, the <code>URLSpan</code>
 * will try to open the url, by calling {@link Core#openURL(URL)}.
 * <p>
 * For example, a <code>URLSpan</code> can be used like this:
 * <pre>
 * SpannableString string = new SpannableString("Text with a url span");
 * string.setSpan(new URLSpan("http://www.developer.android.com"), 12, 15, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
 * </pre>
 * <img src="https://developer.android.com/reference/android/images/text/style/urlspan.png" />
 * <figcaption>Text with <code>URLSpan</code>.</figcaption>
 */
public class URLSpan extends ClickableSpan {

    private final String mURL;

    /**
     * Constructs a {@link URLSpan} from an url string.
     *
     * @param url the url string
     */
    public URLSpan(String url) {
        mURL = Objects.requireNonNull(url);
    }

    /**
     * Get the url string for this span.
     *
     * @return the url string.
     */
    @Nonnull
    public String getURL() {
        return mURL;
    }

    @Override
    public void onClick(@Nonnull View widget) {
        Core.openURI(mURL);
    }
}
