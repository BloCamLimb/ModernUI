/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.resources;

import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.Contract;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * A resource identifier. This can uniquely identify a resource in the ResourceTable.
 * <p>
 * A resource identifier consists of three parts: namespace, type, and entry. Its single
 * string representation is "namespace:type/entry" or "type/entry" (if namespace is empty).
 * For a valid resource identifier, type and entry can not be empty.
 *
 * @param namespace the domain of resource
 * @param type      the class of resource
 * @param entry     the name of resource entry
 */
@Immutable
public record ResourceId(@NonNull String namespace, @NonNull String type, @NonNull String entry)
        implements Comparable<ResourceId> {

    public ResourceId(@NonNull String namespace, @NonNull String type, @NonNull String entry) {
        this.namespace = Objects.requireNonNull(namespace);
        this.type = Objects.requireNonNull(type);
        this.entry = Objects.requireNonNull(entry);
    }

    @NonNull
    @Contract("_ -> new")
    public static ResourceId parse(@NonNull CharSequence name) {
        return parse(name, "", "");
    }

    @NonNull
    @Contract("_, _, _ -> new")
    public static ResourceId parse(@NonNull CharSequence name,
                                   @NonNull String fallbackType,
                                   @NonNull String fallbackNamespace) {
        String namespace = null;
        String type = null;
        int start = 0;
        int end = name.length();
        char c;
        if (start != end && ((c = name.charAt(start)) == '@' || c == '?')) {
            start++;
        }
        int current = start;
        while (current != end) {
            c = name.charAt(current);
            if (type == null && c == '/') {
                type = name.subSequence(start, current).toString();
                start = current + 1;
            } else if (namespace == null && c == ':') {
                namespace = name.subSequence(start, current).toString();
                start = current + 1;
            }
            current++;
        }
        if (type == null) {
            type = fallbackType;
        }
        if (namespace == null) {
            namespace = fallbackNamespace;
        }
        return new ResourceId(namespace, type, name.subSequence(start, end).toString());
    }

    /**
     * Returns a new, single string representation of this resource identifier.
     */
    @NonNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return namespace.isEmpty() ? type + "/" + entry
                : namespace + ":" + type + "/" + entry;
    }

    /**
     * Compare strings lexicographically, by namespace, then type, then entry.
     *
     * @param o the object to be compared.
     */
    @Override
    public int compareTo(@NonNull ResourceId o) {
        int res = this.namespace.compareTo(o.namespace);
        if (res != 0) return res;
        res = this.type.compareTo(o.type);
        if (res != 0) return res;
        return this.entry.compareTo(o.entry);
    }

    public static int comparePair(@NonNull String lhsNamespace, @NonNull String lhsName,
                                  @NonNull String rhsNamespace, @NonNull String rhsName) {
        int res = lhsNamespace.compareTo(rhsNamespace);
        if (res != 0) return res;
        return lhsName.compareTo(rhsName);
    }
}
