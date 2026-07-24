# Halo Issue 插件

[![Halo](https://img.shields.io/badge/Halo-%3E%3D2.23.0-blue)](https://www.halo.run/)
[![License](https://img.shields.io/badge/License-GPL--3.0-green)](LICENSE)

作者：[Akagi_Zen](https://github.com/fhfzfy1231/halo-plugin-issue)

适用于 Halo 2.23.0 及以上版本。插件为 Halo 提供独立的 Issue 管理系统，支持 Issue 列表、模板、标签、经办人、状态流转、评论和系统操作时间线。

Issue、模板和标签统一收纳在 Console 侧栏的 `Issue` 折叠菜单中。

访客可直接访问：

```text
https://你的域名/issues
```

## 功能

- Console 侧栏提供 `Issue` 父级菜单，展开后可进入 `Issue 列表`、`Issue 模板` 和 `Issue 标签`。
- 提供独立的 Issue 前台列表、分页、搜索、新建、详情和评论页面。
- 前台固定入口为 `/issues`，Issue 详情地址为 `/issues/{Issue名称}`。
- Issue 列表显示全部 Issue，不要求先选择主体。
- 支持模板创建 Issue，并按模板配置显示自定义字段。
- 支持处理中、等待中、已关闭等状态流转，并在时间线中记录状态变化。
- 支持分配、移除和修改经办人，并在时间线中记录经办人变化。
- 支持添加和移除标签，并在时间线中记录标签变化。
- Issue 详情页统一展示正文、普通评论和系统操作事件，并按照创建时间排序。
- 桌面端详情页右侧经办人、标签和通知区域支持 Sticky 固定显示。
- 支持关键词、模板、标签、状态、发布者、日期与排序筛选。

## 使用方法

1. 在 Halo 后台进入“插件”，上传构建产生的 JAR 并启用。
2. 启用后，Console 侧栏会出现 `Issue` 折叠菜单，可管理 Issue、模板和标签。
3. 前台访问 `/issues` 查看 Issue 列表、创建 Issue 或进入 Issue 详情页。
4. 拥有相应权限的用户可在 Issue 详情页修改状态、经办人和标签，并查看完整操作时间线。

## 本地构建

环境要求：JDK 21、Node.js 20+、pnpm 10.12.4。项目已包含 Gradle Wrapper，正常构建不需要单独安装 Gradle。

```bash
cd ui
corepack enable
pnpm install --frozen-lockfile
cd ..
./gradlew clean build
```

Windows：

```powershell
cd ui
corepack enable
pnpm install --frozen-lockfile
cd ..
.\gradlew.bat clean build
```

构建完成后，插件 JAR 位于：

```text
build/libs/
```

`packages/issue-static` 是访客端资源源码。如修改前台脚本或样式，需要先在该目录执行构建，并将生成资源同步到 `src/main/resources/static/webAssets/`。

## 开发模式

```bash
./gradlew haloServer
```

该命令会启动 Halo 开发环境并加载插件。详细要求以 Halo 官方插件开发文档为准。

## 许可

GPL-3.0
