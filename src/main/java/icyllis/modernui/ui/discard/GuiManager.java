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

package icyllis.modernui.ui.discard;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@Deprecated
@OnlyIn(Dist.CLIENT)
public enum GuiManager {
    INSTANCE;

    /*private final Map<ResourceLocation, Pair<ITextComponent, Consumer<IModuleFactory>>> SCREENS = new HashMap<>();

    private final Map<ResourceLocation, Triple<ITextComponent, IContainerFactory<? extends Container>, Supplier<IModule>>> CONTAINER_SCREENS = new HashMap<>();

    @SuppressWarnings("ConstantConditions")
    public void openContainerScreen(ResourceLocation id, int windowId, PacketBuffer extraData) {
        ScreenManager.registerFactory();
        if (CONTAINER_SCREENS.containsKey(id)) {
            Triple<ITextComponent, IContainerFactory<? extends Container>, Consumer<IModuleFactory>> triple = CONTAINER_SCREENS.get(id);
            GlobalModuleManager.INSTANCE.setExtraData(new PacketBuffer(extraData.copy()));
            ITextComponent title = triple.getLeft();
            IContainerFactory<?> factory = triple.getMiddle();
            Container container = factory.create(windowId, Minecraft.getInstance().player.inventory, extraData);
            Minecraft.getInstance().player.openContainer = container;
            Minecraft.getInstance().displayGuiScreen(new ModernContainerScreen<>(title, container, triple.getRight()));
        } else {
            Minecraft.getInstance().player.connection.sendPacket(new CCloseWindowPacket(windowId));
        }
    }

    @Override
    public <M extends Container> void registerContainerGui(ResourceLocation id, ITextComponent title, IContainerFactory<M> containerFactory, Consumer<IModuleFactory> moduleFactory) {
        if (CONTAINER_SCREENS.containsKey(id)) {
            ModernUI.LOGGER.error("Duplicated ID when registering container gui ({})", id.toString());
        } else {
            CONTAINER_SCREENS.put(id, new ImmutableTriple<>(title, containerFactory, moduleFactory));
        }
    }*/

}
