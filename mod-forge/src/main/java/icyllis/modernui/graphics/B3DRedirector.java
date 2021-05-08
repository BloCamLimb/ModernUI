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

package icyllis.modernui.graphics;

import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modernui.ModernUI;
import icyllis.modernui.platform.RenderCore;

/**
 * Integration with Blaze3D
 */
public class B3DRedirector implements GLWrapper.Redirector {

    @Override
    public void onInit() {
        ModernUI.LOGGER.info(RenderCore.MARKER, "Sharing GL states with Blaze3D");
    }

    @Override
    public boolean bindTexture(int target, int texture) {
        if (target == GLWrapper.GL_TEXTURE_2D) {
            GlStateManager._bindTexture(texture);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteTexture(int target, int texture) {
        if (target == GLWrapper.GL_TEXTURE_2D) {
            GlStateManager._deleteTexture(texture);
            return true;
        }
        return false;
    }
}
