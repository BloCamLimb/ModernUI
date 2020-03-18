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

package icyllis.modernui.gui.module;

import icyllis.modernui.gui.element.Background;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.window.ConfirmWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PopupLockDifficulty implements IGuiModule {

    private List<IElement> elements = new ArrayList<>();

    private List<IGuiEventListener> listeners = new ArrayList<>();

    public PopupLockDifficulty(Runnable callback) {
        elements.add(new Background(4));
        Consumer<IGuiEventListener> consumer = s -> listeners.add(s);
        elements.add(new ConfirmWindow(consumer, "Confirm Lock World Difficulty",
                new TranslationTextComponent("difficulty.lock.question", new TranslationTextComponent("options.difficulty." + Minecraft.getInstance().world.getWorldInfo().getDifficulty().getTranslationKey())).getFormattedText(),
                "Lock", callback, 5));
    }

    @Override
    public List<IElement> getElements() {
        return elements;
    }

    @Override
    public List<? extends IGuiEventListener> getEventListeners() {
        return listeners;
    }
}
