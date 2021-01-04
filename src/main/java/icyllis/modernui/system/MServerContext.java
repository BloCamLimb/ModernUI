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

package icyllis.modernui.system;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;

/**
 * Modern UI server service, both for dedicated server or integrated server
 */
public final class MServerContext {

    static boolean serverStarted = false;

    // time in millis that server will auto-shutdown
    private static long shutdownTime = 0;

    private static long nextShutdownNotifyTime = 0;
    private static final long[] shutdownNotifyTimes = new long[]{
            1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 60000, 300000, 600000, 1800000};

    static void determineShutdownTime() {
        if (Config.COMMON_CONFIG.autoShutdown.get()) {
            Calendar calendar = Calendar.getInstance();
            int current = calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
            int target = Integer.MAX_VALUE;
            for (String s : Config.COMMON_CONFIG.shutdownTimes.get()) {
                try {
                    String[] s1 = s.split(":", 2);
                    int h = Integer.parseInt(s1[0]);
                    int m = Integer.parseInt(s1[1]);
                    if (h >= 0 && h < 24 && m >= 0 && m < 60) {
                        int t = h * 3600 + m * 60;
                        if (t < current) {
                            t += 86400;
                        }
                        target = Math.min(t, target);
                    } else {
                        ModernUI.LOGGER.error(ModernUI.MARKER, "Wrong time format while setting auto-shutdown time, input: {}", s);
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    ModernUI.LOGGER.error(ModernUI.MARKER, "Wrong time format while setting auto-shutdown time, input: {}", s, e);
                }
            }
            if (target < Integer.MAX_VALUE && target > current) {
                shutdownTime = System.currentTimeMillis() + (target - current) * 1000L;
                ModernUI.LOGGER.debug(ModernUI.MARKER, "Server will shutdown at {}",
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(shutdownTime)));
                nextShutdownNotifyTime = shutdownNotifyTimes[shutdownNotifyTimes.length - 1];
            } else {
                shutdownTime = 0;
            }
        } else {
            shutdownTime = 0;
        }
    }

    static void sendShutdownNotification(long countdown) {
        if (countdown < nextShutdownNotifyTime) {
            while (countdown < nextShutdownNotifyTime) {
                int index = Arrays.binarySearch(shutdownNotifyTimes, nextShutdownNotifyTime);
                if (index > 0) {
                    nextShutdownNotifyTime = shutdownNotifyTimes[index - 1];
                } else if (index == 0) {
                    nextShutdownNotifyTime = 0;
                    break;
                } else {
                    nextShutdownNotifyTime = shutdownNotifyTimes[0];
                }
            }
            long l = Math.round(countdown / 1000D);
            String text;
            if (l > 60) {
                l = Math.round(l / 60D);
                text = "Server will shutdown in " + l + " minutes";
            } else {
                text = "Server will shutdown in " + l + " seconds";
            }
            ModernUI.LOGGER.info(ModernUI.MARKER, text);
            ITextComponent component = new StringTextComponent(text).mergeStyle(TextFormatting.LIGHT_PURPLE);
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(p -> p.sendStatusMessage(component, true));
        }
    }

    static long getShutdownTime() {
        return shutdownTime;
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @throws ClassCastException this method is not called on server thread
     * @see #openMenu(PlayerEntity, IContainerProvider, Consumer)
     */
    public static void openMenu(@Nonnull PlayerEntity player, @Nonnull IContainerProvider constructor) {
        openMenu((ServerPlayerEntity) player, constructor, (Consumer<PacketBuffer>) null);
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @param pos         a data writer to send a block pos to client, this will be passed to
     *                    the menu supplier (IContainerFactory) that registered on client
     * @throws ClassCastException this method is not called on server thread
     * @see #openMenu(PlayerEntity, IContainerProvider, Consumer)
     * @see net.minecraftforge.common.extensions.IForgeContainerType#create(IContainerFactory)
     */
    public static void openMenu(@Nonnull PlayerEntity player, @Nonnull IContainerProvider constructor, @Nonnull BlockPos pos) {
        openMenu((ServerPlayerEntity) player, constructor, buf -> buf.writeBlockPos(pos));
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @param writer      a data writer to send additional data to client, this will be passed
     *                    to the menu supplier (IContainerFactory) that registered on client
     * @throws ClassCastException this method is not called on server thread
     * @see net.minecraftforge.common.extensions.IForgeContainerType#create(IContainerFactory)
     */
    public static void openMenu(@Nonnull PlayerEntity player, @Nonnull IContainerProvider constructor, @Nullable Consumer<PacketBuffer> writer) {
        openMenu((ServerPlayerEntity) player, constructor, writer);
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
    public static void openMenu(@Nonnull ServerPlayerEntity player, @Nonnull IContainerProvider constructor, @Nullable Consumer<PacketBuffer> writer) {
        // do the same thing as ServerPlayer.openMenu()
        if (player.openContainer != player.container) {
            player.closeScreen();
        }
        player.getNextWindowId();
        int containerId = player.currentWindowId;
        Container menu = constructor.createMenu(containerId, player.inventory, player);
        if (menu == null) {
            return;
        }
        NetMessages.menu(containerId, Registry.MENU.getId(menu.getType()), writer).sendToPlayer(player);
        menu.addListener(player);
        player.openContainer = menu;
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, menu));
    }
}
