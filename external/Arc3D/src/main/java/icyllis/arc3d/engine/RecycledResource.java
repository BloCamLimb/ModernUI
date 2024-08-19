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

package icyllis.arc3d.engine;

/**
 * The subclass that supports recycling.
 */
public abstract class RecycledResource extends ManagedResource {

    public RecycledResource(Device device) {
        super(device);
    }

    /**
     * When recycle is called and there is only one ref left on the resource, we will signal that
     * the resource can be recycled for reuse. If the subclass (or whoever is managing this resource)
     * decides not to recycle the objects, it is their responsibility to call unref on the object.
     */
    public final void recycle() {
        if (unique()) {
            onRecycle();
        } else {
            unref();
        }
    }

    /**
     * Override this method to invoke recycling of the underlying resource.
     */
    public abstract void onRecycle();
}
