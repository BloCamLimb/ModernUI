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

import icyllis.arc3d.fastutil.HashCommon;
import icyllis.modernui.annotation.Immutable;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.util.Log;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 * Assigns a slot to every attribute that a resolution needs across an inheritance chain.
 *
 * <p>A slot is the position of an attribute in the output array of one resolution. Slots are
 * append-only: the parent keeps its slots {@code [0, parent.size())}, and the {@code j}-th child name
 * gets slot {@code parent.size() + j}. Child names are appended as they are, without deduplication, so
 * the slot of a child name is known from its position alone and parent slots never move.
 *
 * <p>Child names are expected to be sorted ascending by {@link String#compareTo(String)} and unique, both
 * among themselves and against the parent. A violation is logged as a warning and does not fail the
 * construction. A name that is already present keeps its original slot, which is the one that
 * {@link #indexOf(String)} returns; the slot appended for the repeated name stays allocated but cannot be
 * found by name.
 */
@Immutable
public final class AttributeSlots {

    public static final Marker MARKER = MarkerFactory.getMarker("AttributeSlots");

    /** Name of every slot, parent names first. */
    private final @NonNull String keys @NonNull[];

    /** Open-addressing table holding slots, where {@code -1} marks an empty cell. */
    private final int table @NonNull[];

    private final int mask;

    /**
     * Builds the slots of a child on top of the slots of its parent.
     *
     * @param parent     slots of the parent, or {@code null} if there is none
     * @param childNames names the child needs, expected sorted ascending and unique against each other
     *                   and against the parent
     */
    public AttributeSlots(@Nullable AttributeSlots parent, @NonNull String @NonNull[] childNames) {
        this(parent, childNames, true);
    }

    /**
     * Builds the slots of a child on top of the slots of its parent, taking the child names from another
     * instance.
     *
     * @param parent slots of the parent
     * @param child  slots whose names are appended after the parent's
     */
    public AttributeSlots(@NonNull AttributeSlots parent, @NonNull AttributeSlots child) {
        this(Objects.requireNonNull(parent), child.keys, false);
    }

    private AttributeSlots(@Nullable AttributeSlots parent, @NonNull String @NonNull[] childNames,
                           boolean checkOrder) {
        int parentCount = parent != null ? parent.keys.length : 0;
        int total = parentCount + childNames.length;

        int capacity = Math.max(
                2, MathUtil.ceilPow2((int) Math.ceil((double) total / 0.75))
        );
        int mask = capacity - 1;

        String[] keys = new String[total];
        int[] table;
        if (parent == null) {
            table = newTable(capacity);
        } else {
            System.arraycopy(parent.keys, 0, keys, 0, parentCount);
            if (parent.table.length == capacity) {
                table = parent.table.clone();
            } else {
                table = newTable(capacity);
                for (int slot = 0; slot < parentCount; slot++) {
                    add(keys, table, mask, keys[slot], slot);
                }
            }
        }

        for (int j = 0; j < childNames.length; j++) {
            String name = childNames[j];
            int slot = parentCount + j;
            if (checkOrder && j > 0 && childNames[j - 1].compareTo(name) > 0) {
                Log.LOGGER.warn(MARKER, "Attribute name '{}' is out of order after '{}'",
                        name, childNames[j - 1]);
            }
            keys[slot] = name;
            int existing = add(keys, table, mask, name, slot);
            if (existing >= 0) {
                Log.LOGGER.warn(MARKER, "Duplicate attribute name '{}': keeping slot {}, ignoring slot {}",
                        name, existing, slot);
            }
        }

        this.keys = keys;
        this.table = table;
        this.mask = mask;
    }

    /**
     * Returns the number of slots, including slots that were appended for repeated names.
     */
    public int size() {
        return keys.length;
    }

    /**
     * Returns the name at the given slot.
     *
     * @throws IndexOutOfBoundsException oob
     */
    public @NonNull String get(int index) {
        return keys[index];
    }

    /**
     * Returns the name of every slot, parent names first.
     *
     * @return the internal array, not a copy
     * @hidden
     */
    @ApiStatus.Internal
    public @NonNull String @NonNull[] getKeys() {
        return keys;
    }

    /**
     * Looks up the slot of a name.
     *
     * @param name the name to look for
     * @return the slot, or {@code -1} if the name has no slot
     */
    public int indexOf(@NonNull String name) {
        int hash = name.hashCode();
        int pos = HashCommon.mix(hash) & mask;
        int slot;
        while ((slot = table[pos]) >= 0) {
            String k = keys[slot];
            if (hash == k.hashCode() && name.equals(k)) {
                return slot;
            }
            pos = (pos + 1) & mask;
        }
        return -1;
    }

    private static int @NonNull[] newTable(int capacity) {
        int[] table = new int[capacity];
        Arrays.fill(table, -1);
        return table;
    }

    /**
     * Inserts a slot unless the name is already present.
     *
     * @return the slot already holding the name, or {@code -1} if the slot was inserted
     */
    private static int add(@NonNull String @NonNull[] keys, int @NonNull[] table,
                           int mask, @NonNull String name, int slot) {
        int pos = HashCommon.mix(name.hashCode()) & mask;
        int existing;
        while ((existing = table[pos]) >= 0) {
            if (name.equals(keys[existing])) {
                return existing;
            }
            pos = (pos + 1) & mask;
        }
        table[pos] = slot;
        return -1;
    }
}
