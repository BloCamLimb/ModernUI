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

package icyllis.modernui.core;

import javax.annotation.Nullable;

/**
 * Provides the ability to cancel an operation in progress.
 */
public final class CancellationSignal {

    private boolean mIsCanceled;
    private OnCancelListener mOnCancelListener;
    private boolean mCancelInProgress;

    /**
     * Creates a cancellation signal, initially not canceled.
     */
    public CancellationSignal() {
    }

    /**
     * Returns true if the operation has been canceled.
     *
     * @return True if the operation has been canceled.
     */
    public boolean isCanceled() {
        synchronized (this) {
            return mIsCanceled;
        }
    }

    /**
     * Throws {@link OperationCanceledException} if the operation has been canceled.
     *
     * @throws OperationCanceledException if the operation has been canceled.
     */
    public void throwIfCanceled() {
        if (isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    /**
     * Cancels the operation and signals the cancellation listener.
     * If the operation has not yet started, then it will be canceled as soon as it does.
     */
    public void cancel() {
        final OnCancelListener listener;
        synchronized (this) {
            if (mIsCanceled) {
                return;
            }
            mIsCanceled = true;
            mCancelInProgress = true;
            listener = mOnCancelListener;
        }

        try {
            if (listener != null) {
                listener.onCancel();
            }
        } finally {
            synchronized (this) {
                mCancelInProgress = false;
                notifyAll();
            }
        }
    }

    /**
     * Sets the cancellation listener to be called when canceled.
     * <p>
     * This method is intended to be used by the recipient of a cancellation signal
     * such as a database or a content provider to handle cancellation requests
     * while performing a long-running operation.  This method is not intended to be
     * used by applications themselves.
     * <p>
     * If {@link CancellationSignal#cancel} has already been called, then the provided
     * listener is invoked immediately.
     * <p>
     * This method is guaranteed that the listener will not be called after it
     * has been removed.
     *
     * @param listener The cancellation listener, or null to remove the current listener.
     */
    public void setOnCancelListener(@Nullable OnCancelListener listener) {
        synchronized (this) {
            waitForCancelFinishedLocked();

            if (mOnCancelListener == listener) {
                return;
            }
            mOnCancelListener = listener;
            if (!mIsCanceled || listener == null) {
                return;
            }
        }
        listener.onCancel();
    }

    private void waitForCancelFinishedLocked() {
        while (mCancelInProgress) {
            try {
                wait();
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Listens for cancellation.
     */
    @FunctionalInterface
    public interface OnCancelListener {

        /**
         * Called when {@link CancellationSignal#cancel} is invoked.
         */
        void onCancel();
    }
}
