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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

final class ServerHandler {

    static final ServerHandler INSTANCE = new ServerHandler();

    boolean started = false;

    // time in millis that server will auto-shutdown
    private long shutdownTime = 0;

    private long nextShutdownNotify = 0;
    private final long[] shutdownNotifyTimes = new long[]{
            1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 60000, 300000, 600000, 1800000};

    @SubscribeEvent
    void onStart(@Nonnull ServerStartedEvent event) {
        started = true;
        determineShutdownTime();
    }

    @SubscribeEvent
    void onStop(@Nonnull ServerStoppingEvent event) {
        started = false;
    }

    void determineShutdownTime() {
        if (!started) {
            return;
        }
        if (Config.COMMON.autoShutdown.get()) {
            Calendar calendar = Calendar.getInstance();
            int current =
                    calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
            int target = Integer.MAX_VALUE;
            for (String s : Config.COMMON.shutdownTimes.get()) {
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
                        ModernUI.LOGGER.warn(ModernUI.MARKER, "Wrong time format while setting auto-shutdown time, " +
                                "input: {}", s);
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    ModernUI.LOGGER.error(ModernUI.MARKER, "Wrong time format while setting auto-shutdown time, " +
                            "input: {}", s, e);
                }
            }
            if (target < Integer.MAX_VALUE && target > current) {
                shutdownTime = System.currentTimeMillis() + (target - current) * 1000L;
                ModernUI.LOGGER.debug(ModernUI.MARKER, "Server will shutdown at {}",
                        SimpleDateFormat.getDateTimeInstance().format(new Date(shutdownTime)));
                nextShutdownNotify = shutdownNotifyTimes[shutdownNotifyTimes.length - 1];
            } else {
                shutdownTime = 0;
            }
        } else {
            shutdownTime = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    void onLastEndTick(@Nonnull TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && shutdownTime > 0) {
            long countdown = shutdownTime - System.currentTimeMillis();
            sendShutdownNotification(countdown);
            if (countdown <= 0) {
                ServerLifecycleHooks.getCurrentServer().halt(false);
            }
        }
    }

    private void sendShutdownNotification(long countdown) {
        if (countdown < nextShutdownNotify) {
            do {
                int index = Arrays.binarySearch(shutdownNotifyTimes, nextShutdownNotify);
                if (index > 0) {
                    nextShutdownNotify = shutdownNotifyTimes[index - 1];
                } else {
                    nextShutdownNotify = 0;
                    break;
                }
            } while (countdown < nextShutdownNotify);
            long l = Math.round(countdown / 1000D);
            final String key;
            final String str;
            if (l > 60) {
                l = Math.round(l / 60D);
                key = "message.modernui.server_shutdown_min";
                str = "Server will shutdown in %d minutes";
            } else {
                key = "message.modernui.server_shutdown_sec";
                str = "Server will shutdown in %d seconds";
            }
            ModernUI.LOGGER.info(ModernUI.MARKER, String.format(str, l));
            final Component component = new TranslatableComponent(key, l).withStyle(ChatFormatting.LIGHT_PURPLE);
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(p -> p.displayClientMessage(component, true));
        }
    }
}
