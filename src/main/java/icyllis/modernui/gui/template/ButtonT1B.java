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

package icyllis.modernui.gui.template;

import icyllis.modernui.api.ModernUI_API;
import icyllis.modernui.api.template.IButtonT1B;
import icyllis.modernui.gui.master.GlobalAnimationManager;
import icyllis.modernui.gui.master.GlobalElementBuilder;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;

public class ButtonT1B extends ButtonT1 implements IButtonT1B {

    private boolean lock = false;

    @Override
    public IButtonT1B init(Function<Integer, Float> x, Function<Integer, Float> y, float w, float h, ResourceLocation texture, float u, float v, float scale, IntPredicate availability, int leader) {
        this.texture = GlobalElementBuilder.INSTANCE.texture().buildForMe();
        this.texture.init(x, y, w, h, texture, u, v, 0x00808080, scale);
        GlobalAnimationManager.INSTANCE
                .create(a -> a
                                .setInit(0)
                                .setTarget(1.0f)
                                .setTiming(3.0f)
                                .setDelay(1.0f),
                        r -> this.texture.alpha = r);
        Consumer<Boolean> c = GlobalAnimationManager.INSTANCE
                .createHS(a -> a
                                .setInit(0.5f)
                                .setTarget(1.0f)
                                .setTiming(4.0f),
                        r -> this.texture.tintR = this.texture.tintG = this.texture.tintB = r);
        listener.addHoverOn(s -> c.accept(true));
        listener.addHoverOff(s -> {
            if(!lock)
                c.accept(false);
        });
        ModernUI_API.INSTANCE.getModuleManager().addModuleSwitchEvent(i -> {
            lock = availability.test(i);
            if(!lock)
                c.accept(false);
        });
        initEventListener(b -> b.setPos(x, y).setRectShape(w * scale, h * scale));
        listener.addLeftClick(q -> ModernUI_API.INSTANCE.getModuleManager().switchModule(leader));
        return this;
    }
}
