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

public abstract class AttributeFinder {

    public AttributeFinder(int start, int end) {
        this.start = start;
        this.end = end;
        current = start;
    }

    protected String currentNamespace;
    protected String currentAttribute;

    protected abstract void onGetAttribute(int index);

    private boolean first = true;
    private final int start;
    private final int end;
    private int current;

    // Returns the index or '-1' if not found
    public int find(String namespace, String attribute) {
        if (start >= end) {
            return -1;
        }

        if (first) {
            // One-time initialization. We do this here instead of the constructor
            // because the subclass we access in onGetAttribute() may not be
            // fully constructed.
            first = false;
            onGetAttribute(start);
        }

        while (current != end) {
            boolean namespaceMatch = currentNamespace.equals(namespace);
            int attributeCompare = currentAttribute.compareTo(attribute);
            if (namespaceMatch && attributeCompare > 0) {
                // The attribute we are looking was not found.
                break;
            }

            ++current;
            if (current != end) {
                onGetAttribute(current);
            }

            if (namespaceMatch && attributeCompare == 0) {
                // We found the attribute we were looking for.
                return current - 1;
            }
        }

        return -1;
    }
}
