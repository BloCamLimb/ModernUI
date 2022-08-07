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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides custom shader code to the Arc UI shading pipeline. Processor objects <em>must</em> be
 * immutable: after being constructed, their fields may not change.
 */
@Immutable
public abstract class Processor {

    // Reserved ID for missing (null) processors
    public static final int NULL_CLASS_ID = 0;

    private static final AtomicInteger sNextClassID = new AtomicInteger(NULL_CLASS_ID + 1);

    protected final int mClassID;

    protected Processor(int classID) {
        mClassID = classID;
    }

    protected static int genClassID() {
        final int id = sNextClassID.getAndIncrement();
        assert id != NULL_CLASS_ID : "This should never wrap as it should only be called once for each Processor " +
                "subclass.";
        return id;
    }

    /**
     * Human-meaningful string to identify this processor; may be embedded in generated shader
     * code and must be a legal ArSL identifier prefix.
     */
    public abstract String name();

    /**
     * @return unique ID to identify this class at runtime.
     */
    public final int classID() {
        return mClassID;
    }
}
