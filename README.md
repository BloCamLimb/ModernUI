# Modern UI
[![MavenCore](https://img.shields.io/badge/dynamic/xml?color=orange&label=Core%20Version&query=%2Fmetadata%2Fversioning%2Flatest&url=https%3A%2F%2Fmaven.izzel.io%2Freleases%2Ficyllis%2Fmodernui%2FModernUI-Core%2Fmaven-metadata.xml)]()
[![Discord](https://img.shields.io/discord/696234198767501363?color=green&label=Discord&style=flat)](https://discord.gg/kmyGKt2)
### Description
Modern UI (by Icyllis Milica) is a desktop application framework designed for standalone 2D and 3D rendering software development.
It makes use of modern 3D graphical APIs and technologies to provide high real-time rendering performance.
This framework is similar to JavaFX or Android, with a complete set of event loops, rendering systems, and UI components,
which are also suitable for game development.

There is also an official version that extends to Minecraft and Forge, it combines Modern UI with Minecraft and
provides a number of additional features and modding APIs. See [ModernUI-MC](https://github.com/BloCamLimb/ModernUI-MC) repository.

What are the advantages?  
Powerful UI functionality, good internationalization support, complete text layout engine based on HarfBuzz.
This framework has a powerful graphics engine, which is good to OpenGL 3.3 and OpenGL 4.5 core profiles and
is specifically optimized for desktop GPUs, some engine designs are better than Google Skia.

This project is still at a *relatively* early stage.  
Releases for Minecraft Mod are available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-ui).  
If you have any questions, feel free to join our [Discord](https://discord.gg/kmyGKt2) server.
### License
* Modern UI
  - Copyright (C) 2019-2024 BloCamLimb
  - [![License](https://img.shields.io/badge/License-LGPL--3.0--or--later-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0.en.html)
  - Copyright (C) 2006 The Android Open Source Project
  - [![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0)
* Runtime Libraries (not part of the ModernUI project)
  - [lwjgl](https://github.com/LWJGL/lwjgl3) licensed under BSD-3-Clause
  - [fastutil](https://github.com/vigna/fastutil) by Vigna, licensed under Apache-2.0
  - [log4j](https://github.com/apache/logging-log4j2) licensed under Apache-2.0
  - [icu4j](https://github.com/unicode-org/icu) by Unicode, Inc. see [LICENSE](https://github.com/unicode-org/icu/blob/main/LICENSE)
### Documentation
[JavaDoc](https://blocamlimb.github.io/ModernUI/javadoc/index.html)  
Specification (WIP)

#### Environment requirements
- Windows 10 or above, Linux, macOS 10.10 or above
- JDK 17.0.1 or above
- OpenGL 3.3 or above (OpenGL 4.3 is recommended)
- Vulkan 1.1 or above (WIP)

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