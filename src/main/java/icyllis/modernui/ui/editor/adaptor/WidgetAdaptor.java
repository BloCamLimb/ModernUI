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

package icyllis.modernui.ui.editor.adaptor;

import com.google.gson.*;
import icyllis.modernui.ui.editor.WidgetContainer;
import icyllis.modernui.ui.test.Widget;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;

public class WidgetAdaptor implements JsonSerializer<WidgetContainer>, JsonDeserializer<WidgetContainer> {

    @Override
    public JsonElement serialize(@Nonnull WidgetContainer container, Type type, @Nonnull JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", container.name);
        jsonObject.addProperty("class", container.widget.getClass().getName());
        jsonObject.add("widget", context.serialize(container.widget));
        return jsonObject;
    }

    @Override
    public WidgetContainer deserialize(@Nonnull JsonElement json, Type type, @Nonnull JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        try {
            String name = jsonObject.get("name").getAsString();
            Class<?> clazz = Class.forName(jsonObject.get("class").getAsString());
            Widget widget = context.deserialize(jsonObject.get("widget"), clazz);
            return new WidgetContainer(name, widget);
        } catch (ClassNotFoundException ignored) {

        }
        return null;
    }
}
