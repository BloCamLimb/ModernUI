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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.util.Log;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;

final class RpcState {

    public static final boolean DEBUG = true;
    public static final Marker MARKER = MarkerFactory.getMarker("RpcState");

    public void sendConnectionInit(RpcConnection connection, RpcSession session) throws Exception {
        try (var stack = MemoryStack.stackPush()) {
            var init = stack.malloc(8);
            init.put((byte)'c').put((byte)'c').put((byte)'i').put((byte)'\0')
                    .putInt(0);
            rpcSend(connection, session, "connection init", new ByteBuffer[]{init}, 0, 1);
        }
    }

    private void rpcSend(RpcConnection connection, RpcSession session,
                         String what, ByteBuffer[] iovs, int offset, int limit) throws Exception {
        var transport = connection.transport;

        if (DEBUG) {
            for (int i = offset; i < limit; i++) {
                Log.LOGGER.info(MARKER, "Sending {} (part {} of {}) on RpcTransport {}: {}",
                        what, i - offset + 1, limit - offset, transport, hexString(iovs[i]));
            }
        }

        try {
            transport.interruptibleWriteFully(iovs, offset, limit);
        } catch (IOException e) {
            handleRpcError(transport, session, e, "write", what, limit - offset);
        }
    }

    private void handleRpcError(RpcTransport transport, RpcSession session,
                                Exception ex, String stateContext, String what,
                                int niovs) throws Exception {
        boolean convertToDeadObject = false;
        if (ex instanceof SocketException &&
                // this is hardcoded in JDK
                "Connection reset".equals(ex.getMessage())) {
            convertToDeadObject = true;
        }
        if (ex instanceof EOFException) {
            convertToDeadObject = true;
        }

        if (ex instanceof DeadObjectException || convertToDeadObject) {
            if (DEBUG) {
                Log.LOGGER.info(MARKER, "Failed to {} {} ({} iovs) on RpcTransport {}, error: {}", stateContext, what, niovs,
                        transport, ex.toString());
            }
        } else {
            Log.LOGGER.error(MARKER, "Failed to {} {} ({} iovs) on RpcTransport {}, error: {}", stateContext, what, niovs,
                    transport, ex.toString());
        }

        session.shutdownAndWait(false);

        if (convertToDeadObject) {
            throw new DeadObjectException();
        } else {
            throw ex;
        }
    }

    static final char[] digits = {
            '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'
    };

    @NonNull
    public static StringBuilder hexString(@NonNull ByteBuffer bytes) {
        StringBuilder sb = new StringBuilder(bytes.remaining() * 2);
        for (int i = bytes.position(); i < bytes.limit(); i++) {
            int c = bytes.get(i) & 0xFF;
            sb
                    .append(digits[c >>> 4])
                    .append(digits[c & 0xF]);
        }
        return sb;
    }
}
