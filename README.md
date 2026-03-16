# Spring Config Jump

[中文文档](README_CN.md)

IntelliJ IDEA plugin for bidirectional navigation between Spring application configuration properties and their code references.

## Features

- **Config -> Code**: Click gutter icons in `application.properties` / `application.yml` to jump to code that references the property key
- **Code -> Config**: Click gutter icons on Spring annotations to jump to the property definition in configuration files
- **Ctrl+Click**: Direct navigation via Ctrl+Click (Cmd+Click on macOS) on property keys and annotation values

## Supported Reference Types

| Reference Type | Example |
|---|---|
| `@Value` | `@Value("${spring.datasource.url}")` |
| `@ConfigurationProperties` | `@ConfigurationProperties(prefix = "spring.datasource")` |
| `Environment.getProperty()` | `env.getProperty("spring.datasource.url")` |
| `@ConditionalOnProperty` | `@ConditionalOnProperty(name = "feature.enabled")` |

## Supported File Formats

- `application.properties`
- `application.yml` / `application.yaml`
- Profile-specific variants (e.g., `application-dev.yml`)
- `bootstrap.properties` / `bootstrap.yml`

## Requirements

- IntelliJ IDEA 2023.2+
- Java 17+

## Installation

1. Download the latest release zip from `build/distributions/`
2. In IntelliJ IDEA: **Settings -> Plugins -> Install Plugin from Disk...**
3. Select the zip file, restart IDE

## Build

```bash
# macOS / Linux
./build.sh build

# Windows
build.bat build
```

Or use Gradle directly:

```bash
./gradlew buildPlugin
```

The output zip is located at `build/distributions/application-jump-<version>.zip`.

## Development

Launch a sandbox IDEA instance with the plugin loaded:

```bash
# macOS / Linux
./build.sh run

# Windows
build.bat run
```

## License

MIT
