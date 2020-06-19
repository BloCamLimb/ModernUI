/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.widget;

import com.google.common.collect.Lists;
import icyllis.modernui.ui.test.IHost;
import icyllis.modernui.ui.test.Widget;
import icyllis.modernui.ui.test.Align9D;
import icyllis.modernui.ui.test.Locator;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Multi-paged scroll panel, each page has a fixed number of entries, and can sort them
 * All entries should be added by this class, or in constructor of scroll group
 * @param <E> entry type
 * @param <G> group type
 */
public class MultiPageScrollPanel<E extends UniformScrollEntry, G extends UniformScrollGroup<E>> extends ScrollPanel<E, G> {

    protected final int maxEntry;

    protected final List<E> allEntries;

    @Nullable
    private Comparator<E> comparator;

    private int cPage, maxPage;

    private boolean hasScrolled;

    public MultiPageScrollPanel(IHost host, @Nonnull Builder builder, @Nonnull Function<ScrollPanel<E, G>, G> group) {
        super(host, builder, group);
        this.maxEntry = builder.maxEntry;
        this.allEntries = Lists.newArrayList(this.group.entries);
        sortAndPaging();
    }

    /*@Override
    protected void onScrollPanel(double amount) {
        if (Screen.hasControlDown()) {
            if (amount > 0) {
                gotoPreviousPage();
            } else if (amount < 0) {
                gotoNextPage();
            }
        } else {
            super.onScrollPanel(amount);
        }
    }*/

    @Override
    public void callbackScrollAmount(float scrollAmount) {
        super.callbackScrollAmount(scrollAmount);
        hasScrolled = true;
    }

    @Override
    public void tick(int ticks) {
        if (hasScrolled && (ticks & 0x7) == 0) {
            onVisibleChanged(group.visible);
            hasScrolled = false;
        }
    }

    protected void onVisibleChanged(List<E> visible) {

    }

    protected void onPageChanged() {

    }

    private void sortAndPaging() {
        int c = cPage;
        int m = maxPage;
        maxPage = MathHelper.ceil((float) allEntries.size() / maxEntry);
        cPage = MathHelper.clamp(cPage, 1, maxPage);
        if (c != cPage || m != maxPage) {
            onPageChanged();
        }
        sort();
    }

    private void refreshPage(boolean scrollToTop) {
        group.entries.clear();
        for (int i = 0; i < maxEntry; i++) {
            int index = (cPage - 1) * maxEntry + i;
            if (index >= 0 && index < allEntries.size()) {
                group.entries.add(allEntries.get(index));
            }
        }
        layoutList();
        if (scrollToTop) {
            controller.scrollDirectBy(-getMaxScrollAmount());
        }
        hasScrolled = true;
    }

    public void sort() {
        if (comparator != null) {
            allEntries.sort(comparator);
        }
        refreshPage(false);
    }

    public void sort(@Nullable Comparator<E> comparator) {
        setComparator(comparator);
        sort();
    }

    public void setComparator(@Nullable Comparator<E> comparator) {
        this.comparator = comparator;
    }

    public void addEntry(@Nonnull E entry) {
        if (addEntry0(entry)) {
            sortAndPaging();
        }
    }

    public void addEntries(@Nonnull Collection<E> collection) {
        collection.forEach(this::addEntry0);
        sortAndPaging();
    }

    private boolean addEntry0(E entry) {
        if (!allEntries.contains(entry)) {
            allEntries.add(entry);
            return true;
        }
        return false;
    }

    public void removeEntry(E entry) {
        if (allEntries.remove(entry)) {
            sortAndPaging();
        }
    }

    public void clearEntries() {
        allEntries.clear();
        sortAndPaging();
    }

    /**
     * Always greater than or equal to 1
     * @return c
     */
    public int getCurrentPage() {
        return cPage;
    }

    public int getMaxPage() {
        return maxPage;
    }

    public void gotoNextPage() {
        if (cPage < maxPage) {
            ++cPage;
            refreshPage(true);
        }
    }

    public void gotoPreviousPage() {
        if (cPage > 1) {
            --cPage;
            refreshPage(true);
        }
    }

    public void gotoSpecificPage(int page) {
        if (page != cPage && page >= 1 && page <= maxPage) {
            cPage = page;
            refreshPage(true);
        }
    }

    public static class Builder extends Widget.Builder {

        protected final int maxEntry;

        public Builder(int maxEntry) {
            this.maxEntry = maxEntry;
        }

        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

        @Override
        public Builder setHeight(float height) {
            super.setHeight(height);
            return this;
        }

        @Override
        public Builder setLocator(@Nonnull Locator locator) {
            super.setLocator(locator);
            return this;
        }

        @Override
        public Builder setAlign(@Nonnull Align9D align) {
            super.setAlign(align);
            return this;
        }
    }
}
