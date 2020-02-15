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

package icyllis.modern.system.asm;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import icyllis.modern.system.ModernUI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TransformationService implements ITransformationService {

    @Nonnull
    @Override
    public String name() {
        return ModernUI.MODID;
    }

    @Override
    public void initialize(@Nonnull IEnvironment environment) {

    }

    @Override
    public void beginScanning(@Nonnull IEnvironment environment) {

    }

    @Override
    public void onLoad(@Nonnull IEnvironment env, @Nonnull Set<String> otherServices) {

    }

    @Nonnull
    @Override
    public List<ITransformer> transformers() {
        List<ITransformer> list = new ArrayList<>();
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            list.add(new ScreenTransformer());
        });
        return list;
    }
}
