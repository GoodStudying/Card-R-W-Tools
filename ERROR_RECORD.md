# 错误记录

## 错误类型：Android资源链接失败

### 错误信息
```
Execution failed for task ':app:processReleaseResources'.
> A failure occurred while executing com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask$TaskAction
   > Android resource linking failed
     cn.ratnoumi.bcardtools.app-mergeReleaseResources-30:/layout/activity_batch_burn.xml:30: error: attribute android:space not found.
     error: failed linking file resources.
```

### 错误原因
- **文件**：`app/src/main/res/layout/activity_batch_burn.xml`
- **位置**：第30行
- **问题**：使用了不存在的Android属性 `android:space`

### 解决方案
- 删除不存在的 `android:space` 属性
- 如需设置间距，应使用 `android:layout_margin` 或 `android:padding` 属性

### 正确的布局代码示例
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">
    <!-- 子元素 -->
</LinearLayout>
```

### 预防措施
1. **属性验证**：使用Android Studio的XML验证功能检查属性是否存在
2. **文档参考**：遇到不确定的属性时，参考Android官方文档
3. **构建测试**：在提交代码前运行构建命令，确保没有资源链接错误
4. **代码审查**：检查XML布局文件中的属性拼写和有效性

### 相关文件
- `app/src/main/res/layout/activity_batch_burn.xml`

### 修复日期
2026-03-05