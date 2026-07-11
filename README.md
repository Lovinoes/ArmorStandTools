# ArmorStandTools
A fork of [ArmorStandTools](https://github.com/Cubeside/ArmorStandTools) by Cubeside
## Building
This project uses Gradle. If you don't already have a Gradle wrapper, generate one once with a
local Gradle install:
```bash
gradle wrapper --gradle-version 9.5.0
```
Then build the plugin jar:
```bash
./gradlew build
```
The finished jar will be at `build/libs/ArmorStandTools.jar`.
## Permissions
- `armorstandtools.unlimitedsize` — allows scaling an armor stand up to the
  `settings.unlimited-size-limit` instead of `settings.default-size-limit`
