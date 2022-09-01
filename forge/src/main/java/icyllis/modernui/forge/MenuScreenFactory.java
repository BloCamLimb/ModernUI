/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.IContainerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The factory interface is used to create menu screens with a main {@link Fragment}.
 * The factory is invoked when the server requires the client to open an application
 * screen to interact with a container menu. The menu instance is created on the
 * client with {@link IContainerFactory#create(int, Inventory, FriendlyByteBuf)},
 * which contains custom network data from server. For example:
 * <pre>{@code
 * @SubscribeEvent
 * static void setupClient(FMLClientSetupEvent event) {
 *     event.enqueueWork(() -> {
 *         MenuScreens.register(MyRegistry.MY_MENU.get(), MenuScreenFactory.create(menu -> new MyFragment()));
 *     });
 * }
 * }</pre>
 * <p>
 * All menu types are registered with {@link net.minecraftforge.registries.RegisterEvent}
 * and {@link net.minecraftforge.registries.ForgeRegistries#MENU_TYPES}, use
 * {@link net.minecraftforge.common.extensions.IForgeMenuType#create(IContainerFactory)}
 * to create your registry entries when the register event is fired.
 *
 * @see net.minecraftforge.network.NetworkHooks#openScreen(ServerPlayer, MenuProvider, Consumer)
 */
@FunctionalInterface
public interface MenuScreenFactory<T extends AbstractContainerMenu> extends
        MenuScreens.ScreenConstructor<T, AbstractContainerScreen<T>> {

    /**
     * Helper method that down-casts the screen factory.
     *
     * @param factory the factory
     * @param <T>     the menu type
     * @return the factory
     */
    static <T extends AbstractContainerMenu> MenuScreenFactory<T> create(MenuScreenFactory<T> factory) {
        return factory;
    }

    @Nonnull
    @Override
    default AbstractContainerScreen<T> create(@Nonnull T menu,
                                              @Nonnull Inventory inventory,
                                              @Nonnull Component title) {
        return new MenuScreen<>(UIManager.getInstance(),
                Objects.requireNonNull(createFragment(menu)),
                menu,
                inventory,
                title);
    }

    /**
     * Creates a new {@link Fragment} for the given menu.
     * <p>
     * Optionally, the main {@link Fragment} can implement {@link ScreenCallback}
     * to describe the screen properties.
     *
     * @param menu the container menu
     * @return the main fragment
     */
    @Nonnull
    Fragment createFragment(T menu);
}
