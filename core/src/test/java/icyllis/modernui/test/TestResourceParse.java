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

package icyllis.modernui.test;

import icyllis.modernui.resources.ResourceParser;
import icyllis.modernui.resources.ResourceUtils;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.Configurator;

import javax.xml.stream.*;
import javax.xml.stream.events.StartElement;
import java.io.StringReader;

public class TestResourceParse {

    public static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        Configurator.setRootLevel(Level.INFO);

        try {
            var reader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader("""
                    <!-- Base attributes available to CheckBoxPreference. -->
                    <declare-styleable name="CheckBoxPreference">
                        <!-- The summary for the Preference in a PreferenceActivity screen when the
                             CheckBoxPreference is checked. If separate on/off summaries are not
                             needed, the summary attribute can be used instead. -->
                        <attr name="summaryOn" format="string" />
                        <!-- The summary for the Preference in a PreferenceActivity screen when the
                             CheckBoxPreference is unchecked. If separate on/off summaries are not
                             needed, the summary attribute can be used instead. -->
                        <attr name="summaryOff" format="string" />
                        <!-- The state (true for on, or false for off) that causes dependents to be disabled. By default,
                             dependents will be disabled when this is unchecked, so the value of this preference is false. -->
                        <attr name="disableDependentsState" format="boolean" />
                    </declare-styleable>"""));
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamReader.START_ELEMENT) {
                    if (reader.getLocalName().equals("declare-styleable")) {
                        ResourceParser parser = new ResourceParser();
                        ResourceParser.ParsedResource resource = new ResourceParser.ParsedResource();
                        if (parser.DeclareStyleable(reader, resource)) {
                            LOGGER.info(resource);
                        }
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}
