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
 * The ResourceAllocator explicitly distributes {@link GpuResource}s at flush time. It operates by
 * being given the usage intervals of the various proxies. It keeps these intervals in a singly
 * linked list sorted by increasing start index. (It also maintains a hash table from proxyID
 * to interval to find proxy reuse). The ResourceAllocator uses Registers (in the sense of register
 * allocation) to represent a future surface that will be used for each proxy during
 * `planAssignment`, and then assigns actual surfaces during `assign`.
 * <p>
 * Note: the op indices (used in the usage intervals) come from the order of the ops in
 * their opsTasks after the opsTask DAG has been linearized.
 * <p>
 * The planAssignment method traverses the sorted list and:
 * <ul>
 *     <li>moves intervals from the active list that have completed (returning their registers
 *     to the free pool) into the finished list (sorted by increasing start)</li>
 *
 *     <li>allocates a new register (preferably from the free pool) for the new interval
 *     adds the new interval to the active list (that is sorted by increasing end index)</li>
 * </ul>
 * After assignment planning, the user can choose to call `makeBudgetHeadroom` which:
 * <ul>
 *     <li>computes how much VRAM would be needed for new resources for all extant Registers</li>
 *
 *     <li>asks the resource cache to purge enough resources to get that much free space</li>
 *
 *     <li>if it's not possible, do nothing and return false. The user may opt to reset
 *     the allocator and start over with a different DAG.</li>
 * </ul>
 * <p>
 * If the user wants to commit to the current assignment plan, they call `assign` which:
 * <ul>
 *     <li>instantiates lazy proxies</li>
 *
 *     <li>instantiates new surfaces for all registers that need them</li>
 *
 *     <li>assigns the surface for each register to all the proxies that will use it</li>
 * </ul>
 * <p>
 * ************************************************************************************************
 * How does instantiation failure handling work when explicitly allocating?
 * <p>
 * In the gather usage intervals pass all the SurfaceProxies used in the flush should be
 * gathered (i.e., in OpsTask::gatherProxyIntervals).
 * <p>
 * During addInterval, read-only lazy proxies are instantiated. If that fails, the resource
 * allocator will note the failure and ignore pretty much anything else until `reset`.
 * <p>
 * During planAssignment, fully-lazy proxies are instantiated so that we can know their size for
 * budgeting purposes. If this fails, return false.
 * <p>
 * During assign, partially-lazy proxies are instantiated and new surfaces are created for all other
 * proxies. If any of these fails, return false.
 * <p>
 * The drawing manager will drop the flush if any proxies fail to instantiate.
 */
public class ResourceAllocator {

    static class Register {

    }
}
