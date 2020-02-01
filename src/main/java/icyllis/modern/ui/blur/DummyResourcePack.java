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

package icyllis.modern.ui.blur;

import com.google.common.collect.ImmutableSet;
import icyllis.modern.system.ModernUI;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.resources.data.PackMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

public class DummyResourcePack implements IResourcePack {

    private final ModFile mf = FMLLoader.getLoadingModList().getModFileById(ModernUI.MODID).getFile();

    @Override
    public InputStream getRootResourceStream(String fileName) throws IOException {
        return Files.newInputStream(mf.findResource("assets/minecraft/" + fileName));
    }

    @Override
    public InputStream getResourceStream(ResourcePackType type, ResourceLocation location) throws IOException {
        if (type == ResourcePackType.CLIENT_RESOURCES && location.getNamespace().equals("minecraft") && location.getPath().startsWith("shaders/")) {
            try {
                return Files.newInputStream(mf.findResource(location.getPath()));
            } catch (IOException ignored) {

            }
        }
        throw new FileNotFoundException(location.toString());
    }

    @Override
    public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType type, String pathIn, int maxDepth, Predicate<String> filter) {
        return Collections.emptyList();
    }

    @Override
    public boolean resourceExists(ResourcePackType type, ResourceLocation location) {
        return type == ResourcePackType.CLIENT_RESOURCES
                && location.getNamespace().equals("minecraft")
                && location.getPath().startsWith("shaders/")
                && Files.exists(mf.findResource(location.getPath()));
    }

    @Override
    public Set<String> getResourceNamespaces(ResourcePackType type) {
        return type == ResourcePackType.CLIENT_RESOURCES ? ImmutableSet.of("minecraft") : Collections.emptySet();
    }

    @Nullable
    @Override
    public <T> T getMetadata(IMetadataSectionSerializer<T> deserializer) {
        if ("pack".equals(deserializer.getSectionName())) {
            return (T) new PackMetadataSection(new StringTextComponent(""), 3);
        }
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void close() {

    }
}
