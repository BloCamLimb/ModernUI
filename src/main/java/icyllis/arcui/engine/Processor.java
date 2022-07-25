/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Provides custom shader code to the Arc UI shading pipeline. Processor objects <em>must</em> be
 * immutable: after being constructed, their fields may not change.
 */
@Immutable
public abstract class Processor {

    public static final int INVALID_CLASS_ID = -1;

    private static final ConcurrentHashMap<Class<? extends Processor>, Integer> CLASS_IDS = new ConcurrentHashMap<>();
    private static final Function<Class<? extends Processor>, Integer> CLASS_ID_GEN = clz -> CLASS_IDS.size();

    protected Processor() {
    }

    /**
     * @return Unique ID to identify this class in runtime.
     */
    public final int getClassID() {
        return CLASS_IDS.computeIfAbsent(getClass(), CLASS_ID_GEN);
    }
}
