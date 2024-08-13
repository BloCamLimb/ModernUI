/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine.trash;

import javax.annotation.concurrent.Immutable;

/**
 * Provides custom shader code to the Arc3D shading pipeline. Processor objects <em>must</em> be
 * immutable: after being constructed, their fields may not change.
 */
@Deprecated
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
            DefaultGeoProc_ClassID = 4,
            SDFRect_GeoProc_ClassID = 5,
            Hard_XferProc_ClassID = 6;

    protected final int mClassID;

    protected Processor(int classID) {
        mClassID = classID;
    }
}
