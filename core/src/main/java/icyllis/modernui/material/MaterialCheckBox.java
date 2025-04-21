/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.material;

import icyllis.modernui.R;
import icyllis.modernui.core.Context;
import icyllis.modernui.widget.CheckBox;

/**
 * @deprecated use base class {@link CheckBox} instead
 */
@Deprecated(forRemoval = true)
public class MaterialCheckBox extends CheckBox {

    public MaterialCheckBox(Context context) {
        super(context, null, null, R.style.Widget_Material3_CompoundButton_CheckBox);
    }
}
