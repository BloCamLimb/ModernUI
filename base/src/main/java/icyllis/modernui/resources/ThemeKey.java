/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.resources;

import icyllis.modernui.annotation.Immutable;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

/**
 * An immutable key identifying a theme: the result of applying a sequence of
 * style resources to an empty theme.
 *
 * <p>Each style resource, identified by a {@link ResourceId}, is a set of
 * attribute name to attribute value pairs. Applying a style either overwrites
 * attributes the theme already defines ({@code force = true}) or only fills in
 * attributes the theme does not define yet ({@code force = false}). A style may
 * also contain {@code @null} values, which remove the attribute when applied with
 * {@code force = true}. Because of this, the order of applications matters, and
 * the contents of styles are not known to the key.
 *
 * <p>Only applications that can never change the resulting theme are discarded.
 * Consequently, equal keys always describe equal themes, but equivalent themes
 * are not guaranteed to have equal keys. This is safe for caching: the worst case
 * is a missed cache hit.
 *
 * <p>Instances are immutable and thread-safe. Use {@link Builder} to create them.
 *
 * @see Resources#newTheme(ThemeKey)
 */
@Immutable
public final class ThemeKey {

    /** Style resources in application order. Never mutated after construction. */
    private final Object[] items;

    /** {@code forces[i]} is the force flag of the application of {@code ids[i]}. Never mutated. */
    private final boolean[] force;

    /** Precomputed hash code. */
    private final transient int hash;

    /**
     * Creates a key that takes ownership of the given arrays.
     *
     * @param items style resources in application order; must not be shared or mutated afterwards
     * @param force force flags, same length as {@code resId}; must not be shared or mutated afterwards
     */
    private ThemeKey(Object[] items, boolean[] force) {
        this.items = items;
        this.force = force;

        int hash = 0;
        for (int i = 0; i < items.length; i++) {
            hash = 31 * hash + items[i].hashCode();
            hash = 31 * hash + (force[i] ? 1 : 0);
        }
        this.hash = hash;
    }

    /**
     * Returns the number of style applications that make up this theme.
     *
     * @return the number of applications
     */
    public int count() {
        return items.length;
    }

    /**
     * Returns the style resource of the application at the given position.
     *
     * @param index the position, in {@code [0, size())}
     * @return the style resource id
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     */
    public @NonNull Object item(int index) {
        return items[index];
    }

    /**
     * Returns whether the application at the given position overwrites existing
     * attribute values.
     *
     * @param index the position, in {@code [0, size())}
     * @return {@code true} if the style overwrites attributes already defined in
     *         the theme, {@code false} if it only fills in undefined ones
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     */
    public boolean isForce(int index) {
        return force[index];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ThemeKey t &&
                hash == t.hash &&
                Arrays.equals(force, t.force) &&
                Arrays.equals(items, t.items);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append('(').append(items[i]).append(", force=").append(force[i]).append(')');
        }
        return sb.append(']').toString();
    }

    // ------------------------------------------------------------------

    /**
     * Builds a {@link ThemeKey} by describing, step by step, which styles are
     * applied to a theme. Applications that cannot change the theme are discarded
     * automatically.
     *
     * <p>This class is not thread-safe.
     */
    public static final class Builder {

        /** Style resources applied so far, in order; only the first {@link #count} entries are valid. */
        private Object[] items = new Object[8];

        /** Force flags matching {@link #items}; only the first {@link #count} entries are valid. */
        private boolean[] force = new boolean[8];

        /** Number of valid entries in {@link #items} and {@link #force}. */
        private int count;

        /**
         * Styles applied since the most recent forced application, including that
         * application itself. A non-forced application of one of these can never
         * change the theme, because nothing has been removed since.
         */
        private final HashSet<Object> sinceForce = new HashSet<>();

        /** Creates a builder describing an empty theme. */
        public Builder() {}

        /**
         * Layers a style resource on top of the theme described so far.
         *
         * <p>If {@code force} is {@code true}, every attribute the style defines
         * ends up with the style's value, replacing any value the theme already has,
         * and an {@code @null} value removes the attribute. If {@code false}, the
         * style only supplies attributes the theme does not define yet, and existing
         * values are kept.
         *
         * <p>Layering a style that cannot make a difference to the resulting theme
         * leaves the builder unchanged.
         *
         * @param resId the style resource to layer; must not be {@code null}
         * @param force whether the style's values take precedence over the theme's
         *              existing values
         * @return this builder
         * @throws NullPointerException if {@code resId} is {@code null}
         */
        public Builder append(@NonNull ResourceId resId, boolean force) {
            append((Object) resId, force);
            return this;
        }

        public Builder append(@NonNull InlineStyle style, boolean force) {
            append((Object) style, force);
            return this;
        }

        private void append(@NonNull Object item, boolean force) {
            Objects.requireNonNull(item);
            if (force) {
                // Applying the same forced style twice in a row is idempotent.
                if (count > 0 && this.force[count - 1] && this.items[count - 1].equals(item))
                    return;
                sinceForce.clear();
                sinceForce.add(item);
            } else if (!sinceForce.add(item)) {
                return;
            }
            if (count == this.items.length) {
                this.items = Arrays.copyOf(this.items, count * 2);
                this.force = Arrays.copyOf(this.force, count * 2);
            }
            this.items[count] = item;
            this.force[count++] = force;
        }

        /**
         * Layers another theme on top of the theme described so far, by applying
         * the same styles, in the same order and with the same force flags, that
         * produced {@code other}.
         *
         * @param other the theme to layer; must not be {@code null}
         * @return this builder
         * @throws NullPointerException if {@code other} is {@code null}
         */
        public Builder append(@NonNull ThemeKey other) {
            if (count == 0)
                return setTo(other);   // replaying into an empty builder yields other itself
            for (int i = 0; i < other.items.length; i++)
                append(other.items[i], other.force[i]);
            return this;
        }

        /**
         * Discards everything applied so far, so that the builder describes an
         * empty theme again.
         *
         * @return this builder
         */
        public Builder clear() {
            Arrays.fill(items, 0, count, null);
            count = 0;
            sinceForce.clear();
            return this;
        }

        /**
         * Discards everything applied so far and makes the builder describe exactly
         * the theme of the given key. Further styles can then be layered on top.
         *
         * @param key the key to start from; must not be {@code null}
         * @return this builder
         * @throws NullPointerException if {@code key} is {@code null}
         */
        public Builder setTo(@NonNull ThemeKey key) {
            clear();
            int len = key.items.length;
            if (items.length < len) {
                items = new Object[len];
                force = new boolean[len];
            }
            System.arraycopy(key.items, 0, items, 0, len);
            System.arraycopy(key.force, 0, force, 0, len);
            count = len;
            // Rebuild the redundancy set: everything from the last forced application onwards.
            int last = len - 1;
            while (last >= 0 && !force[last])
                last--;
            for (int i = Math.max(last, 0); i < len; i++)
                sinceForce.add(items[i]);
            return this;
        }

        /**
         * Creates the key for the theme described so far. The builder remains usable.
         *
         * @return a new immutable key
         */
        @Contract(" -> new")
        public @NonNull ThemeKey build() {
            return new ThemeKey(Arrays.copyOf(items, count), Arrays.copyOf(force, count));
        }
    }
}
