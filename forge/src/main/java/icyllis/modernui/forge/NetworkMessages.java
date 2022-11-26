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

import icyllis.modernui.fragment.Fragment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Internal use.
 */
@ApiStatus.Internal
public sealed class NetworkMessages extends NetworkHandler {

    private static final int S2C_OPEN_MENU = 0;

    static NetworkHandler sNetwork;

    NetworkMessages() {
        super(ModernUIForge.location("network"), "360", true);
    }

    /*@Deprecated
    @Nonnull
    public static PacketDispatcher syncFood(float foodSaturationLevel, float foodExhaustionLevel) {
        FriendlyByteBuf buf = NetworkHandler.buffer(0);
        buf.writeFloat(foodSaturationLevel);
        buf.writeFloat(foodExhaustionLevel);
        return sNetwork.dispatcher(buf);
    }*/

    @SuppressWarnings("deprecation")
    static PacketBuffer openMenu(@Nonnull AbstractContainerMenu menu, @Nullable Consumer<FriendlyByteBuf> writer) {
        PacketBuffer buf = sNetwork.buffer(S2C_OPEN_MENU);
        buf.writeVarInt(menu.containerId);
        buf.writeVarInt(Registry.MENU.getId(menu.getType()));
        if (writer != null) {
            writer.accept(buf);
        }
        return buf;
    }

    static NetworkMessages client() {
        return new Client();
    }

    // this class doesn't load on dedicated server
    static final class Client extends NetworkMessages {

        static {
            assert (FMLEnvironment.dist.isClient());
        }

        private Client() {
        }

        @Override
        protected void handleClientMessage(int index,
                                           @Nonnull FriendlyByteBuf payload,
                                           @Nonnull Supplier<NetworkEvent.Context> source,
                                           @Nonnull BlockableEventLoop<?> looper) {
            /*case 0:
                    syncFood(payload, player);
                    break;*/
            if (index == S2C_OPEN_MENU) {
                openMenu(payload, source, looper);
            }
        }

        /*@Deprecated
        private static void syncFood(@Nonnull FriendlyByteBuf buffer, @Nonnull LocalPlayer player) {
            FoodData foodData = player.getFoodData();
            foodData.setSaturation(buffer.readFloat());
            ((AccessFoodData) foodData).setExhaustionLevel(buffer.readFloat());
        }*/

        @SuppressWarnings("deprecation")
        private static void openMenu(@Nonnull FriendlyByteBuf payload,
                                     @Nonnull Supplier<NetworkEvent.Context> source,
                                     @Nonnull BlockableEventLoop<?> looper) {
            final int containerId = payload.readVarInt();
            // No barrier, SAFE
            final MenuType<?> type = Registry.MENU.byIdOrThrow(payload.readVarInt());
            final ResourceLocation key = Registry.MENU.getKey(type);
            assert key != null;
            payload.retain();
            looper.execute(() -> {
                try {
                    final LocalPlayer p = getClientPlayer(source);
                    if (p != null) {
                        final AbstractContainerMenu menu = type.create(containerId, p.getInventory(), payload);
                        final OpenMenuEvent event = new OpenMenuEvent(menu);
                        ModernUIForge.post(key.getNamespace(), event);
                        final Fragment fragment = event.getFragment();
                        if (fragment == null) {
                            p.closeContainer(); // close server menu whatever it is
                        } else {
                            p.containerMenu = menu;
                            Minecraft.getInstance().setScreen(new MenuScreen<>(UIManager.getInstance(),
                                    fragment,
                                    menu,
                                    p.getInventory(),
                                    CommonComponents.EMPTY));
                        }
                    }
                } finally {
                    payload.release();
                }
            });
        }
    }
}
