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

package icyllis.modernui.gui.math;

import icyllis.modernui.gui.master.Widget;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.system.StorageManager;

import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Custom locator using JS
 */
public class ScriptLocator implements ILocator {

    private static final ScriptEngine ENGINE = StorageManager.INSTANCE.getScriptManager().getEngineByName("JavaScript");

    private String xScript = "hw / 2.0";

    private String yScript = "hh / 2.0";

    private String wScript = "16.0";

    private String hScript = "16.0";

    @Override
    public float getLocatedX(@Nullable Widget prev, float hostWidth) {
        if (prev != null) {
            ENGINE.put("x", prev.getLeft());
            ENGINE.put("pw", prev.getWidth());
        } else {
            ENGINE.put("x", 0);
            ENGINE.put("pw", 0);
        }
        ENGINE.put("hw", hostWidth);
        try {
            return ((Double) ENGINE.eval(xScript)).floatValue();
        } catch (ScriptException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS X Locator parse failed, {}", e.getMessage());
        } catch (NullPointerException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS X Locator parse failed, script can't be empty");
        }
        return hostWidth / 2.0f;
    }

    @Override
    public float getLocatedY(@Nullable Widget prev, float hostHeight) {
        if (prev != null) {
            ENGINE.put("y", prev.getLeft());
            ENGINE.put("ph", prev.getWidth());
        } else {
            ENGINE.put("y", 0);
            ENGINE.put("ph", 0);
        }
        ENGINE.put("hh", hostHeight);
        try {
            return ((Double) ENGINE.eval(yScript)).floatValue();
        } catch (ScriptException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS Y Locator parse failed, {}", e.getMessage());
        } catch (NullPointerException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS Y Locator parse failed, script can't be empty");
        }
        return hostHeight / 2.0f;
    }

    @Override
    public float getSizedW(@Nullable Widget prev, float hostWidth) {
        if (prev != null) {
            ENGINE.put("pw", prev.getWidth());
        } else {
            ENGINE.put("pw", 0);
        }
        ENGINE.put("hw", hostWidth);
        try {
            return ((Double) ENGINE.eval(wScript)).floatValue();
        } catch (ScriptException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS W Locator parse failed, {}", e.getMessage());
        } catch (NullPointerException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS W Locator parse failed, script can't be empty");
        }
        return 16.0f;
    }

    @Override
    public float getSizedH(@Nullable Widget prev, float hostHeight) {
        if (prev != null) {
            ENGINE.put("ph", prev.getWidth());
        } else {
            ENGINE.put("ph", 0);
        }
        ENGINE.put("hh", hostHeight);
        try {
            return ((Double) ENGINE.eval(hScript)).floatValue();
        } catch (ScriptException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS H Locator parse failed, {}", e.getMessage());
        } catch (NullPointerException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS H Locator parse failed, script can't be empty");
        }
        return 16.0f;
    }
}
