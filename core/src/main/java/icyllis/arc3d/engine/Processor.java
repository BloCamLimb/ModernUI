/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.engine;

import icyllis.modernui.annotation.NonNull;

import javax.annotation.concurrent.Immutable;

/**
 * Provides custom shader code to the Arc 3D shading pipeline. Processor objects <em>must</em> be
 * immutable: after being constructed, their fields may not change.
 */
@Immutable
public abstract class Processor {

    /**
     * Class IDs.
     */
    public static final int
            Null_ClassID = 0, // Reserved ID for missing (null) processors
            CircularRRect_Geom_ClassID = 1,
            Circle_Geom_ClassID = 2,
            RoundRect_GeoProc_ClassID = 3,
            DefaultGeoProc_ClassID = 4;

    protected final int mClassID;

    protected Processor(int classID) {
        mClassID = classID;
    }

    /**
     * Human-meaningful string to identify this processor; may be embedded in generated shader
     * code and must be a legal AkSL identifier prefix.
     */
    @NonNull
    public abstract String name();

    /**
     * @return unique ID that identifies this processor class.
     */
    public final int classID() {
        return mClassID;
    }
}
