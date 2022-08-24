/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arctic.engine;

/**
 * Struct to supply options to flush calls.
 * <p>
 * After issuing all commands, fNumSemaphore semaphores will be signaled by the gpu. The client
 * passes in an array of fNumSemaphores GrBackendSemaphores. In general these GrBackendSemaphore's
 * can be either initialized or not. If they are initialized, the backend uses the passed in
 * semaphore. If it is not initialized, a new semaphore is created and the GrBackendSemaphore
 * object is initialized with that semaphore. The semaphores are not sent to the GPU until the next
 * GrContext::submit call is made. See the GrContext::submit for more information.
 * <p>
 * The client will own and be responsible for deleting the underlying semaphores that are stored
 * and returned in initialized GrBackendSemaphore objects. The GrBackendSemaphore objects
 * themselves can be deleted as soon as this function returns.
 * <p>
 * If a finishedProc is provided, the finishedProc will be called when all work submitted to the gpu
 * from this flush call and all previous flush calls has finished on the GPU. If the flush call
 * fails due to an error and nothing ends up getting sent to the GPU, the finished proc is called
 * immediately.
 * <p>
 * If a submittedProc is provided, the submittedProc will be called when all work from this flush
 * call is submitted to the GPU. If the flush call fails due to an error and nothing will get sent
 * to the GPU, the submitted proc is called immediately. It is possibly that when work is finally
 * submitted, that the submission actual fails. In this case we will not reattempt to do the
 * submission. Arctic notifies the client of these via the success bool passed into the submittedProc.
 * The submittedProc is useful to the client to know when semaphores that were sent with the flush
 * have actually been submitted to the GPU so that they can be waited on (or deleted if the submit
 * fails).
 * Note about GL: In GL work gets sent to the driver immediately during the flush call, but we don't
 * really know when the driver sends the work to the GPU. Therefore, we treat the submitted proc as
 * we do in other backends. It will be called when the next GrContext::submit is called after the
 * flush (or possibly during the flush if there is no work to be done for the flush). The main use
 * case for the submittedProc is to know when semaphores have been sent to the GPU and even in GL
 * it is required to call GrContext::submit to flush them. So a client should be able to treat all
 * backend APIs the same in terms of how the submitted procs are treated.
 */
public final class FlushInfo {

    public BackendSemaphore[] mSignalSemaphores = null;
    public FinishedCallback mFinishedCallback = null;
    public SubmittedCallback mSubmittedCallback = null;

    @FunctionalInterface
    public interface FinishedCallback {

        void onFinished();
    }

    @FunctionalInterface
    public interface SubmittedCallback {

        void onSubmitted(boolean success);
    }
}
