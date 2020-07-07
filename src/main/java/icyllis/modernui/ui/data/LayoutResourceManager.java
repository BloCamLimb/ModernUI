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

import icyllis.modernui.graphics.shader.ShaderTools;
import icyllis.modernui.ui.master.View;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.ReloadRequirements;
import net.minecraftforge.resource.SelectiveReloadStateHandler;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Serialize and deserialize layout file and instantiate views
 */
@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public enum LayoutResourceManager {
    INSTANCE;

    public static final Type UI_RESOURCE_TYPE = new Type();

    private final Map<String, Constructor<? extends View>> constructorMap = new HashMap<>();

    public static void init() {
        /*((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener(
                (ISelectiveResourceReloadListener) INSTANCE::onResourcesReload);*/
    }

    /*private void onResourcesReload(IResourceManager manager, @Nonnull Predicate<IResourceType> t) {
        if (!t.test(LayoutResourceManager.UI_RESOURCE_TYPE)) {
            return;
        }
        //TODO
    }

    public void reloadLayoutResources() {
        SelectiveReloadStateHandler.INSTANCE.beginReload(ReloadRequirements.include(UI_RESOURCE_TYPE));
        Minecraft.getInstance().reloadResources();
        SelectiveReloadStateHandler.INSTANCE.endReload();
    }*/

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

    private static class Type implements IResourceType {

    }

}
