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

package icyllis.modernui.screen;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This event occurred when the server requires the client to open a user
 * interface to display a container menu in a world, this event is cancelled
 * after setting the application screen. The menu is created on the client by
 * the registered {@link net.minecraftforge.fml.network.IContainerFactory factory},
 * which contains custom network data from server, you can set the application
 * screen through the data and the menu type.  For example:
 *
 * <pre>
 * &#64;SubscribeEvent
 * static void onOpenMenu(OpenMenuEvent event) {
 *     if (event.getMenu().getType() == Registration.TEST_MENU) {
 *         event.setScreen(new TestUI());
 *     }
 * }
 * </pre>
 * <p>
 * This event will be only posted to your own mod event bus. If no application
 * screen set along with this event, the server container menu will be closed.
 */
@Cancelable
@OnlyIn(Dist.CLIENT)
public class OpenMenuEvent extends Event implements IModBusEvent {

    @Nonnull
    private final AbstractContainerMenu mMenu;

    @Nullable
    private ScreenCallback mScreen;

    public OpenMenuEvent(@Nonnull AbstractContainerMenu menu) {
        mMenu = menu;
    }

    /**
     * Get the source of the event.
     *
     * @return the container menu
     */
    @Nonnull
    public AbstractContainerMenu getMenu() {
        return mMenu;
    }

    /**
     * Set the application UI for the menu. After calling this method,
     * the event will be canceled.
     *
     * @param screen the application screen
     */
    public void setScreen(@Nonnull ScreenCallback screen) {
        mScreen = screen;
        setCanceled(true);
    }

    @Nullable
    public ScreenCallback getScreen() {
        return mScreen;
    }
}
