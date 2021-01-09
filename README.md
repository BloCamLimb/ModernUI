# Modern UI
### Homepage [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-ui)
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
#### Development environment
- Liberica OpenJDK 8u275
- Forge 1.16.4-35.1.0
#### Gradle configuration
Add followings to `build.gradle`
```
repositories {
    maven {
        name 'CurseForge Maven'
        url 'https://www.cursemaven.com/'
    }
}
dependencies {
    compile fg.deobf("curse.maven:ModernUI:[-version]")
}
```
There's currently no stable builds for development