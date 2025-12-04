/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core.windows;

import org.lwjgl.system.*;
import org.lwjgl.system.windows.RECT;
import org.lwjgl.system.windows.WindowProc;

import static org.lwjgl.system.windows.User32.*;

//TODO WIP
public class WindowsNativeWindowBorder {

    public static class WndProc extends WindowProc {

        private long hwnd;
        private long prevWndProc;

        public WndProc(long hwnd) {
            this.hwnd = hwnd;
            prevWndProc = SetWindowLongPtr(null, hwnd, GWL_WNDPROC, address());
            SetWindowPos(null, hwnd, hwnd, 0, 0, 0, 0,
                    SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
        }

        public void destroy() {
            SetWindowLongPtr(null, hwnd, GWL_WNDPROC, prevWndProc);
            SetWindowPos(null, hwnd, hwnd, 0, 0, 0, 0,
                    SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
            free();
        }

        @Override
        @NativeType("LRESULT")
        public long invoke(@NativeType("HWND") long hwnd,
                           @NativeType("UINT") int uMsg,
                           @NativeType("WPARAM") long wParam,
                           @NativeType("LPARAM") long lParam) {
            if (uMsg == WM_NCCALCSIZE) {
                return WmNcCalcSize(hwnd, uMsg, wParam, lParam);
            }
            if (uMsg == WM_NCHITTEST) {
                return WmNcHitTest(hwnd, uMsg, wParam, lParam);
            }
            if (uMsg == WM_NCLBUTTONDOWN || uMsg == WM_NCLBUTTONUP) {
                if (wParam == HTMINBUTTON || wParam == HTMAXBUTTON || wParam == HTCLOSE) {
                    return 0;
                }
            }
            return nCallWindowProc(prevWndProc, hwnd, uMsg, wParam, lParam);
        }

        private long WmNcCalcSize(long hwnd, int uMsg, long wParam, long lParam) {
            if (wParam != 1) {
                return nCallWindowProc(prevWndProc, hwnd, uMsg, wParam, lParam);
            }
            //noinspection resource
            RECT firstRect = RECT.create(lParam);
            int originalTop = firstRect.top();
            long lResult = nCallWindowProc(prevWndProc, hwnd, uMsg, wParam, lParam);
            if (lResult != 0) {
                return lResult;
            }
            try (var stack = MemoryStack.stackPush()) {
                var borderThick = stack.mallocInt(1);
                int hResult = Dwmapi.DwmGetWindowAttribute(hwnd, 37, MemoryUtil.memAddress(borderThick), 4);
                if (hResult == 0) {
                    originalTop += borderThick.get(0);
                }
            }
            firstRect.top(originalTop);
            return lResult;
        }

        private long WmNcHitTest(long hwnd, int uMsg, long wParam, long lParam) {
            long lResult = nCallWindowProc(prevWndProc, hwnd, uMsg, wParam, lParam);
            if (lResult != HTCLIENT) {
                return lResult;
            }
            int x = (short) (lParam & 0xffff);
            int y = (short) ((lParam >> 16) & 0xffff);
            if (x >= 600 && x <= 620 && y >= 400 && y <= 420) {
                return HTCLOSE;
            }
            return HTCLIENT;
        }
    }
}
