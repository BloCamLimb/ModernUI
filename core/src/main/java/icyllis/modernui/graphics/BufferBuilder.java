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
 * This class is used to build the most primitive graphics API buffers on the client,
 * and also includes its renderer. These buffers can be used directly for native API
 * calls. A RenderNode is one-to-one correspondence with a BufferBuilder. When the
 * content of this node itself has not changed, there is no need to rebuild and
 * upload buffer data, avoiding sending data to GPU frequently.
 * <p>
 * See the implementation class documentation for more details
 */
public abstract class BufferBuilder {
}
