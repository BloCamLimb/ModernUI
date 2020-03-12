/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.api.manager;

import icyllis.modernui.api.global.IContainerFactory;
import icyllis.modernui.api.global.IModuleFactory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;

public interface IGuiManager {

    /**
     * Register a gui with container on client side
     *  @param id registry name
     * @param title screen title
     * @param containerFactory factory to create container
     * @param moduleFactory gui modules, ID is determined by order, the first one would be main module
     */
    <M extends Container> void registerContainerGui(ResourceLocation id, ITextComponent title, IContainerFactory<M> containerFactory, Consumer<IModuleFactory> moduleFactory);

}
