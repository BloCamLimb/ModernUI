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

package icyllis.modernui.resources;

import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.Contract;

import javax.xml.stream.*;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses an XML file for resources and adds them to a ResourceTable.
 */
public class ResourceParser {

    public static class ParsedResource {
        public Resource.ResourceName name;
        public int id;
        public boolean staged_api = false;
        public boolean allow_new = false;
        public ResourceValues.Value value;
        public List<ParsedResource> child_resources;

        @Override
        public String toString() {
            return "ParsedResource{" +
                    "name=" + name +
                    ", id=" + id +
                    ", staged_api=" + staged_api +
                    ", allow_new=" + allow_new +
                    ", value=" + value +
                    ", child_resources=" + child_resources +
                    '}';
        }
    }

    //@formatter:off
    @Contract(pure = true)
    public static int FormatTypeNoEnumOrFlags(@NonNull String s) {
        return switch (s) {
            case "reference"    -> ResourceTypes.TYPE_REFERENCE;
            case "string"       -> ResourceTypes.TYPE_STRING;
            case "integer"      -> ResourceTypes.TYPE_INTEGER;
            case "boolean"      -> ResourceTypes.TYPE_BOOLEAN;
            case "color"        -> ResourceTypes.TYPE_COLOR;
            case "float"        -> ResourceTypes.TYPE_FLOAT;
            case "dimension"    -> ResourceTypes.TYPE_DIMENSION;
            case "fraction"     -> ResourceTypes.TYPE_FRACTION;
            default             -> 0;
        };
    }

    @Contract(pure = true)
    public static int FormatType(@NonNull String s) {
        return switch (s) {
            case "enum"     -> ResourceTypes.TYPE_ENUM;
            case "flags"    -> ResourceTypes.TYPE_FLAGS;
            default         -> FormatTypeNoEnumOrFlags(s);
        };
    }
    //@formatter:on

    public static int FormatAttribute(@NonNull String s) {
        int mask = 0;
        for (var part : s.split("\\|")) {
            var trim = part.trim();
            int type = FormatType(trim);
            if (type == 0) {
                return 0;
            }
            mask |= type;
        }
        return mask;
    }

    public boolean Parse(XMLStreamReader reader) {
        try {
            if (reader.nextTag() == XMLResourceReader.START_ELEMENT &&
                    reader.getNamespaceURI() == null &&
                    reader.getLocalName().equals("resources")) {
                return ParseResources(reader);
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static void main(String[] args) throws XMLStreamException {
        boolean success = new ResourceParser().Parse(XMLInputFactory.newFactory().createXMLStreamReader(new StringReader("""
                <?xml version="1.0" encoding="utf-8"?>
                <resources>
                </resources>
                """)));
        System.out.println(success);
    }

    public boolean ParseResources(XMLStreamReader reader) {
        return true;
    }

    public boolean Attr(XMLStreamReader reader, ParsedResource out) {
        return Attr0(reader, out, false);
    }

    public boolean Attr0(XMLStreamReader reader, ParsedResource out_resource, boolean weak) {

        int type_mask = 0;

        String maybe = ResourceUtils.findAttribute(reader, "format");
        if (maybe != null) {
            type_mask = FormatAttribute(maybe);
            if (type_mask == 0) {
                return false;
            }
        }

        boolean hasMin = false;
        boolean hasMax = false;
        int min = 0;
        int max = 0;

        maybe = ResourceUtils.findAttribute(reader, "min");
        if (maybe != null) {
            try {
                min = Integer.decode(maybe);
                hasMin = true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        maybe = ResourceUtils.findAttribute(reader, "max");
        if (maybe != null) {
            try {
                max = Integer.decode(maybe);
                hasMax = true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        if ((hasMin || hasMax) && (type_mask & ResourceTypes.TYPE_INTEGER) == 0) {
            return false;
        }

        var attribute = new ResourceValues.Attribute(
                type_mask != 0 ? type_mask : ResourceTypes.TYPE_ANY);
        if (hasMin) {
            attribute.min_int = min;
        }
        if (hasMax) {
            attribute.max_int = max;
        }
        attribute.weak = weak;
        out_resource.value = attribute;
        return true;
    }

    /*public boolean DeclareStyleable(XMLStreamReader reader, ParsedResource out_resource) {
        out_resource.name = new Resource.ResourceName();
        out_resource.name.setType(Resource.TYPE_STYLEABLE);

        var styleable = new ResourceValues.Styleable();

        try {
            while (reader.nextTag() != XMLResourceReader.END_ELEMENT) {
                var element_namespace = reader.getNamespaceURI();
                var element_name = reader.getLocalName();
                if (element_namespace == null && element_name.equals("attr")) {
                    var maybe = ResourceUtils.findNonEmptyAttribute(reader, "name");
                    if (maybe == null) {
                        return false;
                    }
                    var ref = ResourceUtils.parseXmlAttributeName(maybe);

                    ParsedResource resource = new ParsedResource();
                    resource.name = ref.name;

                    if (!Attr0(reader, resource, true)) {
                        return false;
                    }

                    if (styleable.entries == null) {
                        styleable.entries = new ArrayList<>();
                    }
                    styleable.entries.add(ref);

                    ResourceValues.Attribute attr = (ResourceValues.Attribute) resource.value;

                    if (attr.type_mask != ResourceTypes.TYPE_ANY) {
                        if (out_resource.child_resources == null) {
                            out_resource.child_resources = new ArrayList<>();
                        }
                        out_resource.child_resources.add(resource);
                    }
                } else {
                    return false;
                }
                if (reader.nextTag() != XMLResourceReader.END_ELEMENT) {
                    return false;
                }
            }
        } catch (XMLStreamException e) {
            return false;
        }

        out_resource.value = styleable;
        return true;
    }*/
}
