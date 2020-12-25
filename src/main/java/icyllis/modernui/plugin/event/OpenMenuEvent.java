/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.plugin.event;

import icyllis.modernui.view.ApplicationUI;
import net.minecraft.inventory.container.Container;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.GenericEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Cancelable
@OnlyIn(Dist.CLIENT)
public class OpenMenuEvent<T extends Container> extends GenericEvent<T> {

    @Nonnull
    private final T menu;

    @Nullable
    private ApplicationUI applicationUI;

    @SuppressWarnings("unchecked")
    public OpenMenuEvent(@Nonnull T menu) {
        super((Class<T>) menu.getClass());
        this.menu = menu;
    }

    @Nonnull
    public T getMenu() {
        return menu;
    }

    public void setApplicationUI(@Nonnull ApplicationUI applicationUI) {
        this.applicationUI = applicationUI;
        setCanceled(true);
    }

    @Nullable
    public ApplicationUI getApplicationUI() {
        return applicationUI;
    }
}
