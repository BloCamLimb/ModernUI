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

import icyllis.modernui.fragment.Fragment;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Fragment manager to create a UI, contains some system properties and fragment factories to pass to UIManager.
 * Also a listener to listen system methods in UIManager.
 */
@Deprecated
public interface IFragmentManager {

    /**
     * Define the title of this gui screen, default is empty.
     * No need for you to override this method since title is no use in gui screens.
     * Notice that container screen's title is included in the network packet,
     * it won't use this title at all.
     * see {@link net.minecraftforge.fml.network.NetworkHooks#openGui(ServerPlayerEntity, INamedContainerProvider, Consumer)}
     *
     * @return screen title
     */
    @Nonnull
    default ITextComponent createScreenTitle() {
        return new StringTextComponent("");
    }

    /**
     * Used to define UI window position and size depend on game main window.
     * A small area on screen center is widely used in UIs with a container.
     *
     * @return UI window layout
     */
    @Nonnull
    default IWindowLayout createWindowLayout() {
        return new DefaultWindowLayout();
    }

    /**
     * Register fragment factories for fragment creation.
     * The map must contain a key of 0 as the main fragment in this UI,
     * which will be created immediately this UI is opened
     * and can't be null and can't be destroyed as well.
     *
     * @param map id to fragment map
     */
    void registerFragmentFactories(@Nonnull Map<Integer, Supplier<Fragment>> map);

    interface IWindowLayout {

        /**
         * Transform game window width to UI window left
         *
         * @return window left
         */
        @Nonnull
        default IntFunction<Integer> getLeft() {
            return i -> 0;
        }

        /**
         * Transform game window height to UI window top
         *
         * @return window top
         */
        @Nonnull
        default IntFunction<Integer> getTop() {
            return i -> 0;
        }

        /**
         * Transform game window width to UI window width
         *
         * @return window width
         */
        @Nonnull
        default IntFunction<Integer> getWidth() {
            return i -> i;
        }

        /**
         * Transform game window height to UI window height
         *
         * @return window height
         */
        @Nonnull
        default IntFunction<Integer> getHeight() {
            return i -> i;
        }
    }

    class DefaultWindowLayout implements IWindowLayout {

    }
}
