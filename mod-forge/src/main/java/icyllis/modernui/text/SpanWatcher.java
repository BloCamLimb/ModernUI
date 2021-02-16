/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icyllis.modernui.text;

/**
 * When an object of this type is attached to a Spannable, its methods
 * will be called to notify it that other markup objects have been
 * added, changed, or removed.
 */
public interface SpanWatcher extends NoCopySpan {

    /**
     * This method is called to notify you that the specified object
     * has been attached to the specified range of the text.
     */
    void onSpanAdded(Spannable text, Object what, int start, int end);

    /**
     * This method is called to notify you that the specified object
     * has been detached from the specified range of the text.
     */
    void onSpanRemoved(Spannable text, Object what, int start, int end);

    /**
     * This method is called to notify you that the specified object
     * has been relocated from the range <code>ost&hellip;oen</code>
     * to the new range <code>nst&hellip;nen</code> of the text.
     */
    void onSpanChanged(Spannable text, Object what, int ost, int oen, int nst, int nen);
}
