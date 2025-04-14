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

    public ResourceId {
        //noinspection ConstantValue
        if (namespace == null | type == null | entry == null) {
            throw new NullPointerException();
        }
    }

    @NonNull
    @Contract("_ -> new")
    public static ResourceId parse(@NonNull String name) {
        return parse(name, "", "");
    }

    @NonNull
    @Contract("_, _, _ -> new")
    public static ResourceId parse(@NonNull String name,
                                   @NonNull String fallbackType,
                                   @NonNull String fallbackNamespace) {
        String namespace = null;
        String type = null;
        int start = 0;
        int end = name.length();
        if (start != end && name.charAt(start) == '@') {
            start++;
        }
        int current = start;
        while (current != end) {
            char c = name.charAt(current);
            if (type == null && c == '/') {
                type = name.substring(start, current);
                start = current + 1;
                if (namespace != null) {
                    break;
                }
            } else if (namespace == null && c == ':') {
                namespace = name.substring(start, current);
                start = current + 1;
                if (type != null) {
                    break;
                }
            }
            current++;
        }
        if (type == null) {
            type = fallbackType;
        }
        if (namespace == null) {
            namespace = fallbackNamespace;
        }
        return new ResourceId(namespace, type, name.substring(start, end));
    }

    /**
     * Helper method to create an attribute resource id, where the resource type
     * name will be "attr".
     */
    @NonNull
    @Contract("_, _ -> new")
    public static ResourceId attr(@NonNull String namespace, @NonNull String name) {
        return new ResourceId(namespace, "attr", name);
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

    @NonNull
    @Contract(pure = true)
    public static String toString(@NonNull String namespace, @NonNull String type, @NonNull String entry) {
        return namespace.isEmpty() ? type + "/" + entry
                : namespace + ":" + type + "/" + entry;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object instanceof ResourceId that)
            return type.equals(that.type) &&
                    entry.equals(that.entry) &&
                    namespace.equals(that.namespace);
        return false;
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + entry.hashCode();
        return result;
    }

    /**
     * Compare strings lexicographically, by namespace, then type, then entry.
     *
     * @param o the object to be compared.
     */
    @Override
    public int compareTo(@NonNull ResourceId o) {
        int result = this.namespace.compareTo(o.namespace);
        if (result == 0)
            result = this.type.compareTo(o.type);
        if (result == 0)
            result = this.entry.compareTo(o.entry);
        return result;
    }

    @SuppressWarnings("StringEquality")
    public static int comparePair(@NonNull String lhsNamespace, @NonNull String lhsName,
                                  @NonNull String rhsNamespace, @NonNull String rhsName) {
        // Namespace strings are interned, so compare identity first can be more efficient
        int res = lhsNamespace == rhsNamespace ? 0 : lhsNamespace.compareTo(rhsNamespace);
        return res != 0 ? res : lhsName.compareTo(rhsName);
    }
}
