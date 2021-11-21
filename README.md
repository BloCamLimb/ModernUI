# Modern UI
[![CurseForge](http://cf.way2muchnoise.eu/full_352491_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/modern-ui)
[![CurseForge](http://cf.way2muchnoise.eu/versions/For%20Minecraft_352491_all.svg)](https://www.curseforge.com/minecraft/mc-mods/modern-ui)
[![MavenCore](https://img.shields.io/badge/dynamic/xml?color=orange&label=Core%20Version&query=%2Fmetadata%2Fversioning%2Flatest&url=https%3A%2F%2Fmaven.izzel.io%2Freleases%2Ficyllis%2Fmodernui%2FModernUI-Core%2Fmaven-metadata.xml)]()
[![Discord](https://img.shields.io/discord/696234198767501363?color=green&label=Discord&style=flat)](https://discord.gg/kmyGKt2)
### Description
Modern UI is a UI framework for desktop application development.
Many of the structures are similar to Android, but the implementation can be quite different.
The render engine uses OpenGL 4.5 core profile and can be multi-threaded, so the performance is much better than 2D graphics libraries drawn by CPU.
The text engine is unicode-based and uses HarfBuzz and ICU4j, so it has a broad compatibility for various languages.

This project is still in early stages.  
Releases for Minecraft Mod are available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-ui).  
If you have any questions, feel free to join our [Discord](https://discord.gg/kmyGKt2) server.
### License
* Modern UI
  - Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
  - [![License](https://img.shields.io/badge/License-LGPL--3.0--or--later-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0.en.html)
  - Copyright (C) 2006 The Android Open Source Project
  - [![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0)
* Textures, Shaders, Models, Documents, Translations
  - Copyright (C) 2019-2021 BloCamLimb et al.
  - [![License](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-yellow.svg?style=flat-square)](https://creativecommons.org/licenses/by-nc-sa/4.0/)
* Sounds
  - [![License](https://img.shields.io/badge/License-No%20Restriction-green.svg?style=flat-square)](https://creativecommons.org/publicdomain/zero/1.0/)
* Third Party Libraries
  - [caffeine](https://github.com/ben-manes/caffeine) by Ben Manes
  - [flexmark-java](https://github.com/vsch/flexmark-java) by Atlassian Pty Ltd, Vladimir Schneider
  - [fastutil](https://github.com/vigna/fastutil) by Vigna
  - Apache Log4j, IBM ICU4j, LWJGL
### Adding Modern UI to your project
#### Environment requirements
- Windows, Linux or macOS
- JDK 16.0.1 or above
- OpenGL 4.5 or above (see Mesa Zink for macOS users)
- (Optional) Forge 1.17.1-37.0.97
#### Gradle configuration
```
repositories {
    maven {
        name 'IzzelAliz Maven'
        url 'https://maven.izzel.io/releases/'
    }
}
dependencies {
    implementation "icyllis.modernui:ModernUI-Core:${modernui_version}"
}
```
##### ForgeGradle 5
You need to regenerate run configurations.
```
minecraft {
    runs {
        client {
            property 'mixin.env.remapRefMap', 'true'
            property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"
        }
        server {
            property 'mixin.env.remapRefMap', 'true'
            property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"
        }
    }
}
configurations {
    library
    implementation.extendsFrom library
}
minecraft.runs.all {
    lazyToken('minecraft_classpath') {
        configurations.library.copyRecursive().resolve().collect { it.absolutePath }.join(File.pathSeparator)
    }
}
dependencies {
    library "icyllis.modernui:ModernUI-Core:${modernui_version}"
    implementation fg.deobf("icyllis.modernui:ModernUI-Forge:${minecraft_version}-${modernui_version}")
}
```
### Debugging Modern UI
Note that for debugging in Minecraft environment, you need to pack shaders in the core (sub)project to a resource pack in the run directory.
### Screenshots
![new3.gif](https://i.loli.net/2021/09/27/yNsL98XtpKP7UVA.gif)
![new2](https://i.loli.net/2021/09/24/TJjyzd6oOf5pPcq.png)
![new](https://i.loli.net/2021/03/24/nMZhJaiz7qDp2xF.png)
#### out-of-date
![a](https://i.loli.net/2020/05/15/fYAow29d4JtqaGu.png)
![b](https://i.loli.net/2020/04/10/LDBFc1qo5wtnS8u.png)
