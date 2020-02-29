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

package icyllis.modernui.api.builder;

import icyllis.modernui.gui.element.IBase;
import icyllis.modernui.gui.element.Texture2D;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ITextureBuilder {

    /**
     * Set element relative position to given window size.
     * Set element size.
     * Set texture location.
     * Set texture position.
     * Set tint color in hex.
     * Set element scaling.
     * @param x x position
     * @param y x position
     * @param w element width
     * @param h element height
     * @param texture texture
     * @param u texture x
     * @param v texture y
     * @param tintRGBA tint color
     * @param scale scale
     * @return builder
     */
    ITextureBuilder init(Function<Integer, Float> x, Function<Integer, Float> y, float w, float h, ResourceLocation texture, float u, float v, int tintRGBA, float scale);

    /**
     * Build this element to pool
     */
    void buildToPool(Consumer<IBase> pool);

    /**
     * Build this element to pool with modifiers
     */
    void buildToPool(Consumer<IBase> pool, Consumer<Texture2D> consumer);

    /**
     * Return build instance
     */
    Texture2D buildForMe();
}
