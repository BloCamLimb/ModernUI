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

package icyllis.modernui.api.template;

import icyllis.modernui.gui.element.IBase;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;

public interface IButtonT1B {

    IButtonT1B init(Function<Integer, Float> x, Function<Integer, Float> y, float w, float h, ResourceLocation texture, float u, float v, float scale, IntPredicate availability, int leader);

    void buildToPool(Consumer<IBase> pool);
}
