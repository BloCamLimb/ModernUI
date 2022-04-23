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

package icyllis.modernui.forge;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinConfigPlugin implements IMixinConfigPlugin {

    private int mLevel;

    @Override
    public void onLoad(String mixinPackage) {
        mLevel = ModernUIForge.getBootstrapLevel();
    }

    @Override
    public String getRefMapperConfig() {
        return FMLLoader.getNameFunction("srg").isPresent() ? null : "ModernUI-ModernUI-Forge-refmap.json";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (ModernUIForge.isOptiFineLoaded() &&
                mixinClassName.equals("icyllis.modernui.mixin.AccessVideoSettings")) {
            return false;
        }
        if ((mLevel & ModernUIForge.BOOTSTRAP_SMOOTH_SCROLLING) != 0) {
            return !mixinClassName.equals("icyllis.modernui.mixin.MixinScrollPanel") &&
                    !mixinClassName.equals("icyllis.modernui.mixin.MixinSelectionList");
        }
        if ((mLevel & ModernUIForge.BOOTSTRAP_TEXT_ENGINE) != 0) {
            return !mixinClassName.equals("icyllis.modernui.mixin.MixinBidiReorder") &&
                    !mixinClassName.equals("icyllis.modernui.mixin.MixinClientLanguage") &&
                    !mixinClassName.equals("icyllis.modernui.mixin.MixinFontRenderer") &&
                    !mixinClassName.equals("icyllis.modernui.mixin.MixinIngameGui") &&
                    !mixinClassName.equals("icyllis.modernui.mixin.MixinLanguage") &&
                    !mixinClassName.equals("icyllis.modernui.mixin.MixinStringSplitter");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
