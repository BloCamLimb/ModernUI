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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.view.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public abstract class Module implements IModule, IHost {

    private final UIManager manager = UIManager.getInstance();

    protected final Minecraft minecraft = Minecraft.getInstance();

    //private final Canvas plotter = new Canvas();

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

    private boolean autoLoseFocus = true;

    public Module() {

    }

    @Override
    public final void draw(float time) {
        RenderSystem.pushMatrix();
        if (overDraw) {
            drawChild(time);
            for (IDrawable drawable : drawables) {
                //drawable.draw(plotter, time);
            }
        } else {
            for (IDrawable drawable : drawables) {
                //drawable.draw(plotter, time);
            }
            drawChild(time);
        }
        RenderSystem.popMatrix();
    }

    protected void drawChild(float time) {

    }

    /**
     * Make this module draw over child module
     */
    protected void makeOverDraw() {
        overDraw = true;
    }

    /**
     * Keyboard listeners will auto lose their focus when click on other place
     * Call this to disable the function
     */
    protected void disableAutoLoseFocus() {
        autoLoseFocus = false;
    }

    /*protected void parseWidgets(String modid, Consumer<WidgetContainer> containerConsumer) {
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
            try {
                HOST.set(e.widget, this);
            } catch (IllegalAccessException ex) {
                ModernUI.LOGGER.fatal(UIManager.MARKER, "I'm fine");
                ex.printStackTrace();
            }
            addWidget(e.widget);
            containerConsumer.accept(e);
        });
    }*/

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
            //drawable.resize(width, height);
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
    public int getGameWidth() {
        return manager.getScreenWidth();
    }

    @Override
    public int getGameHeight() {
        return manager.getScreenHeight();
    }

    @Override
    public double getAbsoluteMouseX() {
        return manager.getCursorX();
    }

    @Override
    public double getAbsoluteMouseY() {
        return manager.getCursorY();
    }

    @Override
    public double getRelativeMouseX() {
        return manager.getCursorX();
    }

    @Override
    public double getRelativeMouseY() {
        return manager.getCursorY();
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
        return manager.getElapsedTicks();
    }

    @Override
    public float getAnimationTime() {
        return manager.getDrawingTime();
    }

    @Override
    public void refocusMouseCursor() {

    }

    public void playSound(@Nonnull SoundEvent soundEvent) {
        minecraft.getSoundHandler().play(SimpleSound.master(soundEvent, 1.0f));
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
    public void setDraggable(@Nonnull IDraggable draggable) {
        this.draggable = draggable;
    }

    @Nullable
    @Override
    public IDraggable getDraggable() {
        return draggable;
    }

    @Override
    public void setKeyboardListener(@Nullable IKeyboardListener keyboardListener) {
        minecraft.keyboardListener.enableRepeatEvents(keyboardListener != null);
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
            if (!result) {
                if (listener.updateMouseHover(mouseX, mouseY)) {
                    UIEditor.INSTANCE.setHoveredWidget(listener);
                    result = true;
                }
            } else {
                //listener.setMouseHoverExit();
            }
            if (!result) {
                UIEditor.INSTANCE.setHoveredWidget(null);
            }
        }
        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (autoLoseFocus) {
            IKeyboardListener k = getKeyboardListener();
            if (k instanceof IMouseListener) {
                /*if (!((IMouseListener) k).isMouseHovered()) {
                    setKeyboardListener(null);
                    return true;
                }*/
            }
            IMouseListener m = null;
            for (IMouseListener listener : mouseListeners) {
                /*if (listener.isMouseHovered() && listener.mouseClicked(mouseX, mouseY, mouseButton)) {
                    m = listener;
                    break;
                }*/
            }
            if (m != null) {
                if (k != null && (getKeyboardListener() != k || m != k)) {
                    setKeyboardListener(null);
                }
                return true;
            }
            if (getKeyboardListener() != null) {
                setKeyboardListener(null);
                return true;
            }
        } else {
            for (IMouseListener listener : mouseListeners) {
                /*if (listener.isMouseHovered() && listener.mouseClicked(mouseX, mouseY, mouseButton)) {
                    return true;
                }*/
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (draggable != null) {
            draggable.stopMouseDragging();
            draggable = null;
            return true;
        }
        for (IMouseListener listener : mouseListeners) {
            /*if (listener.isMouseHovered() && listener.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }*/
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (IMouseListener listener : mouseListeners) {
            /*if (listener.isMouseHovered() && listener.mouseScrolled(, amount)) {
                return true;
            }*/
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
