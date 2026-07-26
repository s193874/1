# PlayTranslate Quick Bubble 二改补丁

对上游 [dominostars/playtranslate](https://github.com/dominostars/playtranslate)（commit `23248d8`）的修改包。
CI 会自动拉取上游源码、套用本补丁、编译出 APK（见 Actions 的 `PlayTranslate-QuickBubble-debug-apk` 产物）。

## 功能

- **悬浮球快捷菜单**：点击悬浮球弹出三大按钮——截图翻译 / 实时翻译 / 双语注释（原文下方加注释式译文）
- **悬浮球换肤**：自选图片、透明度、大小（菜单里的 ✦ 按钮）
- **空闲零打扰**：悬浮球闲置时不占前台服务、无常驻通知、不截屏；只在发起翻译时工作
- **翻译浮层外观**：设置 → 外观 → 翻译浮层，可选自适应/深色/浅色背景 + 背景不透明度滑条
- **框选体验修复**：✕/✓ 操作条自动避开框选区域；点击框选区外空白直接取消
- **设置精简**：一级设置只留捕获、翻译服务、外观；快捷键 / Anki / 词典 / 朗读收进「高级功能」折叠组
- **默认更快**：实时翻译默认间隔 0.8s；默认语言对 英语 → 系统语言；配置 OpenAI 兼容 API（自定义 base URL + key）后翻译走 API

## 使用

```bash
# 本地套用（需先 checkout 上游 23248d8）
python3 apply_mod.py <playtranslate仓库路径> [--dry-run] [--force]
```

或直接 push 本仓库，GitHub Actions 会自动构建 APK。

## 文件说明

- `files/` — 覆盖到上游仓库的文件（新增 + 修改）
- `upstream-checksums.json` — 被覆盖的上游原始文件的 sha256（防止套错版本）
- `apply_mod.py` — 套用脚本（复制 files/ + 在 AndroidManifest 注册 Activity）
