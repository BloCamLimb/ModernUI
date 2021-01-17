/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.mcimpl;

import icyllis.modernui.ModernUI;

/**
 * An abstract class represents the core class of Modern UI as a Minecraft mod.
 * Injecting all needed things by implemented platform makes Modern UI work
 * in Minecraft. We provide many unique features and APIs for Minecraft.
 */
public abstract class ModernUIMod extends ModernUI {

    protected static ModernUIMod instance;

    public ModernUIMod() {
        instance = this;
    }

    public static ModernUIMod getMod() {
        return instance;
    }

    public abstract void warnSetup(String key, Object... args);
}
