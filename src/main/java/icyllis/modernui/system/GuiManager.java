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

package icyllis.modernui.system;

import icyllis.modernui.api.global.IContainerFactory;
import icyllis.modernui.api.manager.IGuiManager;
import icyllis.modernui.api.global.IModuleFactory;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.ModernUIScreen;
import icyllis.modernui.gui.master.ModernUIScreenG;
import javafx.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public enum GuiManager implements IGuiManager {
    INSTANCE;

    /*private final Map<ResourceLocation, IContainerFactory> CONTAINERS = new HashMap<>();
    private final Map<ResourceLocation, Function<PacketBuffer, IGuiScreen>> SCREENS = new HashMap<>();*/

    private final Map<ResourceLocation, Pair<IContainerFactory<? extends Container>, Consumer<IModuleFactory>>> CONTAINER_SCREENS = new HashMap<>();

    @SuppressWarnings("ConstantConditions")
    public void openContainerScreen(ResourceLocation id, int windowId, PacketBuffer extraData) {
        if (CONTAINER_SCREENS.containsKey(id)) {
            Pair<IContainerFactory<? extends Container>, Consumer<IModuleFactory>> pair = CONTAINER_SCREENS.get(id);
            GlobalModuleManager.INSTANCE.setExtraData(new PacketBuffer(extraData.copy()));
            IContainerFactory factory = pair.getKey();
            Container container = factory.create(windowId, Minecraft.getInstance().player.inventory, extraData);
            Minecraft.getInstance().player.openContainer = container;
            Minecraft.getInstance().displayGuiScreen(new ModernUIScreenG<>(pair.getValue(), container));
        } else {
            Minecraft.getInstance().player.connection.sendPacket(new CCloseWindowPacket(windowId));
        }
    }

    @Override
    public <M extends Container> void registerContainerGui(ResourceLocation id, IContainerFactory<M> containerFactory, Consumer<IModuleFactory> moduleFactory) {
        if (CONTAINER_SCREENS.containsKey(id)) {
            ModernUI.LOGGER.error("Duplicated ID when registering container gui ({})", id.toString());
        } else {
            CONTAINER_SCREENS.put(id, new Pair<>(containerFactory, moduleFactory));
        }
    }

    @Override
    public void openGui(Consumer<IModuleFactory> factoryConsumer) {
        Minecraft.getInstance().displayGuiScreen(new ModernUIScreen(factoryConsumer));
    }

}
