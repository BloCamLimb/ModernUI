# Modern UI
### Homepage [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-ui)
### License
* Modern UI
  - Copyright (C) 2019-2020 BloCamLimb. All rights reserved. 
  - [![License](https://img.shields.io/badge/License-LGPLv3-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0.en.html)
* Textures, Shaders, Models
  - Copyright (C) 2019-2020 BloCamLimb. All rights reserved. 
  - [![License](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-yellow.svg?style=flat-square)](https://creativecommons.org/licenses/by-nc-sa/4.0/)
* Sounds, Translations
  - [![License](https://img.shields.io/badge/License-No%20Restriction-green.svg?style=flat-square)](https://creativecommons.org/publicdomain/zero/1.0/)
### Screenshots
![a](https://i.loli.net/2020/05/15/fYAow29d4JtqaGu.png)
![b](https://i.loli.net/2020/04/10/LDBFc1qo5wtnS8u.png)
### Adding Modern UI to your project
#### Development environment
- Java 8u251
- Forge 1.16.1-32.0.93
#### Gradle configuration
Add followings to `build.gradle`
```
plugins {
    id "com.wynprice.cursemaven" version "2.1.5"
}
```
```
dependencies {
    compileOnly fg.deobf("curse.maven:ModernUI:[FileID -userdev]")
    runtimeOnly fg.deobf("curse.maven:ModernUI:[FileID -universal]")
}
```
Latest version: (no stable build for 1.16.1) (FileID: )

