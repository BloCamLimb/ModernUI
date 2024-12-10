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

package icyllis.arc3d.core;

import org.jspecify.annotations.NonNull;

public interface PathIterable {

    /**
     * Returns the filling rule for determining the interior of the
     * path.
     *
     * @return the winding rule.
     * @see PathIterator#FILL_EVEN_ODD
     * @see PathIterator#FILL_NON_ZERO
     */
    int getFillRule();

    @NonNull
    PathIterator getPathIterator();

    default void forEach(@NonNull PathConsumer action) {
        var it = getPathIterator();
        int verb;
        float[] coords = new float[6];
        while ((verb = it.next(coords, 0)) != PathIterator.VERB_DONE) {
            switch (verb) {
                case PathIterator.VERB_MOVE -> action.moveTo(coords[0], coords[1]);
                case PathIterator.VERB_LINE -> action.lineTo(coords[0], coords[1]);
                case PathIterator.VERB_QUAD -> action.quadTo(coords, 0);
                case PathIterator.VERB_CUBIC -> action.cubicTo(coords, 0);
                case PathIterator.VERB_CLOSE -> action.close();
            }
        }
        action.done();
    }
}
