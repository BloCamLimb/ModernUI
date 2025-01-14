/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;
import static org.lwjgl.system.JNI.callPPI;

/**
 * Native bindings to dwmapi.dll.
 *
 * @hidden
 */
public class Dwmapi {

    private static final SharedLibrary DWMAPI = Library.loadNative(Dwmapi.class, "org.lwjgl", "dwmapi");

    public static final class Functions {

        private Functions() {
        }

        public static final long
                DwmExtendFrameIntoClientArea = apiGetFunctionAddress(DWMAPI, "DwmExtendFrameIntoClientArea"),
                DwmGetWindowAttribute = apiGetFunctionAddress(DWMAPI, "DwmGetWindowAttribute");

    }

    public static SharedLibrary getLibrary() {
        return DWMAPI;
    }

    @NativeType("HRESULT")
    public static int DwmExtendFrameIntoClientArea(@NativeType("HWND") long hWnd,
                                                   @NativeType("const MARGINS *") long pMarInset) {
        long __functionAddress = Functions.DwmExtendFrameIntoClientArea;
        return callPPI(hWnd, pMarInset, __functionAddress);
    }

    @NativeType("HRESULT")
    public static int DwmGetWindowAttribute(@NativeType("HWND") long hWnd,
                                            @NativeType("DWORD") int dwAttribute,
                                            @NativeType("PVOID") long pvAttribute,
                                            @NativeType("DWORD") int cbAttribute) {
        long __functionAddress = Functions.DwmGetWindowAttribute;
        return callPPI(hWnd, dwAttribute, pvAttribute, cbAttribute, __functionAddress);
    }
}
