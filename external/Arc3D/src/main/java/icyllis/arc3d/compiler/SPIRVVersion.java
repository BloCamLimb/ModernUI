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

package icyllis.arc3d.compiler;

public enum SPIRVVersion {
    /**
     * SPIR-V version 1.0 for OpenGL 4.5 and Vulkan 1.0.
     */
    SPIRV_1_0(0x00010000),
    /**
     * SPIR-V version 1.3 for Vulkan 1.1.
     */
    SPIRV_1_3(0x00010300),
    /**
     * SPIR-V version 1.4 for Vulkan 1.2.
     */
    SPIRV_1_4(0x00010400),
    /**
     * SPIR-V version 1.5 for Vulkan 1.2.
     */
    SPIRV_1_5(0x00010500),
    /**
     * SPIR-V version 1.6 for Vulkan 1.3.
     */
    SPIRV_1_6(0x00010600);

    public final int mVersionNumber;

    SPIRVVersion(int versionNumber) {
        mVersionNumber = versionNumber;
    }

    public boolean isBefore(SPIRVVersion other) {
        return compareTo(other) < 0;
    }

    public boolean isAtLeast(SPIRVVersion other) {
        return compareTo(other) >= 0;
    }
}
