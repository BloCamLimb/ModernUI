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

package icyllis.modernui.akashi;

import javax.annotation.Nonnull;

public class PipelineDesc extends KeyBuilder {

    /**
     * Builds a base pipeline descriptor, without additional information.
     *
     * @param desc the pipeline descriptor
     * @param info the pipeline information
     * @param caps the context capabilities
     */
    @Nonnull
    public static PipelineDesc build(PipelineDesc desc, PipelineInfo info, Caps caps) {
        desc.reset();
        /*genKey(desc, info, caps);
        desc.mBaseLength = desc.length();*/
        return desc;
    }
}
