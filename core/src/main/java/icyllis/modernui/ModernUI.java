/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * The core class of the client side of Modern UI
 */
public class ModernUI {

    public static final String ID = "modernui";
    public static final String NAME_CPT = "ModernUI";

    public static final Logger LOGGER = LogManager.getLogger(NAME_CPT);
    public static final Marker MARKER = MarkerManager.getMarker("Core");

    public ModernUI() {
        checkJava();
    }

    private static void checkJava() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "Java version is missing");
        } else if (javaVersion.startsWith("1.8")) {
            try {
                int update = Integer.parseInt(javaVersion.split("_")[1].split("-")[0]);
                if (update < 201) {
                    throw new RuntimeException(
                            "Modern UI requires Java 1.8.0_271 or above to run, your version is Java " + javaVersion);
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                ModernUI.LOGGER.warn(ModernUI.MARKER, "Failed to check java version: {}", javaVersion, e);
            }
        }
    }
}
