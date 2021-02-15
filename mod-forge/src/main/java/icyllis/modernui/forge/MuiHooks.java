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

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.network.IContainerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Exposed for external calls
 */
public final class MuiHooks {

    private MuiHooks() {
    }

    /**
     * Get the lifecycle of current server.
     *
     * @return {@code true} if server started
     */
    public static boolean isServerStarted() {
        return ServerHandler.INSTANCE.started;
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @throws ClassCastException this method is not called on logic server
     * @see #openMenu(Player, MenuConstructor, Consumer)
     */
    public static void openMenu(@Nonnull Player player, @Nonnull MenuConstructor constructor) {
        openMenu((ServerPlayer) player, constructor, (Consumer<FriendlyByteBuf>) null);
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @param pos         a block pos to send to client, this will be passed to
     *                    the menu supplier (IContainerFactory) that registered on client
     * @throws ClassCastException this method is not called on logic server
     * @see #openMenu(Player, MenuConstructor, Consumer)
     * @see net.minecraftforge.common.extensions.IForgeContainerType#create(IContainerFactory)
     */
    public static void openMenu(@Nonnull Player player, @Nonnull MenuConstructor constructor, @Nonnull BlockPos pos) {
        openMenu((ServerPlayer) player, constructor, buf -> buf.writeBlockPos(pos));
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @param writer      a data writer to send additional data to client, this will be passed
     *                    to the menu supplier (IContainerFactory) that registered on client
     * @throws ClassCastException this method is not called on logic server
     * @see net.minecraftforge.common.extensions.IForgeContainerType#create(IContainerFactory)
     */
    public static void openMenu(@Nonnull Player player, @Nonnull MenuConstructor constructor, @Nullable Consumer<FriendlyByteBuf> writer) {
        openMenu((ServerPlayer) player, constructor, writer);
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @param writer      a data writer to send additional data to client, this will be passed
     *                    to the menu supplier (IContainerFactory) that registered on client
     * @see net.minecraftforge.common.extensions.IForgeContainerType#create(IContainerFactory)
     */
    public static void openMenu(@Nonnull ServerPlayer player, @Nonnull MenuConstructor constructor, @Nullable Consumer<FriendlyByteBuf> writer) {
        // do the same thing as ServerPlayer.openMenu()
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        player.nextContainerCounter();
        int containerId = player.containerCounter;
        AbstractContainerMenu menu = constructor.createMenu(containerId, player.inventory, player);
        if (menu == null) {
            return;
        }
        NetMessages.menu(containerId, Registry.MENU.getId(menu.getType()), writer).sendToPlayer(player);
        menu.addSlotListener(player);
        player.containerMenu = menu;
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, menu));
    }

    @OnlyIn(Dist.CLIENT)
    public static final class C {

        private C() {
        }

        public static int calcGuiScales() {
            return calcGuiScales(Minecraft.getInstance().getWindow());
        }

        public static int calcGuiScales(@Nonnull Window window) {
            return calcGuiScales(window.getWidth(), window.getHeight());
        }

        public static int calcGuiScales(int framebufferWidth, int framebufferHeight) {
            int w = framebufferWidth / 16;
            int h = framebufferHeight / 9;

            if ((w & 1) == 1) {
                w++;
            }
            if ((h & 1) == 1) {
                h++;
            }

            double base = Math.min(w, h);
            double high = Math.max(w, h);

            int min;
            int max = Mth.clamp((int) (base / 26), 1, 6);
            if (max > 1) {
                int i = (int) (base / 64);
                int j = (int) (high / 64);
                min = Mth.clamp(j != i ? Math.min(i, j) + 1 : i, 2, 6);
            } else {
                min = 1;
            }

            int best;
            if (min > 1) {
                double b = base > 150 ? 40 : base > 100 ? 36 : 32;
                int i = (int) (base / b);
                int j = (int) (high / b);
                double v1 = base / (i * 32);
                if (v1 > 1.25 || j > i) {
                    best = Math.min(max, i + 1);
                } else {
                    best = Math.min(max, i);
                }
            } else {
                best = 1;
            }

            return min << 8 | best << 4 | max;
        }

        /* Screen */
        /*public static int getScreenBackgroundColor() {
            return (int) (BlurHandler.INSTANCE.getBackgroundAlpha() * 255.0f) << 24;
        }*/

        /* Minecraft */
        /*public static void displayInGameMenu(boolean usePauseScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.currentScreen == null) {
                // If press F3 + Esc and is single player and not open LAN world
                if (usePauseScreen && minecraft.isIntegratedServerRunning() && minecraft.getIntegratedServer() != null && !minecraft.getIntegratedServer().getPublic()) {
                    minecraft.displayGuiScreen(new IngameMenuScreen(false));
                    minecraft.getSoundHandler().pause();
                } else {
                    //UIManager.INSTANCE.openGuiScreen(new TranslationTextComponent("menu.game"), IngameMenuHome::new);
                    minecraft.displayGuiScreen(new IngameMenuScreen(true));
                }
            }
        }*/
    }
}
