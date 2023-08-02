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

package icyllis.modernui.graphics;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;

/**
 * Custom drawables allow to execute using the underlying 3D API rather than the Canvas API.
 *
 * @since 3.8
 */
//TODO
public interface CustomDrawable {

    /**
     * The engine backend is deferred so the handler will be given access to the 3D API at
     * the correct point in the drawing stream as the engine backend flushes. Since the
     * drawable may mutate, each time it is drawn to a Canvas a new handler is snapped,
     * representing the drawable's state at the time of the snap.
     * <p>
     * When the engine backend flushes to the 3D API it will call the {@link #draw} on the
     * DrawHandler. At this time the drawable may add commands to the stream of commands for
     * the underlying 3D API. The draw function takes a DirectContext and a DrawableInfo
     * which contains information about the current state of 3D API which the caller must
     * respect. See DrawableInfo subclasses for more specific details on what information
     * is sent and the requirements for different 3D APIs.
     * <p>
     * Additionally there may be a slight delay from when the drawable adds its commands
     * to when those commands are actually submitted to the GPU. Thus the drawable or
     * DrawHandler is required to keep any resources that are used by its added commands
     * alive and valid until those commands are submitted to the driver. The DrawHandler
     * will be kept alive and then closed once the commands are submitted to the driver.
     * The {@link #close} of the DrawHandler is the signal to the drawable that the
     * commands have all been submitted. Different 3D APIs may have additional requirements
     * for certain resources which require waiting for the driver to finish all work on
     * those resources before reusing or deleting them. In this case, the drawable can
     * use the close call of the DrawHandler to add a fence to the GPU to track when the
     * GPU work has completed.
     * <p>
     * For OpenGL, if any context state is changed, {@link DirectContext#resetContext(int)}
     * should be called at the end of {@link #draw}.
     */
    @FunctionalInterface
    interface DrawHandler extends AutoCloseable {

        /**
         * The direct context may be used to invalidate backend context states.
         *
         * @param dContext the direct context
         * @param info     the backend specified info used to draw the drawable
         * @see DirectContext#resetContext(int)
         */
        void draw(DirectContext dContext, DrawableInfo info);

        /**
         * This is called when the draw call is submitted, used to clean up backend resources
         * created along with this object.
         */
        @Override
        default void close() {
        }
    }

    /**
     * Snaps off a new {@link DrawHandler} to represent the state of the {@link CustomDrawable} at
     * the time the snap is called. This is used for executing backend specific draws intermixed
     * with Arc 3D draws. The backend API, which will be used for the draw, as well as the view
     * matrix, device clip bounds and image info of the target buffer are passed in as inputs.
     *
     * @param backendApi see {@link Engine.BackendApi}
     * @return a drawable for this snap call
     */
    DrawHandler snapDrawHandler(int backendApi,
                                Matrix4 viewMatrix,
                                Rect2i clipBounds,
                                ImageInfo targetInfo);

    /**
     * Return the (conservative) bounds of what the drawable will draw. If the drawable can
     * change what it draws (e.g. animation or in response to some external change), then this
     * must return a bounds that is always valid for all possible states.
     */
    RectF getBounds();
}
