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
import icyllis.modernui.annotation.Nullable;

import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class RpcSession implements AutoCloseable {

    private final ReentrantLock mLock = new ReentrantLock();
    private final Condition mAvailableConnectionCv = mLock.newCondition();

    private int mWaitingThreads = 0;
    private final ArrayList<RpcConnection> mOutgoing = new ArrayList<>();

    public RpcSession(RpcTransportCtx ctx) {

    }

    @Override
    public void close() {

    }

    public void shutdownAndWait(boolean wait) {

    }

    private void addOutgoingConnection(RpcTransport rpcTransport, boolean init) {
        RpcConnection connection = new RpcConnection(rpcTransport);
        mLock.lock();
        try {
            connection.exclusiveThread = Thread.currentThread();
            mOutgoing.add(connection);
        } finally {
            mLock.unlock();
        }
    }

    private void clearConnectionThread(@NonNull RpcConnection connection) {
        mLock.lock();
        try {
            connection.exclusiveThread = null;
            if (mWaitingThreads > 0) {
                mAvailableConnectionCv.signal();
            }
        } finally {
            mLock.unlock();
        }
    }
}

final class RpcConnection {

    final @NonNull RpcTransport transport;

    // we never hold strong refs to dying threads, so no leaks here
    @Nullable
    Thread exclusiveThread;

    boolean allowNested = false;

    RpcConnection(@NonNull RpcTransport transport) {
        this.transport = transport;
    }
}
