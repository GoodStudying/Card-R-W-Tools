# GitHub Actions 工作流更新指南

我已成功更新 Android 发布工作流，以满足您的需求。

## 更新内容

### 1. 触发条件
- **旧行为**: 每次推送到 `main` 分支都运行。
- **新行为**: 仅在以下情况运行：
    - 推送以 `v` 开头的 Tag (例如 `v1.2.0`)。
    - 在 "Actions" 选项卡手动触发。

### 2. APK 命名
- 生成的 APK 现在会自动重命名为 `BCardTools_版本号.apk`。
- 如果通过 Tag `v1.2.0` 触发，文件名为 `BCardTools_v1.2.0.apk`。
- 如果手动触发，文件名为 `BCardTools_manual-build.apk` (或其他标识)。

### 3. 自动发布 & 说明
- 推送 Tag 时，会自动创建一个 **GitHub Release**。
- 发布说明会自动读取项目根目录下的 `RELEASE_NOTES.txt` 文件内容。

## 如何使用

1.  **编写发布说明**:
    在准备发布前，打开项目根目录的 `RELEASE_NOTES.txt`，填写本次更新日志。
    ```text
    - 修复了登录 Bug
    - 新增了某种卡片类型
    ```

2.  **提交并打 Tag**:
    ```bash
    git add .
    git commit -m "准备发布 v1.2.0"
    git tag v1.2.0
    git push origin v1.2.0
    ```
    *(注意: 推送 Tag 才会触发构建)*

3.  **查看 GitHub**:
    前往仓库的 "Actions" 标签页查看构建进度。完成后，在 "Releases" 页面即可看到包含 APK 和说明的新发布版本。

## 验证
- 已检查 `android_release.yml` 的 YAML 语法和条件逻辑。
- 已确认 `RELEASE_NOTES.txt` 存在并被工作流正确引用。

> [!TIP]
> 如果只想构建 APK 而不发布 Release，可以在 GitHub Actions 页面手动运行 "Android Release" 工作流。
