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
import org.lwjgl.system.MemoryUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

//TODO
public final class RpcTransportCtxTls extends RpcTransportCtx {

    private final SSLContext mSslContext;

    public RpcTransportCtxTls(@NonNull SSLContext sslContext) {
        mSslContext = sslContext;
    }

    @NonNull
    @Override
    public RpcTransport newTransport(@NonNull SocketChannel socket, boolean isClient) throws IOException {

        SSLEngine ssl = mSslContext.createSSLEngine();

        //TODO setup ssl parameters and do handshake

        return new RpcTransportTls(socket, ssl);
    }
}

//TODO
final class RpcTransportTls extends RpcTransport {

    private final SSLEngine mSsl;
    private final ByteBuffer mWorkingBuffer;

    RpcTransportTls(@NonNull SocketChannel socket, @NonNull SSLEngine ssl) {
        super(socket);
        SSLSession session = ssl.getSession();
        mWorkingBuffer = MemoryUtil.memAlloc(
                session.getPacketBufferSize());
        mSsl = ssl;
    }

    @Override
    public void interruptibleWriteFully(@NonNull ByteBuffer[] iovs, int offset, int limit) throws IOException {
    }

    @Override
    public void interruptibleReadFully(@NonNull ByteBuffer[] iovs, int offset, int limit) throws IOException {
    }

    @Override
    public void close() throws IOException {
        MemoryUtil.memFree(mWorkingBuffer);
        super.close();
    }
}
