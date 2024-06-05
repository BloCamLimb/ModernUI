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

package icyllis.arc3d.test;

import icyllis.arc3d.core.Matrix;
import icyllis.arc3d.granite.UniformDataCache;
import icyllis.arc3d.granite.UniformDataGatherer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class TestUniformDataCache {

    public static final Logger LOGGER = LoggerFactory.getLogger("Arc3D");

    // -Dorg.slf4j.simpleLogger.logFile=System.out -Dorg.lwjgl.util.DebugAllocator=true -ea
    public static void main(String[] args) {
        var gatherer = new UniformDataGatherer(UniformDataGatherer.Std140Layout);
        var cache = new UniformDataCache();
        try (gatherer; cache) {
            gatherer.write2f(5f, 11.5f);
            gatherer.write4f(0.2f, 0.4f, 0.4f, 0.5f);
            gatherer.write3f(3f, 4f, 5f);
            var finish1 = gatherer.finish();
            var cached1 = cache.insert(finish1);
            log(1, finish1, cached1);

            gatherer.reset();
            gatherer.writeMatrix3f(new Matrix());
            gatherer.write4i(20, 30, 40, 50);
            var finish2 = gatherer.finish();
            var cached2 = cache.insert(finish2);
            log(2, finish2, cached2);

            gatherer.reset();
            gatherer.write2f(5f, 11.5f);
            gatherer.write4f(0.2f, 0.4f, 0.4f, 0.5f);
            gatherer.write3f(3f, 4f, 5f);
            var finish3 = gatherer.finish();
            var cached3 = cache.insert(finish3);
            log(3, finish3, cached3);
        }
    }

    static void log(int index, ByteBuffer finish, ByteBuffer cached) {
        LOGGER.info("Finish {}: {}@{}", index, finish, Integer.toHexString(System.identityHashCode(finish)));
        LOGGER.info("Cached {}: {}@{}", index, cached, Integer.toHexString(System.identityHashCode(cached)));
    }
}
