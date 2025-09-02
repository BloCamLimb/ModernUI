# Modern UI
[![MavenCore](https://img.shields.io/badge/dynamic/xml?color=orange&label=Core%20Version&query=%2Fmetadata%2Fversioning%2Flatest&url=https%3A%2F%2Fmaven.izzel.io%2Freleases%2Ficyllis%2Fmodernui%2FModernUI-Core%2Fmaven-metadata.xml)]()
[![Discord](https://img.shields.io/discord/696234198767501363?color=green&label=Discord&style=flat)](https://discord.gg/kmyGKt2)
### Description
**Modern UI** (by Icyllis Milica), also known as **ModernUI**, is a desktop application framework designed for standalone 2D and 3D rendering software development.
It makes use of modern 3D graphical APIs and technologies to provide high real-time rendering performance.
This framework is similar to JavaFX or Android, with a complete set of event loops, rendering systems, and UI components,
which are also suitable for game development.

There is also an official version that extends to Minecraft, it combines Modern UI with Minecraft and
provides a number of additional features and modding APIs. See [ModernUI-MC](https://github.com/BloCamLimb/ModernUI-MC) repository.

What are the advantages?  
Powerful UI functionality, good internationalization support, complete text layout engine based on HarfBuzz.
This framework has a powerful graphics engine, which is good to OpenGL 3.3 and OpenGL 4.5 core profiles and
is specifically optimized for desktop GPUs, some engine designs are better than Google Skia.

Modern UI is published on [IzzelAliz Repo](https://maven.izzel.io/).  
Releases for Minecraft Mod are available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-ui) and
[Modrinth](https://modrinth.com/mod/modern-ui).  
If you have any questions, feel free to join our [Discord](https://discord.gg/kmyGKt2) server.
### License
* Modern UI
  - Copyright (C) 2019-2025 BloCamLimb and contributors
  - [![License-LGPL--3.0--or--later](https://img.shields.io/badge/License-LGPL--3.0--or--later-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0.en.html)

All modules of the Modern UI project are licensed under GNU Lesser General Public License v3.0 or later (LGPL-3.0-or-later).  
For the full license text, see [LICENSE](LICENSE). For additional license notices, see [NOTICE](NOTICE).
### Dependencies
Modern UI requires the following third-party libraries to compile and run:
* LWJGL (https://github.com/LWJGL/lwjgl3)
  - License: BSD-3-Clause (https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
  - Modules: lwjgl-core, lwjgl-glfw, lwjgl-jemalloc lwjgl-openal lwjgl-opengl lwjgl-stb lwjgl-tinyfd lwjgl-vma lwjgl-vulkan
* fastutil (https://github.com/vigna/fastutil)
  - License: Apache-2.0 (https://github.com/vigna/fastutil/blob/master/LICENSE-2.0)
* ICU4J (https://github.com/unicode-org/icu)
  - License: Unicode-3.0 (https://github.com/unicode-org/icu/blob/main/LICENSE)
* SLF4J (https://github.com/qos-ch/slf4j)
  - License: MIT License (https://github.com/qos-ch/slf4j/blob/master/LICENSE.txt)
* Arc3D (https://github.com/BloCamLimb/Arc3D)
  - License: LGPL-3.0-or-later (https://github.com/BloCamLimb/Arc3D/blob/master/LICENSE)
  - Note: Arc3D is part of Modern UI project, but its upstream is in an independent repository.
    Currently the Modern UI Core jar contains all the Arc3D code, but in the future we will make
    Arc3D a standalone release.

The above dependencies are flat, with no nested dependencies.  
There are also some annotation libraries that are used for compilation but not required for runtime, not listing here.
### Documentation
Developer Guide (WIP)  
Specification (WIP)  
[JavaDoc](https://blocamlimb.github.io/ModernUI/javadoc/index.html)  

#### Environment requirements
Modern UI can run on any platform as long as it supports:
- Java SE 17 or above
- GLFW 3.4
- One of 3D APIs:
  * OpenGL 3.3 or above
  * OpenGL ES 3.0 or above
  * Vulkan 1.1 or above

Note: LWJGL natives currently do not support big-endian CPUs, although Java and Modern UI support.
#### Gradle configuration
```
repositories {
    maven {
        name 'IzzelAliz Maven'
        url 'https://maven.izzel.io/releases/'
    }
}
dependencies {
    implementation "icyllis.modernui:ModernUI-Core:${modernui_core_version}"
    // apply appropriate LWJGL platform here (mandatory)
    // apply other Modern UI modules (optional)
}
```

#### Building Modern UI
JDK 21 is preferred.
The build command: `gradlew build`

Modern UI requires [Arc 3D](https://github.com/BloCamLimb/Arc3D) codebase to build.
Arc 3D is a low-level graphics engine and frequently updated. It won't be published
on Maven repository. A snapshot is merged into this repository in `/external` subdirectory, and
all `Arc3D` classes and sources will be included in `ModernUI-Core`. You may follow these steps
when you want to update it.
```shell
// add remote if not
git remote add -f --no-tags arc3d git@github.com:BloCamLimb/Arc3D.git
// fetch if not
git fetch --no-tags arc3d
// delete the old code if any
git rm -rf external/Arc3D
// merge arc3d/master branch
git merge -s ours --no-commit arc3d/master --allow-unrelated-histories
// read the root directory of arc3d/master into 'external/Arc3D'
git read-tree --prefix=external/Arc3D -u arc3d/master:
git commit
```
Note: You must not make any local changes to `/external`.