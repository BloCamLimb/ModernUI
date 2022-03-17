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

package icyllis.modernui.graphics;

/**
 * Base class for image filters (sometimes known as image effects, or post-processing effects).
 * If one is installed in the paint, then all drawing occurs as
 * usual, but it is as if the drawing happened into an offscreen (before the transfer mode is applied).
 * This offscreen bitmap will then be handed to the image filter, who in turn creates a new bitmap
 * which is what will finally be drawn to the device (using the original transfer mode).
 * <p>
 * The local space of image filters matches the local space of the drawn geometry. For instance if
 * there is rotation on the canvas, the blur will be computed along those rotated axes and not in
 * the device space. In order to achieve this result, the actual drawing of the geometry may happen
 * in an un-rotated coordinate system so that the filtered image can be computed more easily, and
 * then it will be post transformed to match what would have been produced if the geometry were
 * drawn with the total canvas matrix to begin with.
 */
public class ImageFilter {
}
