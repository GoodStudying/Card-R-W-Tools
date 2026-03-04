package cn.ratnoumi.bcardtools.dao

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import cn.ratnoumi.bcardtools.dao.BambuFilamentDao
import kotlin.concurrent.Volatile

/**
 * @author miceyao
 * @version 1.0
 * @date 2020/5/23 10:51
 */
class SQLiteHelper(context: Context?) : SQLiteOpenHelper(context, "SeatBeltLock", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        Log.d("SQLiteHelper", "onCreate: ")
        for (sql in createTableSql.values) {
            db.execSQL(sql)
            Log.d("SQLiteHelper", sql!!)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("SQLiteHelper", "onUpgrade: oldVersion=$oldVersion, newVersion=$newVersion")
        if (oldVersion < 2) {
            try {
                // 升级到版本2：增加 colorName 字段
                db.execSQL("ALTER TABLE ${BambuFilamentDao.TABLE_NAME} ADD COLUMN colorName TEXT")
            } catch (e: Exception) {
                e.printStackTrace()
                // 如果升级失败，回退到删除表重建的逻辑（虽然会丢失数据，但能保证应用可用）
                dropAndRecreate(db)
            }
        }
    }

    private fun dropAndRecreate(db: SQLiteDatabase) {
        // drop 掉所有表
        val tableNames = createTableSql.keys.iterator()
        while (tableNames.hasNext()) {
            db.execSQL("drop table if exists " + tableNames.next())
        }
        // 重新建表
        onCreate(db)
    }

    override fun close() {
        if (sqLiteHelper != null) {
            sqLiteHelper!!.close()
            sqLiteHelper = null
        }
    }

    companion object {
        protected var createTableSql: MutableMap<String?, String?> = HashMap<String?, String?>()

        @Volatile
        private var sqLiteHelper: SQLiteHelper? = null

        // 注册 数据表
        init {
            createTableSql[BambuFilamentDao.TABLE_NAME] = BambuFilamentDao.SQL_TABLE_CREATE
        }

        fun getInstance(context: Context?): SQLiteHelper? {
            if (sqLiteHelper == null) {
                synchronized(SQLiteHelper::class.java) {
                    if (sqLiteHelper == null) {
                        sqLiteHelper = SQLiteHelper(context)
                        //                    sqLiteHelper.onUpgrade(sqLiteHelper.getWritableDatabase(), 0, 1);
                    }
                }
            }
            return sqLiteHelper
        }
    }
}