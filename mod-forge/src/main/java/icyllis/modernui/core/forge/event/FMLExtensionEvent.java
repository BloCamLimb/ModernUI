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

package icyllis.modernui.core.forge.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Events for loading extensions in FML environment. These events are fired
 * on each mod bus, but with different context and lifecycle. Behaviors other
 * than description are not expected.
 */
@ApiStatus.Experimental
public class FMLExtensionEvent extends Event implements IModBusEvent {

    /**
     * Modern UI will try to create an extension application for each Forge mod,
     * but a linking is required.
     */
    @Cancelable
    public static class Linking extends FMLExtensionEvent {

        /**
         * Calling this method means you want Modern UI to create an extension
         * application for your mod. Otherwise, your mod can't interact with Modern UI.
         */
        public void link() {
            super.setCanceled(true);
        }
    }
}
