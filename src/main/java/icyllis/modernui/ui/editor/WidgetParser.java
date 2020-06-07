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

package icyllis.modernui.ui.editor;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import icyllis.modernui.ui.editor.adaptor.WidgetAdaptor;
import icyllis.modernui.ui.master.UIManager;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public enum WidgetParser {
    INSTANCE;

    private final Gson gson;

    {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(WidgetContainer.class, new WidgetAdaptor())
                .excludeFieldsWithoutExposeAnnotation()
                .create();
    }

    @Nonnull
    public List<WidgetContainer> parseWidgets(ResourceLocation location) {
        List<WidgetContainer> list = new ArrayList<>();
        Type type = new TypeToken<List<WidgetContainer>>(){}.getType();

        try (BufferedReader br =
                     new BufferedReader(
                             new InputStreamReader(
                                     Minecraft.getInstance().getResourceManager().getResource(location).getInputStream()
                             )
                     )
        ) {
            list = gson.fromJson(br, type);
        } catch (IOException e) {
            //ModernUI.LOGGER.debug(GlobalModuleManager.MARKER, "Can't find widget json at {}", location);
            e.printStackTrace();
            //TODO in editing mode, create new file in config folder
        } catch (RuntimeException e) {
            ModernUI.LOGGER.fatal(UIManager.MARKER, "Failed to parse widget json at {}", location);
            e.printStackTrace();
        }
        list.removeIf(Objects::isNull);
        return list;
    }

}
