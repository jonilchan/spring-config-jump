# Spring Config Jump

[English](README.md)

IntelliJ IDEA 插件，实现 Spring 配置文件属性与代码引用之间的双向跳转导航。

## 功能

- **配置 -> 代码**：在 `application.properties` / `application.yml` 文件中，点击行号旁的 gutter 图标，直接跳转到引用该属性的代码位置
- **代码 -> 配置**：在 `@Value`、`@ConfigurationProperties` 等 Spring 注解上，点击 gutter 图标，跳转到配置文件中的属性定义
- **Ctrl+Click**：在属性 key 或注解值上按 Ctrl+Click（macOS 为 Cmd+Click）直接跳转

## 支持的引用类型

| 引用类型 | 示例 |
|---|---|
| `@Value` | `@Value("${spring.datasource.url}")` |
| `@ConfigurationProperties` | `@ConfigurationProperties(prefix = "spring.datasource")` |
| `Environment.getProperty()` | `env.getProperty("spring.datasource.url")` |
| `@ConditionalOnProperty` | `@ConditionalOnProperty(name = "feature.enabled")` |

## 支持的配置文件格式

- `application.properties`
- `application.yml` / `application.yaml`
- 多环境配置（如 `application-dev.yml`、`application-prod.properties`）
- `bootstrap.properties` / `bootstrap.yml`

## 环境要求

- IntelliJ IDEA 2023.2+
- Java 17+

## 安装方式

1. 从 `build/distributions/` 目录获取最新的 zip 包
2. 打开 IntelliJ IDEA：**Settings -> Plugins -> Install Plugin from Disk...**
3. 选择 zip 文件，重启 IDE 即可

## 构建

```bash
# macOS / Linux
./build.sh build

# Windows
build.bat build
```

或直接使用 Gradle：

```bash
./gradlew buildPlugin
```

构建产物位于 `build/distributions/application-jump-<version>.zip`。

## 开发调试

启动一个加载了插件的沙箱 IDEA 实例：

```bash
# macOS / Linux
./build.sh run

# Windows
build.bat run
```

## 项目结构

```
src/main/kotlin/com/github/applicationjump/
├── index/       # FileBasedIndex 索引，加速大项目属性搜索
├── search/      # 属性搜索工具，查找代码引用和配置定义
├── provider/    # LineMarkerProvider，提供 gutter 导航图标
├── reference/   # ReferenceContributor，提供 Ctrl+Click 跳转
└── util/        # 工具类（属性 key 解析、Spring 注解解析）
```

## 许可证

MIT
