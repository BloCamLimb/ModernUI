/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleableRes;
import org.lwjgl.system.MemoryUtil;

import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryUtil.*;

public class ResourceUtils {

    @Nullable
    public static String findAttribute(@NonNull XMLStreamReader reader,
                                       @NonNull String name) {
        var res = reader.getAttributeValue(null, name);
        if (res != null) {
            return res.trim();
        }
        return null;
    }

    @Nullable
    public static String findNonEmptyAttribute(@NonNull XMLStreamReader reader,
                                               @NonNull String name) {
        var res = reader.getAttributeValue(null, name);
        if (res != null) {
            var trim = res.trim();
            if (!trim.isEmpty()) {
                return trim;
            }
        }
        return null;
    }

    @NonNull
    public static ResourceValues.Reference parseXmlAttributeName(@NonNull String s) {
        var name = s.trim();
        int start = 0;

        var ref = new ResourceValues.Reference();
        if (!name.isEmpty() && name.charAt(0) == '*') {
            ref.private_reference = true;
            start++;
        }

        String namespace;
        String entry;

        int i = name.indexOf(':', start);
        if (i >= 0) {
            namespace = name.substring(0, i);
            entry = name.substring(i + 1);
        } else {
            namespace = "";
            entry = name.substring(start);
        }
        ref.name = new ResourceId(namespace, Resource.getTypeName(Resource.TYPE_ATTR), entry);
        return ref;
    }

    /**
     * Find the index of the attribute in the sorted keys (namespace=>attribute pairs).
     * Returns -1 if not found.
     */
    public static int indexOfAttribute(@NonNull String[] keys,
                                       @NonNull String namespace, @NonNull String attribute) {
        assert (keys.length & 1) == 0;
        int low = 0;
        int high = (keys.length >> 1) - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = ResourceId.comparePair(keys[mid << 1], keys[(mid << 1) + 1],
                    namespace, attribute);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        return -1;
    }

    @Nullable
    public static List<String> decomposePath(@NonNull String path) {
        List<String> result = new ArrayList<>();

        int start = 0, end;
        boolean stop = false;

        for (;;) {
            end = path.indexOf('/', start);
            if (end == -1) {
                end = path.length();
                stop = true;
            }

            String segment = path.substring(start, end);
            if (!isValidPathSegment(segment)) {
                return null;
            }

            result.add(segment);

            if (stop) {
                return result;
            }

            start = end + 1;
        }
    }

    @NonNull
    public static Path resolvePath(@NonNull Path base, @NonNull List<String> segments) {
        int size = segments.size();
        switch (size) {
            case 0:
                return base;
            case 1:
                return base.resolve(segments.get(0));
            default:
                String[] more = new String[size - 1];
                for (int i = 1; i < size; i++) {
                    more[i - 1] = segments.get(i);
                }
                return base.resolve(base.getFileSystem().getPath(segments.get(0), more));
        }
    }

    public static boolean isValidPathSegment(@NonNull String segment) {
        if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
            return false;
        }
        for (int i = 0, end = segment.length(); i < end; i++) {
            if (!isValidPathSegmentCharacter(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidPathSegmentCharacter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '.' || c == '_';
    }

    /**
     * Allocates native memory and read buffered resource. The memory <b>MUST</b> be
     * manually freed by {@link MemoryUtil#memFree(Buffer)}. This method can read up
     * to 2GB. This method does NOT close the channel.
     *
     * @param channel where to read input from
     * @return the native pointer to {@code unsigned char *data}
     * @throws IOException some errors occurred while reading
     */
    @NonNull
    public static ByteBuffer readIntoNativeBuffer(@NonNull ReadableByteChannel channel) throws IOException {
        ByteBuffer p = null;
        try {
            if (channel instanceof final SeekableByteChannel ch) {
                long rem = ch.size() - ch.position() + 1;
                p = memAlloc((int) Math.min(rem,
                        Integer.MAX_VALUE));
                //noinspection StatementWithEmptyBody
                while (ch.read(p) > 0)
                    ;
            } else {
                p = memAlloc(4096);
                while (channel.read(p) != -1) {
                    if (p.hasRemaining()) {
                        continue;
                    }
                    long cap = p.capacity();
                    if (cap == Integer.MAX_VALUE) {
                        break;
                    }
                    p = memRealloc(p, (int) Math.min(cap + (cap >> 1), // grow 50%
                            Integer.MAX_VALUE));
                }
            }
        } catch (Throwable t) {
            memFree((Buffer) p);
            throw t;
        }
        return p;
    }
}
