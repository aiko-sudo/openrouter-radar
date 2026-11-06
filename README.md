# 模型雷达 — OpenRouterRadar

> 轻量 Android App：实时获取 OpenRouter 最新 / 免费 / 好用模型，带新模型提醒。

## 功能

- 直连 `https://openrouter.ai/api/v1/models`（无需 API Key）
- 5 个视图：最新（按 created 倒序）、免费（prompt/completion 均为 0）、好用（免费+新+大上下文综合分）、全部、收藏
- 搜索过滤、复制模型 ID、跳官网详情、收藏
- 提醒：默认每 6 小时（可选 2h/24h）后台检查新增模型，弹通知；AlarmManager 精确定时，重启动自动恢复
- 零第三方依赖（仅标准库 + org.json）

## 安装

安装 APK 后打开，点「刷新」拉取列表。顶部「提醒开」默认开启。

## 构建

```bash
cd ~/uclaw/project/openrouter-radar
./gradlew assembleRelease
ls app/build/outputs/apk/release/
```

Jetson/aarch64 构建前确认 `~/android-sdk/aapt2-override/aapt2 version` 可用，`gradle.properties` 已指向该 wrapper（见 `aarch64-android-apk-build` skill）。

## 先查后写

新增功能前先检索 GitHub 活跃开源同类实现，优先借鉴验证过的设计，不要直接复制来源不明代码。
