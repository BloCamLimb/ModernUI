# ModernUI
[![MavenCore](https://img.shields.io/badge/dynamic/xml?color=orange&label=Core%20Version&query=%2Fmetadata%2Fversioning%2Flatest&url=https%3A%2F%2Fmaven.izzel.io%2Freleases%2Ficyllis%2Fmodernui%2FModernUI-Core%2Fmaven-metadata.xml)]()
[![Discord](https://img.shields.io/discord/696234198767501363?color=green&label=Discord&style=flat)](https://discord.gg/kmyGKt2)
### Overview
ModernUI is a cross-platform UI runtime focused on high-performance rendering, layout, and text systems for desktop environments.

The project explores a retained UI model with GPU-accelerated rendering and low-level graphics access,
designed to scale from simple interfaces to highly complex, component-dense scenes while maintaining consistent, pixel-perfect output across platforms.

Core subsystems—rendering, layout, text shaping, and UI components—are largely in place.
Platform window backends (e.g. AWT, GLFW) and multi-window support are under active development.

ModernUI is designed as a UI runtime, not just a rendering library,
with explicit control over layout, text, rendering, and lifecycle within a single system.

Unlike approaches that wrap native rendering libraries or rely on declarative UI layers on top of them,
ModernUI maintains a cohesive retained UI model with direct control over rendering and resource lifetime.

ModernUI has been proven to operate under strict host constraints, including shared window ownership and shared GPU contexts,
without introducing parallel rendering stacks.

ModernUI is currently intended for advanced UI scenarios, custom engines, and architectural exploration, rather than turnkey application development.


#### What this project focuses on

* UI tree and layout system
* Text shaping and rich-text layout (HarfBuzz-based)
* GPU-accelerated rendering pipeline (both 2D and 3D)
* Integration with modern graphics APIs via LWJGL

ModernUI provides deterministic, platform-independent rendering.
It ensures pixel-identical visual results across supported platforms, independent of native UI toolkits or system rendering behavior.

Native-like appearance can be provided as an optional layer through styling and conventions, without compromising rendering determinism.

#### What ModernUI is NOT
* Not a Swing/JavaFX backend
* Not a drop-in, turnkey UI framework yet
* Not a theme or a look-and-feel layer: any visual theme can be implemented in ModernUI
* Not platform-native rendering: it provides platform-independent deterministic rendering (optional native-like styling)

### Documentation
(needs a link here when available)

Repository of the documentation: https://github.com/BloCamLimb/ModernUI-Docs

### FAQ
(needs a link to documentation here)

### Platform & runtime environment
ModernUI is designed to operate on top of the JVM and modern GPU APIs.

ModernUI supports the following platforms:
* Operating systems
  + Windows
  + Linux (X11, Wayland)
  + macOS
* Runtime
  + Java 17 or later
  + LWJGL core
  + ICU4J
  + SLF4J
  + fastutil
* Graphics APIs  
  At least one of the following should be available on the target system:
  + OpenGL 3.3 or later
  + OpenGL ES 3.0 or later
  + Vulkan 1.1 or later
* Graphics bindings
  + Uses LWJGL for platform and graphics API bindings
* Implementation model
  + ModernUI itself is implemented entirely in pure Java
  + No native UI or graphics libraries are bundled or required by ModernUI
  + The rendering engine does not depend on Skia, NanoVG, or similar native graphics stacks

This design allows ModernUI to integrate tightly with host applications while remaining independent of platform-native
UI toolkits and native rendering frameworks.

### Existing integration: Minecraft
While platform window backends for standalone desktop applications are still under development,
ModernUI is already fully integrated and production-used within the Minecraft modding environment.

In this setup, ModernUI runs without its own window system, sharing:
* the same JVM process
* the same application window
* the same GPU context
* the same dependency stack provided by Minecraft

ModernUI introduces no additional native libraries and no parallel graphics or windowing stack.
All required dependencies (including LWJGL, ICU4J, and fastutil) are supplied by the Minecraft environment and used directly.

The Minecraft integration is implemented in [ModernUI-MC](https://github.com/BloCamLimb/ModernUI-MC), a derivative project
that adds host-specific code and is ultimately packaged together with ModernUI into a single mod jar.

Releases for Minecraft Mod are available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-ui) and
[Modrinth](https://modrinth.com/mod/modern-ui).  
### License
* ModernUI
  - Copyright (C) 2019-2026 BloCamLimb and contributors
  - [![License-LGPL--3.0--or--later](https://img.shields.io/badge/License-LGPL--3.0--or--later-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0.en.html)

All modules of the ModernUI project are licensed under GNU Lesser General Public License v3.0 or later (LGPL-3.0-or-later).  
For the full license text, see [LICENSE](LICENSE). For additional license notices, see [NOTICE](NOTICE).
### Dependencies
ModernUI requires the following third-party libraries to compile and run:
* LWJGL (https://github.com/LWJGL/lwjgl3)
  - License: BSD-3-Clause (https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
  - Modules: lwjgl-core, lwjgl-glfw, lwjgl-jemalloc lwjgl-opengl lwjgl-stb lwjgl-tinyfd lwjgl-vma lwjgl-vulkan
* fastutil (https://github.com/vigna/fastutil)
  - License: Apache-2.0 (https://github.com/vigna/fastutil/blob/master/LICENSE-2.0)
* ICU4J (https://github.com/unicode-org/icu)
  - License: Unicode-3.0 (https://github.com/unicode-org/icu/blob/main/LICENSE)
* SLF4J (https://github.com/qos-ch/slf4j)
  - License: MIT License (https://github.com/qos-ch/slf4j/blob/master/LICENSE.txt)
* Arc3D (https://github.com/BloCamLimb/Arc3D)
  - License: LGPL-3.0-or-later (https://github.com/BloCamLimb/Arc3D/blob/master/LICENSE)
  - Note: Arc3D is actually part of ModernUI project, but its upstream is in an independent repository.

The above dependencies are flat, with no nested dependencies.  
There are also some annotation libraries that are used for compilation but not required for runtime, not listing here.