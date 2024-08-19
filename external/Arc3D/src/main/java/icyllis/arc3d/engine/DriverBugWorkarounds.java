/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import java.util.Map;

public class DriverBugWorkarounds {

    // check higher bit first
    public static final int DEFAULT = 0;
    public static final int DISABLED = 0x1;
    public static final int ENABLED = 0x2;

    public DriverBugWorkarounds() {
    }

    public DriverBugWorkarounds(Map<String, Boolean> states) {
        if (states == null || states.isEmpty()) return;
        for (var e : states.entrySet()) {
            switch (e.getKey()) {
            }
        }
    }

    private static byte mask(Map.Entry<?, Boolean> e) {
        var v = e.getValue();
        if (v == Boolean.TRUE) {
            return ENABLED;
        } else if (v == Boolean.FALSE) {
            return DISABLED;
        } else {
            return DEFAULT;
        }
    }

    public static boolean isEnabled(byte state) {
        return (state & ENABLED) != 0;
    }

    public static boolean isDisabled(byte state) {
        return (state & DISABLED) != 0;
    }

    public void applyOverrides(DriverBugWorkarounds workarounds) {
        if (workarounds != null) {
        }
    }
}
