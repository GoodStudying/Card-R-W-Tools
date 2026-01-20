package cn.ratnoumi.bcardtools.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import cn.ratnoumi.bcardtools.drive.bambu.BambuFilamentCard
import cn.ratnoumi.bcardtools.drive.mifare.MifareCard
import com.google.gson.Gson

class BambuFilamentDao(context: Context) : BaseDao<BambuFilamentCard, String>(context) {
    private val gson = Gson()

    companion object {
        const val TABLE_NAME: String = "bambu_filament"
        const val SQL_TABLE_CREATE: String =
            "CREATE TABLE $TABLE_NAME (\n" +
                    "\tid TEXT primary key,\n" +
                    "\tmaterialVariantID TEXT,\n" +
                    "\tmaterialID TEXT,\n" +
                    "\tfilamentType TEXT,\n" +
                    "\tdetailedFilamentType TEXT,\n" +
                    "\tcolor INTEGER,\n" +
                    "\tspoolWeight INTEGER,\n" +
                    "\tfilamentDiameter REAL,\n" +
                    "\tdryingTemperature INTEGER,\n" +
                    "\tdryingHours INTEGER,\n" +
                    "\tbedTemperature INTEGER,\n" +
                    "\tmaxTemperatureHotend INTEGER,\n" +
                    "\tminTemperatureHotend INTEGER,\n" +
                    "\txCamInfo TEXT,\n" +
                    "\tminimumNozzleDiameter REAL,\n" +
                    "\ttrayUID TEXT,\n" +
                    "\tspoolWidth REAL,\n" +
                    "\tproductionDate TEXT,\n" +
                    "\tfilamentLength INTEGER,\n" +
                    "\tcard TEXT\n" +
                    ");"
    }

    override val tableName: String = TABLE_NAME
    override fun toBean(cursor: Cursor?): BambuFilamentCard? {
        cursor?.let {
            return BambuFilamentCard(
                getColumnString(it, "id") ?: "",
                getColumnString(it, "materialVariantID") ?: "",
                getColumnString(it, "materialID") ?: "",
                getColumnString(it, "filamentType") ?: "",
                getColumnString(it, "detailedFilamentType") ?: "",
                getColumnInt(it, "color"),
                getColumnInt(it, "spoolWeight"),
                getColumnFloat(it, "filamentDiameter"),
                getColumnInt(it, "dryingTemperature"),
                getColumnInt(it, "dryingHours"),
                getColumnInt(it, "bedTemperature"),
                getColumnInt(it, "maxTemperatureHotend"),
                getColumnInt(it, "minTemperatureHotend"),
                getColumnString(it, "xCamInfo") ?: "",
                getColumnFloat(it, "minimumNozzleDiameter"),
                getColumnString(it, "trayUID") ?: "",
                getColumnFloat(it, "spoolWidth"),
                getColumnString(it, "productionDate") ?: "",
                getColumnInt(it, "filamentLength"),
                gson.fromJson(getColumnString(it, "card"), MifareCard::class.java),
            )
        }
        return null
    }

    override fun toContentValues(bean: BambuFilamentCard?): ContentValues {
        val cv = ContentValues()
        addContentValues(cv, "id", bean?.uid)
        addContentValues(cv, "materialVariantID", bean?.materialVariantID)
        addContentValues(cv, "materialID", bean?.materialID)
        addContentValues(cv, "filamentType", bean?.filamentType)
        addContentValues(cv, "detailedFilamentType", bean?.detailedFilamentType)
        addContentValues(cv, "color", bean?.color)
        addContentValues(cv, "spoolWeight", bean?.spoolWeight)
        addContentValues(cv, "filamentDiameter", bean?.filamentDiameter)
        addContentValues(cv, "dryingTemperature", bean?.dryingTemperature)
        addContentValues(cv, "dryingHours", bean?.dryingHours)
        addContentValues(cv, "bedTemperature", bean?.bedTemperature)
        addContentValues(cv, "maxTemperatureHotend", bean?.maxTemperatureHotend)
        addContentValues(cv, "minTemperatureHotend", bean?.minTemperatureHotend)
        addContentValues(cv, "xCamInfo", bean?.xCamInfo)
        addContentValues(cv, "minimumNozzleDiameter", bean?.minimumNozzleDiameter)
        addContentValues(cv, "trayUID", bean?.trayUID)
        addContentValues(cv, "spoolWidth", bean?.spoolWidth)
        addContentValues(cv, "productionDate", bean?.productionDate)
        addContentValues(cv, "filamentLength", bean?.filamentLength)
        addContentValues(cv, "card", gson.toJson(bean?.card))
        return cv
    }
}
