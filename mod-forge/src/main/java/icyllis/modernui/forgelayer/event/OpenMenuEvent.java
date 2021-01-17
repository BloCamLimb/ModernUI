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

package icyllis.modernui.forgelayer.event;

import icyllis.modernui.view.ApplicationUI;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This event occurred when the server requires the client to open a user
 * interface to display a container menu in a world, this event is cancelled
 * after setting the application UI. The menu is created on the client by registered
 * {@link net.minecraftforge.fml.network.IContainerFactory factory}, which
 * contains custom network data from server, you can set the application UI
 * through the data and the menu type.  For example:
 *
 * <pre>
 * &#64;SubscribeEvent
 * static void onMenuOpen(OpenMenuEvent event) {
 *     if (event.getMenu().getType() == Registration.TEST_MENU) {
 *         event.setApplicationUI(new TestUI());
 *     }
 * }
 * </pre>
 *
 * If no application UI set along with this event, the server container menu
 * will be closed.
 */
@Cancelable
@OnlyIn(Dist.CLIENT)
public class OpenMenuEvent extends Event {

    @Nonnull
    private final AbstractContainerMenu menu;

    @Nullable
    private ApplicationUI applicationUI;

    public OpenMenuEvent(@Nonnull AbstractContainerMenu menu) {
        this.menu = menu;
    }

    /**
     * Get the source of the event.
     *
     * @return container menu
     */
    @Nonnull
    public AbstractContainerMenu getMenu() {
        return menu;
    }

    /**
     * Set the application UI for the menu. After calling this method,
     * the event will be canceled.
     *
     * @param applicationUI the application user interface
     */
    public void setApplicationUI(@Nonnull ApplicationUI applicationUI) {
        this.applicationUI = applicationUI;
        setCanceled(true);
    }

    @Nullable
    public ApplicationUI getApplicationUI() {
        return applicationUI;
    }
}
