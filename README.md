# Pigeon Core

Pigeon Core is a **framework mod** for Minecraft Forge **1.20.1**. It provides:

- auto registration of items, entities and block entities (`RegisterFactory`),
- a gun framework (muzzles, attachments, magazines, reload/hold actions, shells, bullet holes),
- a GUI toolkit (config screen, sliders, checkboxes, text fields, structure tool),
- networking helpers built on the modern `SimpleChannel` (plain POJO packets),
- first-class [GeckoLib 4.8.2](https://github.com/bernie-G/geckolib) support (`EGun`, `GeoEMob`, `EAttachment`, `GeoController`),
- location/path utilities, build helpers and world utils.

| | |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 1.20.1-47.3.0 (minimum) |
| Java | 17 |
| GeckoLib | 4.8.2 (required) |
| License | LGPL 2.1 (see [LICENSE](LICENSE)) |
| Coordinates | `software.hacker_E303:pigeon_core:1.0.0-forge-1.20.1` |

## Using it in a consumer mod (Gradle)

The published artifact is a **deobfuscated** jar. Consumers **never** decompile
or deobfuscate it: Maven handles the metadata, and GeckoLib (the only external
Minecraft dependency) is re-deobfuscated by *the consumer's* ForgeGradle from
public Maven coordinates — exactly the same as any other Mod dependency.

## `settings.gradle`

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net/' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.7.0'
}

rootProject.name = 'my_mod'
```

## `build.gradle`

```groovy
plugins {
    id 'net.minecraftforge.gradle' version '6.0.24'
}

version = '1.0.0'
group   = 'com.example.my_mod'

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings channel: 'official', version: '1.20.1'
    copyIdeResources = true

    runs {
        client {
            workingDirectory project.file('run')
            // pigeon_core ships as a MOD, not just a classpath library:
            // declare it in `mods` or it will silently not load in the dev client.
            mods {
                my_mod      { source sourceSets.main }
                pigeon_core { } // external mod, loaded from the resolved jar
            }
        }
        server {
            workingDirectory project.file('run')
            mods {
                my_mod      { source sourceSets.main }
                pigeon_core { }
            }
        }
    }
}

repositories {
    mavenCentral()
    // Required because pigeon_core has a transitive GeckoLib dependency:
    maven { url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' }

    // Pick ONE of the pigeon_core locations:

    // [A] Local (fastest, after: cd pigeon_core && ./gradlew publishToMavenLocal)
    mavenLocal()

    // [B] GitHub Packages (hacker_E303/pigeon_core)
    //     Needs a classic PAT with the `read:packages` scope (free), stored as
    //     GITHUB_USERNAME / GITHUB_TOKEN in the environment (or in gradle.properties).
    maven {
        url = uri('https://maven.pkg.github.com/hacker_E303/pigeon_core')
        credentials {
            username = System.getenv('GITHUB_USERNAME') ?: 'hacker_E303'
            password = System.getenv('GITHUB_TOKEN')
        }
    }
}

dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.3.0'

    // `api` so your code can use pigeon_core's PUBLIC API, including the
    // GeckoLib types it exposes (EGun, GeoEMob, GeoController, ...).
    api 'software.hacker_E303:pigeon_core:1.0.0-forge-1.20.1'
}
```

## ⚠️ GeckoLib refmap gotcha (dev environment only)

If your mod also uses GeckoLib directly and Mixin, the ForgeGradle deobfuscated
GeckoLib 4.8.2 jar in the local dev cache has a **broken Mixin refmap**: the
refmap entry for `TextureManager#getTexture` points at an SRG name
(`m_118506_`) that does not exist in the deobfuscated bytecode, so GeckoLib's
`TextureManagerMixin` silently fails to apply in the dev client. It works in
production (production GeckoLib is still obfuscated, so the refmap there is fine).

This project ships a dev-time fix, `fix_geckolib_refmap.gradle`:

```groovy
// inside the CONSUMER project's build.gradle (same machine that built pigeon_core):
apply from: '/path/to/pigeon_core/fix_geckolib_refmap.gradle'
```

It patches the deobfuscated GeckoLib jar in `~/.gradle/caches/forge_gradle/`
in place after deobfuscation completes. No action is needed for players
running the production mod file.

## Building & publishing (maintainers)

```sh
# local build
./gradlew clean build

# publish to the local Maven repository (~/.m2) — no credentials needed
./gradlew publishToMavenLocal

# publish to GitHub Packages (free) — requires the token in the environment
GITHUB_USERNAME=hacker_E303 GITHUB_TOKEN=*** \
    ./gradlew publish
```

The published POM contains:

- coordinates `software.hacker_E303:pigeon_core:1.0.0-forge-1.20.1`, plus the
  `-sources` and `-javadoc` artifacts,
- the `api` transitive dependency
  `software.bernie.geckolib:geckolib-forge-1.20.1:4.8.2`,
- license **LGPL 2.1** and SCM `https://github.com/hacker_E303/pigeon_core`.

Consumers must also keep the GeckoLib Cloudsmith repository in their own
`repositories` block so the transitive GeckoLib dependency resolves.
No deobfuscation of this jar is required by consumers.

## Project layout

| Package | Content |
|---|---|
| `software.hacker_E303.pigeon_core` | `PigeonCore` (mod entry), `RegisterFactory`, `EResources` |
| `...pigeon_core.actions` | gun behaviors (`IShoot`, `IReload`, `IBasic`, `IGratherEvent`, `IGeneration`) |
| `...pigeon_core.client.gui` | `PigeConfigScreen`, sliders, checkboxes, text fields, … |
| `...pigeon_core.client.gun` | gun animation and rendering |
| `...pigeon_core.client.item` | item atlas / model handlers |
| `...pigeon_core.client.entity` | `AnimalGeo`, renderers |
| `...pigeon_core.common.config` | `ConfigContext`, `ConfigEntry`, `ConfigFolder`, … |
| `...pigeon_core.geo` | `EGun`, `GeoEMob`, `EAttachment`, `GeoController`, … |
| `...pigeon_core.mixins` | Mixin targets (inventory, item stack, spawn egg, …) |
| `...pigeon_core.main` | `AutoRegister`, event handlers, `PigeNetworking` |
| `...pigeon_core.main.event.network.*` | all packet classes + `RouterUtils` |
| `...pigeon_core.util` | `BetterMath`, `BetterTexts`, `BetterData`, `PigeUtils`, world/locator utils |
| `...pigeon_core.item` | `EGun` (item form), `EMagazine`, `EItem` base |

## License

LGPL 2.1 — see `LICENSE`.

