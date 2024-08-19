/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite.geom;

/**
 * This technology draws ellipse (fill, stroke, stroke and fill), round rectangles
 * with elliptical corners (fill, stroke, stroke and fill; four corners have the
 * same radius), supports stroke direction (inside, center, outside) using analytic
 * method.
 * <p>
 * Always use instanced rendering, without per-vertex data, without index buffer,
 * without uniforms, emits coverage. Supports solid color, supports over-stroking,
 * supports device-independent antialiasing, supports 32 bit-per-channel color input,
 * support ellipses in local coordinates, supports hard-edge coverage (no AA), supports
 * any local-to-device transforms.
 */
//TODO
public class AnalyticEllipseStep {
}
