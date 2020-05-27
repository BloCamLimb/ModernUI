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

import icyllis.modernui.system.ModernUI;
import icyllis.modernui.system.StorageManager;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class JSLocator implements ILocator {

    private static final ScriptEngine ENGINE = StorageManager.INSTANCE.getScriptManager().getEngineByName("JavaScript");

    private String xScript = "width / 2.0";

    private String yScript = "height / 2.0";

    @Override
    public float getLocatedX(float width) {
        ENGINE.put("width", width);
        try {
            return ((Double) ENGINE.eval(xScript)).floatValue();
        } catch (ScriptException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS X Locator parse failed, {}", e.getMessage());
        } catch (NullPointerException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS X Locator parse failed, script can't be empty");
        }
        return width / 2.0f;
    }

    @Override
    public float getLocatedY(float height) {
        ENGINE.put("height", height);
        try {
            return ((Double) ENGINE.eval(yScript)).floatValue();
        } catch (ScriptException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS Y Locator parse failed, {}", e.getMessage());
        } catch (NullPointerException e) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "JS Y Locator parse failed, script can't be empty");
        }
        return height / 2.0f;
    }
}
