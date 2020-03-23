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

package icyllis.modernui.api.manager;

import icyllis.modernui.gui.animation.IAnimation;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.master.TickEvent;
import icyllis.modernui.gui.widget.StandardEventListener;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.network.PacketBuffer;

import java.util.function.IntConsumer;

public interface IModuleManager {

    /**
     * Switch to specific module
     * @param newID target id
     */
    void switchModule(int newID);

    /**
     * Open a popup window, a special module
     * @param popup popup module
     */
    void openPopup(IGuiModule popup);

    /**
     * Close current popup
     */
    void closePopup();

    /**
     * Add an element to CURRENT module.
     * @param element element to add
     */
    //void addElement(IElement element);

    /**
     * Add a module event that called when switching modules
     * to CURRENT module.
     * @param event int consumer
     */
    //void addModuleEvent(IntConsumer event);

    /**
     * Add a sub event listener to listen mouse and keyboard event
     * to CURRENT module. Generally use {@link StandardEventListener}
     * @param listener event listener
     */
    //void addEventListener(IGuiEventListener listener);

    /**
     * Add animation to global animation pool
     * @param animation animation to add
     */
    void addAnimation(IAnimation animation);

    void addTickEvent(TickEvent event);

    /**
     * Refresh current mouse position, will recall mouseMoved()
     */
    void refreshMouse();

    /**
     * Get current time
     * @return time
     */
    float getAnimationTime();

    /**
     * Get current ticks
     * @return ticks
     */
    int getTicks();

    int getWindowHeight();

    /**
     * Get extra data from server side
     * @return packet buffer
     */
    PacketBuffer getExtraData();
}
