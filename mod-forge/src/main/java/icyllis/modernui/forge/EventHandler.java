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

import icyllis.modernui.ModernUI;
import icyllis.modernui.forge.event.OpenMenuEvent;
import icyllis.modernui.graphics.BlurHandler;
import icyllis.modernui.graphics.RenderCore;
import icyllis.modernui.mcimpl.MuiRegistries;
import icyllis.modernui.mcimpl.TestMenu;
import icyllis.modernui.test.TestUI;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.ProgressOption;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Handles game server or client events from Forge event bus
 */
@Mod.EventBusSubscriber
final class EventHandler {

    @SubscribeEvent
    static void onRightClickItem(@Nonnull PlayerInteractEvent.RightClickItem event) {
        if (ModernUIForge.isDeveloperMode()) {
            if (event.getSide().isServer() && event.getItemStack().getItem() == Items.DIAMOND) {
                MuiHooks.openMenu(event.getPlayer(), TestMenu::new);
            }
        }
    }

    /*@SubscribeEvent
    static void onContainerClosed(PlayerContainerEvent.Close event) {

    }*/

    /**
     * Handles Modern UI events internally
     */
    @OnlyIn(Dist.CLIENT)
    static class Internal {

        @SubscribeEvent
        static void onMenuOpen(@Nonnull OpenMenuEvent event) {
            if (event.getMenu().getType() == MuiRegistries.TEST_MENU) {
                event.setApplicationUI(new TestUI());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class ModClient {

        @SubscribeEvent
        static void onRegistryModel(@Nonnull ModelRegistryEvent event) {
            ModelLoader.addSpecialModel(new ResourceLocation(ModernUI.ID, "item/project_builder_main"));
            ModelLoader.addSpecialModel(new ResourceLocation(ModernUI.ID, "item/project_builder_cube"));
        }

        @SubscribeEvent
        static void onBakeModel(@Nonnull ModelBakeEvent event) {
            Map<ResourceLocation, BakedModel> registry = event.getModelRegistry();
            replaceModel(registry, new ModelResourceLocation(
                            Objects.requireNonNull(MuiRegistries.PROJECT_BUILDER_ITEM.getRegistryName()), "inventory"),
                    m -> new ProjectBuilderModel(m, event.getModelLoader()));
        }

        private static void replaceModel(@Nonnull Map<ResourceLocation, BakedModel> modelRegistry,
                                         ModelResourceLocation location, @Nonnull Function<BakedModel, BakedModel> factory) {
            modelRegistry.put(location, factory.apply(modelRegistry.get(location)));
        }
    }

    /**
     * Handles game client events from Forge event bus
     */
    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = ModernUI.ID, value = Dist.CLIENT)
    static class Client {

        static ProgressOption NEW_GUI_SCALE;

        @SubscribeEvent
        static void onPlayerLogin(@Nonnull ClientPlayerNetworkEvent.LoggedInEvent event) {
            if (ModernUIForge.isDeveloperMode()) {
                LocalPlayer player = event.getPlayer();
                if (player != null && RenderCore.glCapabilitiesErrors > 0) {
                    player.sendMessage(new TextComponent("[Modern UI] There are " + RenderCore.glCapabilitiesErrors +
                            " GL capabilities that are not supported by your GPU, see debug.log for detailed info")
                            .withStyle(ChatFormatting.RED), Util.NIL_UUID);
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        static void onGuiOpenH(@Nonnull GuiOpenEvent event) {
            // TipTheScales is not good, and it also not compatible with OptiFine
            if (ModernUIForge.interceptTipTheScales) {
                if (event.getGui() instanceof VideoSettingsScreen) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        static void onGuiOpenL(@Nonnull GuiOpenEvent event) {
            BlurHandler.INSTANCE.count(event.getGui());
        }

        @SubscribeEvent
        static void onGuiInit(@Nonnull GuiScreenEvent.InitGuiEvent event) {
            if (event.getGui() instanceof VideoSettingsScreen) {
                NEW_GUI_SCALE.setMaxValue(MuiHooks.C.calcGuiScales() & 0xf);
            }
        }

        /*@SubscribeEvent(receiveCanceled = true)
        static void onGuiOpen(@Nonnull GuiOpenEvent event) {

        }

        @SubscribeEvent
        static void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {

        }

        @SubscribeEvent
        static void onClientTick(@Nonnull TickEvent.ClientTickEvent event) {

        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.KeyInputEvent event) {

        }

        @SubscribeEvent
        static void onGuiInit(GuiScreenEvent.InitGuiEvent event) {

        }

        @SubscribeEvent
        public static void onScreenStartMouseClicked(@Nonnull GuiScreenEvent.MouseClickedEvent.Pre event) {

        }

        @SubscribeEvent
        public static void onScreenStartMouseReleased(@Nonnull GuiScreenEvent.MouseReleasedEvent.Pre event) {

        }

        @SubscribeEvent
        public static void onScreenStartMouseDragged(@Nonnull GuiScreenEvent.MouseDragEvent.Pre event) {

        }*/
    }

    /*@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventHandler {

        @SubscribeEvent
        public static void setupCommon(FMLCommonSetupEvent event) {

        }

        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void setupClient(FMLClientSetupEvent event) {

        }

        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void registerSounds(@Nonnull RegistryEvent.Register<SoundEvent> event) {

        }

        @SubscribeEvent
        public static void registerContainers(@Nonnull RegistryEvent.Register<ContainerType<?>> event) {

        }

        @SubscribeEvent
        public static void onConfigChange(@Nonnull ModConfig.ModConfigEvent event) {

        }

        @SubscribeEvent
        public static void onLoadComplete(FMLLoadCompleteEvent event) {

        }
    }*/
}
