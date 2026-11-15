# 基础背景

Are是曾经的在Minecraft中还原彩虹六号：围攻的想法的延续。

允许使用git命令行翻阅下列仓库更久远的提交记录，已有HTTPS模式下的凭据。
参考曾经未完成的项目： 
H:\Minecraft Develop\Rainbow6
H:\Minecraft Develop\MC-R6mod
https://holojaneway.uss-shenzhou.cn/r6ms

在docs/文件夹可以读到本项目的所有文档。

# 文件读写

这是一个半vibe coding项目。

LLM被允许承担绝大多数的工作，在默认情况下，允许直接写入源代码或修改文件。以下是禁止写入的内容：
- docs/文件夹，style.md除外

所有生成的代码都需要经过手动审查才能提交。以下是不需要审查的内容：
- GitHub 工作流文件

# 代码风格

本项目使用渐进式的代码编写风格。在修改或写入文件前，确保已经完整阅读docs/style.md。

# 依赖库

不允许LLM自行添加依赖库。

所有Minecraft 26.3和NeoForge依赖库均可用。

GUI、网络包使用我们自己的T88。源代码位于 H:\Minecraft Develop\t88 ，使用 LocalMaven 引入。

武器系统参考 TimelessAndClassicsZero （TACZ） 模组。参考： H:\Minecraft Develop\TACZ ，其中有 legacy 和 respawn 两个版本。 
在对于同一内容实现不同时，以 respawn 为准。respawn 没有实现的，以 legacy 为准。
TACZ内置的武器等美术资产使用CC BY-NC-ND 4.0许可。已获得作者特别许可，可以不受限制地将其用于开发环境。
TACZ的代码使用GPL3许可，鉴于武器系统代码只占本项目的一个部分，且由LLM生成的代码会经过足够的修改，同时本项目亦使用相同的许可，我们认为是兼容的。
