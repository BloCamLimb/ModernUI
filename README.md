# Modern UI
### Homepage
Releases for Minecraft Mod are available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-ui)
### License
* Modern UI
  - Copyright (C) 2019-2021 BloCamLimb. All rights reserved. 
  - [![License](https://img.shields.io/badge/License-LGPL--3.0-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0.en.html)
* Textures, Shaders, Models, Documents, Translations
  - Copyright (C) 2019-2021 BloCamLimb et al.
  - [![License](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-yellow.svg?style=flat-square)](https://creativecommons.org/licenses/by-nc-sa/4.0/)
* Sounds
  - [![License](https://img.shields.io/badge/License-No%20Restriction-green.svg?style=flat-square)](https://creativecommons.org/publicdomain/zero/1.0/)
### Screenshots
![a](https://i.loli.net/2020/05/15/fYAow29d4JtqaGu.png)
![b](https://i.loli.net/2020/04/10/LDBFc1qo5wtnS8u.png)
### Adding Modern UI to your project
#### Environment requirements
- Windows, Linux or Solaris
- JDK 11.0.8 or above (Compile against Java 8)
- OpenGL 4.3 or above
- Forge 1.16.5-36.0.1
#### Version information
There's currently no stable builds for development
#### Gradle configuration
Add followings to `build.gradle`
```
repositories {
    maven {
        name 'IzzelAliz Repo'
        url 'https://maven.izzel.io/releases'
    }
}

dependencies {
    // ForgeGradle 3
    compile fg.deobf("icyllis.modernui:ModernUI-Forge:{version}")

    // Fabric Loom 0.6
    modCompile "icyllis.modernui:ModernUI-Forge:{version}"
}
```