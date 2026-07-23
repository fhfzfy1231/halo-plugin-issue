# Issue

面向 Halo 2 的独立 Issue 管理插件。

Issue、模板和标签均可从独立的同级菜单进入，不依赖“依托主体”。

## 功能

- Console 侧栏提供 `Issue 列表`、`Issue 模板`、`Issue 标签` 三个同级入口。
- Issue 列表显示全部 Issue，不要求先选择主体。
- 点击“查看内容”可查看 Issue 正文、状态、作者、标签和评论。
- 拥有 Issue 管理权限的用户可在列表中直接勾选或移除标签，操作方式接近 GitHub Issues。
- 支持新建、编辑、审核、关闭、重新打开和删除 Issue。
- 支持关键词、模板、标签、状态、发布者、日期与排序筛选。

## 环境要求

- Halo `>= 2.23.0`
- Java 21
- Node.js 20
- pnpm 10.12.4

## 构建

```bash
cd ui
corepack enable
pnpm install --frozen-lockfile
cd ..
./gradlew clean build
```

构建完成后，插件 JAR 位于 `build/libs/`。

## 安装

进入 Halo Console 的插件管理页面，上传构建得到的 JAR 并启用插件。

## 项目信息

- 作者：[Akagi_Zen](https://github.com/fhfzfy1231)
- 仓库：`fhfzfy1231/halo-plugin-issue`
- 许可证：GPL-3.0
