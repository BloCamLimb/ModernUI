/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.system;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

// Represents a socket connection.
// No thread-safety is guaranteed for these APIs.
public abstract sealed class RpcTransport permits RpcTransportRaw, RpcTransportTls {

    final SocketChannel mSocket;

    RpcTransport(SocketChannel socket) {
        mSocket = socket;
    }

    public abstract void interruptibleWriteFully(
            ByteBuffer[] iovs, int offset, int limit) throws IOException;

    public abstract void interruptibleReadFully(
            ByteBuffer[] iovs, int offset, int limit) throws IOException;

    public void close() throws IOException {
        mSocket.close();
    }
}
