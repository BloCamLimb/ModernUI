/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.test.jmh;

import icyllis.modernui.R;
import icyllis.modernui.material.MaterialTheme;
import icyllis.modernui.resources.AssetManager;
import icyllis.modernui.resources.AssetPack;
import icyllis.modernui.resources.EmptyAssetProvider;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.ResourcesBuilder;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Fork(1)
@Threads(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class TestBagResolve {

    private static final AssetPack sSystemAssetPack;

    static {
        try {
            ResourcesBuilder resourcesBuilder = new ResourcesBuilder(R.ns);
            MaterialTheme.addToResources(resourcesBuilder);
            sSystemAssetPack = resourcesBuilder.buildPack(new EmptyAssetProvider());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create system AssetPack", e);
        }

        AssetManager.Builder ab = new AssetManager.Builder();
        ab.addAssetPack(sSystemAssetPack);
        mResources = ab.build();
    }

    private static final AssetManager mResources;

    @Benchmark
    public static void look() {
        mResources.getBag(R.style.Theme_Material3_Light);
    }
}
