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
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.screen.BlurHandler;
import icyllis.modernui.screen.OpenMenuEvent;
import icyllis.modernui.test.TestMenu;
import icyllis.modernui.test.TestUI;
import icyllis.modernui.textmc.TextLayoutEngine;
import net.minecraft.client.ProgressOption;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
                MForgeCompat.openMenu(event.getPlayer(), TestMenu::new);
            }
        }
    }

    /*@SubscribeEvent
    static void onContainerClosed(PlayerContainerEvent.Close event) {

    }*/

    @OnlyIn(Dist.CLIENT)
    static class ModClientDev {

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
                    baseModel -> new ProjectBuilderModel(baseModel, event.getModelLoader()));
        }

        private static void replaceModel(@Nonnull Map<ResourceLocation, BakedModel> modelRegistry,
                                         @Nonnull ModelResourceLocation location,
                                         @Nonnull Function<BakedModel, BakedModel> replacer) {
            modelRegistry.put(location, replacer.apply(modelRegistry.get(location)));
        }
    }

    /**
     * Handles game client events from Forge event bus
     */
    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = ModernUI.ID, value = Dist.CLIENT)
    static class Client {

        static ProgressOption NEW_GUI_SCALE;

        @Nullable
        private static Screen sCapturedVideoSettingsScreen;

        /*@SubscribeEvent
        static void onPlayerLogin(@Nonnull ClientPlayerNetworkEvent.LoggedInEvent event) {
            if (ModernUIForge.isDeveloperMode()) {
                LocalPlayer player = event.getPlayer();
                if (player != null && RenderCore.glCapabilitiesErrors > 0) {
                    player.sendMessage(new TextComponent("[Modern UI] There are " + RenderCore.glCapabilitiesErrors +
                            " GL capabilities that are not supported by your GPU, see debug.log for detailed info")
                            .withStyle(ChatFormatting.RED), Util.NIL_UUID);
                }
            }
        }*/

        @SubscribeEvent(priority = EventPriority.HIGH)
        static void onGuiOpenH(@Nonnull GuiOpenEvent event) {
            // TipTheScales is not good, and it also not compatible with OptiFine
            if (ModernUIForge.sInterceptTipTheScales) {
                if (event.getGui() instanceof VideoSettingsScreen) {
                    sCapturedVideoSettingsScreen = event.getGui();
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        static void onGuiOpenL(@Nonnull GuiOpenEvent event) {
            BlurHandler.INSTANCE.count(event.getGui());
            // This event should not be cancelled
            if (sCapturedVideoSettingsScreen != null) {
                event.setGui(sCapturedVideoSettingsScreen);
                sCapturedVideoSettingsScreen = null;
            }
        }

        @SubscribeEvent
        static void onGuiInit(@Nonnull GuiScreenEvent.InitGuiEvent event) {
            if (event.getGui() instanceof VideoSettingsScreen && NEW_GUI_SCALE != null) {
                NEW_GUI_SCALE.setMaxValue(MForgeCompat.calcGuiScales() & 0xf);
            }
        }

        @SubscribeEvent
        static void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                RenderCore.flushRenderCalls();
            }
        }

        @SubscribeEvent
        static void onClientTick(@Nonnull TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                TextLayoutEngine.getInstance().tick();
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
