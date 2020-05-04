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

package icyllis.modernui.gui.master;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.editor.WidgetParser;
import icyllis.modernui.editor.WidgetContainer;
import icyllis.modernui.system.ConstantsLibrary;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public abstract class Module implements IModule, IHost {

    private static Field HOST;

    static {
        try {
            HOST = Widget.class.getDeclaredField("host");
            HOST.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private final GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    private final Canvas canvas = new Canvas();

    private final List<IDrawable> drawables = new ArrayList<>();

    private final List<IMouseListener> mouseListeners = new ArrayList<>();

    @Nullable
    private IDraggable draggable;

    @Nullable
    private IKeyboardListener keyboardListener;

    @Nullable
    private ResourceLocation widgetLocation;

    /**
     * If true, this module will draw over child module
     */
    private boolean overDraw = false;

    public Module() {

    }

    @Override
    public final void draw(float time) {
        RenderSystem.pushMatrix();
        if (overDraw) {
            drawChild(time);
            for (IDrawable drawable : drawables) {
                drawable.draw(canvas, time);
            }
        } else {
            for (IDrawable drawable : drawables) {
                drawable.draw(canvas, time);
            }
            drawChild(time);
        }
        RenderSystem.popMatrix();
    }

    protected void drawChild(float time) {

    }

    protected void makeOverDraw() {
        overDraw = true;
    }

    /**
     * Parse widgets from json file
     * @param modid modid
     * @param containerConsumer for editor
     */
    protected void parseWidgets(String modid, Consumer<WidgetContainer> containerConsumer) {
        String[] names = ConstantsLibrary.SPLIT_BY_CAPS.apply(getClass().getSimpleName());

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            builder.append(names[i].toLowerCase(Locale.ROOT));
            if (i != names.length - 1)
                builder.append('_');
        }
        builder.append(".json");

        ResourceLocation location = new ResourceLocation(modid, "gui/" + builder.toString());
        if (widgetLocation != null) {
            return;
        }
        widgetLocation = location;
        List<WidgetContainer> list = WidgetParser.INSTANCE.parseWidgets(location);
        list.forEach(e -> {
            try {
                Field field = getClass().getDeclaredField(e.name);
                field.setAccessible(true);
                field.set(this, e.widget);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {

            }
            //TODO use builder
            try {
                HOST.set(e.widget, this);
            } catch (IllegalAccessException ex) {
                ModernUI.LOGGER.fatal(GlobalModuleManager.MARKER, "I'm fine");
                ex.printStackTrace();
            }
            addWidget(e.widget);
            containerConsumer.accept(e);
        });
    }

    /*private void parseWidgets(ResourceLocation location, BiConsumer<String, Widget> callback) {
        if (widgetLocation != null) {
            return;
        }
        widgetLocation = location;
        Map<String, Widget> map = new HashMap<>();
        Type type = new TypeToken<Map<String, Widget>>(){}.getType();

        try (BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(
                                Minecraft.getInstance().getResourceManager().getResource(location).getInputStream()
                        )
                )
        ) {
            map = gson.fromJson(br, type);
        } catch (IOException e) {
            ModernUI.LOGGER.debug(GlobalModuleManager.MARKER, "Failed to load gui widgets, {}", location);
            e.printStackTrace();
        }
        map.forEach(callback);
    }

    @Nullable
    public ResourceLocation getWidgetLocation() {
        return widgetLocation;
    }*/

    @Override
    public void resize(int width, int height) {
        for (IDrawable drawable : drawables) {
            drawable.resize(width, height);
        }
    }

    @Override
    public void tick(int ticks) {
        for (IDrawable drawable : drawables) {
            drawable.tick(ticks);
        }
    }

    public void addDrawable(IDrawable drawable) {
        drawables.add(drawable);
    }

    public void addWidget(IWidget widget) {
        drawables.add(widget);
        mouseListeners.add(widget);
    }

    @Override
    public int getWindowWidth() {
        return manager.getWindowWidth();
    }

    @Override
    public int getWindowHeight() {
        return manager.getWindowHeight();
    }

    @Override
    public double getAbsoluteMouseX() {
        return manager.getMouseX();
    }

    @Override
    public double getAbsoluteMouseY() {
        return manager.getMouseY();
    }

    @Override
    public double getRelativeMouseX() {
        return manager.getMouseX();
    }

    @Override
    public double getRelativeMouseY() {
        return manager.getMouseY();
    }

    @Override
    public float toAbsoluteX(float rx) {
        return rx;
    }

    @Override
    public float toAbsoluteY(float ry) {
        return ry;
    }

    @Override
    public int getElapsedTicks() {
        return manager.getTicks();
    }

    @Override
    public void refocusMouseCursor() {
        manager.refreshMouse();
    }

    public void playSound(SoundEvent soundEvent) {
        Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(soundEvent, 1.0f));
    }

    /**
     * Called when upper module group want to switch another child module
     * and this onBack return false
     * First value is the delay to switch to another module
     * Second value is that after new module switched, the duration that current
     * module should keep (only) to draw. (oh sorry, my statement is too vague)
     * Both two values must be positive number or 0 (no delay)
     * Unit: ticks
     *
     * @return a array with length of 2
     */
    public int[] onChangingModule() {
        return new int[]{0, 0};
    }

    @Override
    public void setDraggable(@Nullable IDraggable draggable) {
        this.draggable = draggable;
    }

    @Nullable
    @Override
    public IDraggable getDraggable() {
        return draggable;
    }

    @Override
    public void setKeyboardListener(@Nullable IKeyboardListener keyboardListener) {
        Minecraft.getInstance().keyboardListener.enableRepeatEvents(keyboardListener != null);
        if (this.keyboardListener != null) {
            this.keyboardListener.stopKeyboardListening();
        }
        this.keyboardListener = keyboardListener;
    }

    @Nullable
    @Override
    public IKeyboardListener getKeyboardListener() {
        return keyboardListener;
    }

    @Override
    public boolean mouseMoved(double mouseX, double mouseY) {
        boolean result = false;
        for (IMouseListener listener : mouseListeners) {
            if (!result && listener.updateMouseHover(mouseX, mouseY)) {
                result = true;
            } else {
                listener.setMouseHoverExit();
            }
        }
        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (IMouseListener listener : mouseListeners) {
            if (listener.isMouseHovered() && listener.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (draggable != null) {
            draggable.stopDragging();
            return true;
        }
        for (IMouseListener listener : mouseListeners) {
            if (listener.isMouseHovered() && listener.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (IMouseListener listener : mouseListeners) {
            if (listener.isMouseHovered() && listener.mouseScrolled(amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (draggable != null) {
            return draggable.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyboardListener != null) {
            return keyboardListener.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyboardListener != null) {
            return keyboardListener.keyReleased(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (keyboardListener != null) {
            return keyboardListener.charTyped(codePoint, modifiers);
        }
        return false;
    }

}
