为了实现在列表和详情页中增加“色号名称”并支持搜索的功能，我们需要对数据模型、数据库持久化、数据解析逻辑以及 UI 界面进行一系列调整。

以下是具体的修改建议：

### 1. 修改数据模型 (`BambuFilamentCard.kt`)

在 `BambuFilamentCard` 类中新增 `colorName` 字段。

```kotlin
// app/src/main/java/cn/ratnoumi/bcardtools/drive/bambu/BambuFilamentCard.kt

data class BambuFilamentCard(
    val uid: String,
    // ... 其他现有字段
    val detailedFilamentType: String,
    val color: Int,
    val colorName: String, // 新增：色号名称
    // ... 其他字段
    val card: MifareCard
) : Parcelable {
    constructor(parcel: Parcel) : this(
        uid = parcel.readString() ?: "",
        // ... 按顺序读取
        detailedFilamentType = parcel.readString() ?: "",
        color = parcel.readInt(),
        colorName = parcel.readString() ?: "", // 读取新增字段
        // ...
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(uid)
        // ... 按顺序写入
        dest.writeString(detailedFilamentType)
        dest.writeInt(color)
        dest.writeString(colorName) // 写入新增字段
        // ...
    }
}

```

### 2. 修改数据库 DAO 层 (`BambuFilamentDao.kt`)

更新 SQL 建表语句和数据转换逻辑，以存储和读取 `colorName`。

```kotlin
// app/src/main/java/cn/ratnoumi/bcardtools/dao/BambuFilamentDao.kt

class BambuFilamentDao(context: Context) : BaseDao<BambuFilamentCard, String>(context) {
    companion object {
        const val TABLE_NAME: String = "bambu_filament"
        const val SQL_TABLE_CREATE: String =
            "CREATE TABLE $TABLE_NAME (\n" +
                    "\tid TEXT primary key,\n" +
                    // ...
                    "\tcolor INTEGER,\n" +
                    "\tcolorName TEXT,\n" + // 新增数据库字段
                    // ...
                    "\tcard TEXT\n" +
                    ");"
    }

    override fun toBean(cursor: Cursor?): BambuFilamentCard? {
        cursor?.let {
            return BambuFilamentCard(
                // ...
                color = getColumnInt(it, "color"),
                colorName = getColumnString(it, "colorName") ?: "", // 从数据库读取
                // ...
            )
        }
        return null
    }

    override fun toContentValues(bean: BambuFilamentCard?): ContentValues {
        val cv = ContentValues()
        // ...
        addContentValues(cv, "color", bean?.color)
        addContentValues(cv, "colorName", bean?.colorName) // 保存到数据库
        // ...
        return cv
    }
}

```

*注意：如果需要保留现有数据，需在 `SQLiteHelper.kt` 中处理版本升级增加列，或者简单起见直接重装应用触发重建表。*

### 3. 实现色号匹配逻辑 (`BambuUtils.kt`)

在解析卡片数据时，根据 `detailedFilamentType` 和 `color` 从 `耗材色号名称.md` 的数据中匹配名称。

```kotlin
// app/src/main/java/cn/ratnoumi/bcardtools/drive/bambu/BambuUtils.kt

// 建议创建一个简单的映射表或加载耗材色号名称.md中的数据
fun getColorName(type: String, colorInt: Int): String {
    val hexColor = String.format("#%06X", (0xFFFFFF and colorInt))
    // 这里可以根据 耗材色号名称.md 建立一个 Map 进行匹配
    // 示例：if (type == "PETG HF" && hexColor == "#00AE42") return "绿色"
    return "未知颜色" // 默认返回
}

fun getBambuFilament(card: MifareCard): BambuFilamentCard {
    val blocks = card.blocks
    val type = bytesToStr(blocks[4])
    val color = Color.argb(...) // 原有颜色逻辑
    
    return BambuFilamentCard(
        // ...
        detailedFilamentType = type,
        color = color,
        colorName = getColorName(type, color), // 自动匹配色号名称
        // ...
    )
}

```

### 4. 更新 UI 界面展示

#### 列表页 (`item_card.xml` & `CardItemAdapter.kt`)

在 `item_card.xml` 中增加一个显眼的 TextView 用于展示 `colorName`。

```xml
<TextView
    android:id="@+id/colorNameText"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textSize="18sp"
    android:textColor="#333333"
    android:textStyle="bold"
    app:layout_constraintTop_toBottomOf="@+id/filamentTypeText"
    app:layout_constraintStart_toStartOf="parent"/>

```

在 `CardItemAdapter.kt` 的 `onBindViewHolder` 中绑定数据：

```kotlin
holder.colorNameText.text = card.colorName // 优先展示色号名称
holder.filamentColorText.text = "#${Integer.toHexString(card.color).uppercase()}" // 保留原始十六进制

```

#### 详情页 (`activity_card_detail.xml` & `CardDetailActivity.kt`)

在详情页顶部或显著位置增加色号名称显示。

```kotlin
// app/src/main/java/cn/ratnoumi/bcardtools/CardDetailActivity.kt
fun updateView() {
    bambuFilamentCard?.let { info ->
        // 增加色号名称显示
        binding.colorNameText.text = info.colorName 
        binding.filamentColorText.text = "#${Integer.toHexString(info.color).uppercase()}"
        // ...
    }
}

```

### 5. 增加搜索支持 (`MainActivity.kt`)

修改筛选逻辑，支持通过 `colorName` 进行搜索。

```kotlin
// app/src/main/java/cn/ratnoumi/bcardtools/MainActivity.kt

private fun filterCards() {
    val result = cards.filter { card ->
        // ... 现有筛选条件
        val searchMatch = searchText.isEmpty() ||
                card.detailedFilamentType.contains(searchText, ignoreCase = true) ||
                card.colorName.contains(searchText, ignoreCase = true) || // 新增：支持通过名称搜索
                card.uid.contains(searchText, ignoreCase = true) ||
                Integer.toHexString(card.color).contains(searchText, ignoreCase = true)
        // ...
    }
    // ... 更新适配器
}

```

通过上述修改，应用将能够解析、存储并在显眼位置展示耗材的具体颜色名称（如“拓竹绿”、“海蓝色”等），同时保留原始十六进制色号功能，并允许用户在主页直接输入颜色名称进行快速查找。