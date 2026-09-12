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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public final class RpcTransportCtxRaw extends RpcTransportCtx {
    @Override
    public RpcTransport newTransport(SocketChannel socket) {
        return new RpcTransportRaw(socket);
    }
}

final class RpcTransportRaw extends RpcTransport {

    RpcTransportRaw(SocketChannel socket) {
        super(socket);
    }

    @Override
    public void interruptibleWriteFully(ByteBuffer[] iovs, int offset, int limit) throws IOException {
        final SocketChannel socket = mSocket;
        for (;;) {
            long written = socket.write(iovs, offset, limit - offset);

            if (written < 0) throw new EOFException();

            while (offset < limit && !iovs[offset].hasRemaining()) offset++;
            if (offset >= limit) return;
        }
    }

    @Override
    public void interruptibleReadFully(ByteBuffer[] iovs, int offset, int limit) throws IOException {
        final SocketChannel socket = mSocket;
        for (;;) {
            long read = socket.read(iovs, offset, limit - offset);

            if (read < 0) throw new EOFException();

            while (offset < limit && !iovs[offset].hasRemaining()) offset++;
            if (offset >= limit) return;
        }
    }
}
