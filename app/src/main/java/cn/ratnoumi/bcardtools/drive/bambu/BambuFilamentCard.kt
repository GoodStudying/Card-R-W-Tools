package cn.ratnoumi.bcardtools.drive.bambu

import android.os.Parcel
import android.os.Parcelable
import cn.ratnoumi.bcardtools.drive.mifare.MifareCard

/**
 * 某竹耗材信息
 */
data class BambuFilamentCard(
    val uid: String,                    // UID
    val materialVariantID: String,      // 材料变体ID
    val materialID: String,             // 材料ID
    val filamentType: String,           // 耗材类型
    val detailedFilamentType: String,   // 详细耗材类型
    val color: Int,                   // 耗材颜色
    val colorName: String,            // 色号名称
    val spoolWeight: Int,               // 线轴重量
    val filamentDiameter: Float,        // 耗材直径
    val dryingTemperature: Int,         // 干燥温度
    val dryingHours: Int,               // 干燥时间(小时)
    val bedTemperature: Int,            // 建议热床温度
    val maxTemperatureHotend: Int,      // 最大热端温度
    val minTemperatureHotend: Int,      // 最小热端温度
    val xCamInfo: String,               // X凸轮信息
    val minimumNozzleDiameter: Float,   // 最小喷嘴直径
    val trayUID: String,                // 托盘UID
    val spoolWidth: Float,              // 线轴宽度
    val productionDate: String,         // 生产日期
    val filamentLength: Int,            // 耗材长度(米)
    val card: MifareCard
) : Parcelable {
    // 从Parcel反序列化（读取顺序需与writeToParcel完全一致）
    constructor(parcel: Parcel) : this(
        uid = parcel.readString() ?: "",
        materialVariantID = parcel.readString() ?: "",
        materialID = parcel.readString() ?: "",
        filamentType = parcel.readString() ?: "",
        detailedFilamentType = parcel.readString() ?: "",
        color = parcel.readInt(),
        colorName = parcel.readString() ?: "",
        spoolWeight = parcel.readInt(),
        filamentDiameter = parcel.readFloat(),
        dryingTemperature = parcel.readInt(),
        dryingHours = parcel.readInt(),
        bedTemperature = parcel.readInt(),
        maxTemperatureHotend = parcel.readInt(),
        minTemperatureHotend = parcel.readInt(),
        xCamInfo = parcel.readString() ?: "",
        minimumNozzleDiameter = parcel.readFloat(),
        trayUID = parcel.readString() ?: "",
        spoolWidth = parcel.readFloat(),
        productionDate = parcel.readString() ?: "",
        filamentLength = parcel.readInt(),
        // 读取MifareCard（需确保MifareCard已实现Parcelable）
        card = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(MifareCard::class.java.classLoader, MifareCard::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(MifareCard::class.java.classLoader)!!
        }
    )

    // 序列化到Parcel（写入顺序需与构造函数读取顺序完全一致）
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(uid)
        dest.writeString(materialVariantID)
        dest.writeString(materialID)
        dest.writeString(filamentType)
        dest.writeString(detailedFilamentType)
        dest.writeInt(color)
        dest.writeString(colorName)
        dest.writeInt(spoolWeight)
        dest.writeFloat(filamentDiameter)
        dest.writeInt(dryingTemperature)
        dest.writeInt(dryingHours)
        dest.writeInt(bedTemperature)
        dest.writeInt(maxTemperatureHotend)
        dest.writeInt(minTemperatureHotend)
        dest.writeString(xCamInfo)
        dest.writeFloat(minimumNozzleDiameter)
        dest.writeString(trayUID)
        dest.writeFloat(spoolWidth)
        dest.writeString(productionDate)
        dest.writeInt(filamentLength)
        // 写入MifareCard（flags通常传0即可）
        dest.writeParcelable(card, flags)
    }

    // 描述内容（几乎所有情况返回0，仅当包含文件描述符时返回1）
    override fun describeContents(): Int = 0

    // 必须实现CREATOR，类型为Parcelable.Creator<BambuFilament>
    companion object CREATOR : Parcelable.Creator<BambuFilamentCard> {
        // 从Parcel创建对象
        override fun createFromParcel(source: Parcel): BambuFilamentCard {
            return BambuFilamentCard(source)
        }

        // 创建指定大小的数组（用于批量反序列化）
        override fun newArray(size: Int): Array<BambuFilamentCard?> {
            return arrayOfNulls(size)
        }
    }
}