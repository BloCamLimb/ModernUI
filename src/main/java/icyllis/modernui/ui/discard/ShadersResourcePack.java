/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.ui.discard;

import com.google.common.collect.ImmutableSet;
import icyllis.modernui.system.ModernUI;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.resources.data.PackMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class ShadersResourcePack implements IResourcePack {

    private final ModFile MF = FMLLoader.getLoadingModList().getModFileById(ModernUI.MODID).getFile();

    @Nonnull
    @Override
    public InputStream getRootResourceStream(@Nonnull String fileName) throws IOException {
        return Files.newInputStream(MF.findResource("assets/" + ModernUI.MODID + "/" + fileName));
    }

    @Nonnull
    @Override
    public InputStream getResourceStream(@Nonnull ResourcePackType type, @Nonnull ResourceLocation location) throws IOException {
        if (type == ResourcePackType.CLIENT_RESOURCES && location.getNamespace().equals("minecraft") && location.getPath().startsWith("shaders/")) {
            try {
                return Files.newInputStream(MF.findResource("assets/" + ModernUI.MODID + "/" + location.getPath()));
            } catch (IOException ignored) {

            }
        }
        throw new FileNotFoundException(location.toString());
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getAllResourceLocations(@Nonnull ResourcePackType type, @Nonnull String namespaceIn, @Nonnull String pathIn, int maxDepthIn, @Nonnull Predicate<String> filterIn) {
        return Collections.emptyList();
    }

    @Override
    public boolean resourceExists(@Nonnull ResourcePackType type, @Nonnull ResourceLocation location) {
        return type == ResourcePackType.CLIENT_RESOURCES
                && location.getNamespace().equals("minecraft")
                && location.getPath().startsWith("shaders/")
                && Files.exists(MF.findResource("assets/" + ModernUI.MODID + "/" + location.getPath()));
    }

    @Nonnull
    @Override
    public Set<String> getResourceNamespaces(@Nonnull ResourcePackType type) {
        return type == ResourcePackType.CLIENT_RESOURCES ? ImmutableSet.of("minecraft") : Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getMetadata(IMetadataSectionSerializer<T> deserializer) {
        if ("pack".equals(deserializer.getSectionName())) {
            return (T) new PackMetadataSection(new StringTextComponent(""), 3);
        }
        return null;
    }

    @Nonnull
    @Override
    public String getName() {
        return "";
    }

    @Override
    public void close() {

    }
}
