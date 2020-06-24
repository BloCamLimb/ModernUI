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

package icyllis.modernui.ui.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import icyllis.modernui.ui.master.View;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Serialize and deserialize layout file and instantiate views
 */
public enum LayoutManager {
    INSTANCE;

    private final Gson gson;

    private final Map<String, Constructor<? extends View>> constructorMap = new HashMap<>();

    {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LayoutContainer.class, new Adaptor())
                .excludeFieldsWithoutExposeAnnotation()
                .create();
    }

    /*@Nonnull
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
    }*/

    /**
     * Serialize and deserialize a layout file
     */
    private static class Adaptor extends TypeAdapter<LayoutContainer> {

        @Override
        public void write(JsonWriter out, LayoutContainer value) throws IOException {

        }

        @Override
        public LayoutContainer read(JsonReader in) throws IOException {
            return null;
        }
    }

}
