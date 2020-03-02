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

package icyllis.modernui.system;

import icyllis.modernui.api.ModernUI_API;
import icyllis.modernui.api.handler.IGuiManager;
import icyllis.modernui.api.handler.IModuleManager;
import icyllis.modernui.api.handler.INetworkManager;
import icyllis.modernui.gui.blur.BlurHandler;
import icyllis.modernui.gui.master.GlobalModuleManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.Field;

@Mod(ModernUI.MODID)
public class ModernUI {

    public static final String MODID = "modernui";

    public static final Logger LOGGER = LogManager.getLogger("ModernUI");
    public static final Marker MARKER = MarkerManager.getMarker("MAIN");

    public ModernUI() {
        try {
            Class<?> apiClass = Class.forName("icyllis.modernui.api.ModernUI_API");
            Field f = apiClass.getDeclaredField("network");
            f.setAccessible(true);
            f.set(ModernUI_API.INSTANCE, NetworkManager.INSTANCE);
            if (FMLEnvironment.dist == Dist.CLIENT) {
                f = apiClass.getDeclaredField("gui");
                f.setAccessible(true);
                f.set(ModernUI_API.INSTANCE, GuiManager.INSTANCE);
                f = apiClass.getDeclaredField("module");
                f.setAccessible(true);
                f.set(ModernUI_API.INSTANCE, GlobalModuleManager.INSTANCE);
                ModernUI.LOGGER.debug(MARKER, "{} has been initialized", BlurHandler.INSTANCE.getDeclaringClass().getSimpleName()); // call constructor methods
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

}
