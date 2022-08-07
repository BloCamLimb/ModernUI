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

package icyllis.arcui.arsl;

import icyllis.arcui.arsl.ir.SymbolTable;

//TODO
public class Mangler {

    private int mCounter;

    /**
     * Mangles baseName to create a name that is unique within symbolTable.
     */
    public String uniqueName(String baseName, SymbolTable symbolTable) {
        return baseName;
    }

    public void reset() {
        mCounter = 0;
    }
}
