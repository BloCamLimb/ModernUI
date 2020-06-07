/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.layout;

import icyllis.modernui.system.ModernUI;
import icyllis.modernui.system.StorageManager;
import icyllis.modernui.ui.test.IViewRect;
import icyllis.modernui.ui.master.UIManager;

import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Custom layout using js
 */
public class ScriptLayout implements ILayout {

    private static final ScriptEngine ENGINE = StorageManager.INSTANCE.getScriptManager().getEngineByName("JavaScript");

    @Nullable
    private String xScript = null;

    @Nullable
    private String yScript = null;

    @Nullable
    private String wScript = null;

    @Nullable
    private String hScript = null;

    public ScriptLayout(@Nullable String xScript, @Nullable String yScript, @Nullable String wScript, @Nullable String hScript) {
        this.xScript = xScript;
        this.yScript = yScript;
        this.wScript = wScript;
        this.hScript = hScript;
    }

    @Override
    public int getLayoutX(IViewRect prev, IViewRect parent) {
        ENGINE.put("prev", prev);
        ENGINE.put("parent", parent);
        if (xScript != null) {
            try {
                return ((Number) ENGINE.eval(xScript)).intValue();
            } catch (ScriptException e) {
                ModernUI.LOGGER.fatal(UIManager.MARKER, "JS X Locator parse failed, {}", e.getMessage());
            } catch (NullPointerException ignored) {
                ModernUI.LOGGER.fatal(UIManager.MARKER, "JS X Locator parse failed, script can't be empty");
            }
        }
        return parent.getWidth() >> 1;
    }

    @Override
    public int getLayoutY(IViewRect prev, IViewRect parent) {
        ENGINE.put("prev", prev);
        ENGINE.put("parent", parent);
        if (yScript != null) {
            try {
                return ((Number) ENGINE.eval(yScript)).intValue();
            } catch (ScriptException e) {
                ModernUI.LOGGER.fatal(UIManager.MARKER, "JS Y Locator parse failed, {}", e.getMessage());
            } catch (NullPointerException e) {
                ModernUI.LOGGER.fatal(UIManager.MARKER, "JS Y Locator parse failed, script can't be empty");
            }
        }
        return parent.getHeight() >> 1;
    }

    @Override
    public int getLayoutWidth(IViewRect prev, IViewRect parent) {
        ENGINE.put("prev", prev);
        ENGINE.put("parent", parent);
        if (wScript != null) {
            try {
                return ((Number) ENGINE.eval(wScript)).intValue();
            } catch (ScriptException e) {
                ModernUI.LOGGER.fatal(UIManager.MARKER, "JS Width Locator parse failed, {}", e.getMessage());
            } catch (NullPointerException e) {
                ModernUI.LOGGER.fatal(UIManager.MARKER, "JS Width Locator parse failed, script can't be empty");
            }
        }
        return 16;
    }

    @Override
    public int getLayoutHeight(IViewRect prev, IViewRect parent) {
        ENGINE.put("prev", prev);
        ENGINE.put("parent", parent);
        if (hScript != null) {
            try {
                return ((Number) ENGINE.eval(hScript)).intValue();
            } catch (ScriptException e) {
                ModernUI.LOGGER.fatal(UIManager.MARKER, "JS Height Locator parse failed, {}", e.getMessage());
            } catch (NullPointerException e) {
                ModernUI.LOGGER.fatal(UIManager.MARKER, "JS Height Locator parse failed, script can't be empty");
            }
        }
        return 16;
    }
}
