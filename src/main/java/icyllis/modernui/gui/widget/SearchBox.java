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

package icyllis.modernui.gui.widget;

import icyllis.modernui.gui.master.Module;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class SearchBox extends TextField {

    private Runnable enterOperation = () -> {};

    @Nullable
    private Function<String, Boolean> listener;

    public SearchBox(Module module, float width) {
        super(module, width, 12);
        super.setDecoration(f -> new Frame(this, null, 0xffc0c0c0));
    }

    @Deprecated
    @Override
    public void setListener(@Nonnull Consumer<TextField> listener, boolean runtime) {
        throw new RuntimeException();
    }

    @Deprecated
    @Override
    public void setDecoration(@Nonnull Function<TextField, Decoration> function) {
        throw new RuntimeException();
    }

    /**
     * Set listener
     * @param listener function return false to set color to red
     */
    public void setListener(@Nullable Function<String, Boolean> listener) {
        this.listener = listener;
    }

    public void setEnterOperation(Runnable enterOperation) {
        this.enterOperation = enterOperation;
    }

    @Override
    public void onTextChanged() {
        if (listener != null) {
            if (listener.apply(getText())) {
                ((TextField.Frame) Objects.requireNonNull(getDecoration())).setColor(0xffc0c0c0);
            } else {
                ((TextField.Frame) Objects.requireNonNull(getDecoration())).setColor(0xffff5555);
            }
        }
    }

    /**
     * Stop calling listener
     */
    @Override
    public void stopKeyboardListening() {
        editing = false;
        timer = 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            enterOperation.run();
            return true;
        }
        return false;
    }
}
