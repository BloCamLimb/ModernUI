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

package icyllis.modernui.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.*;

/**
 * Mod class. INTERNAL.
 *
 * @author BloCamLimb
 */
@Mod(ModernUIForge.ID)
public final class ModernUIForge {

    public static final String ID = "modernui"; // as well as the namespace
    public static final String NAME_CPT = "ModernUI";

    public static final Logger LOGGER = LogManager.getLogger(NAME_CPT);
    public static final Marker MARKER = MarkerManager.getMarker("Core");

    // mod-loading thread
    public ModernUIForge() {
        if (FMLEnvironment.production && ModernUIForge.class.getSigners() == null) {
            LOGGER.warn(MARKER, "Signature is missing");
        }

        Config.init();

        LOGGER.info(MARKER, "Initialized Modern UI");
    }
}
