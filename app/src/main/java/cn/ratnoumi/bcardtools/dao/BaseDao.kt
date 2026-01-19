package com.zkhg.seatbeltlock.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import cn.ratnoumi.bcardtools.dao.SQLiteHelper

/**
 * @author miceyao
 * @version 1.0
 * @date 2020/5/23 11:46
 */
abstract class BaseDao<T, K>(private val context: Context?) {
    /**
     * 获取数据表名
     *
     * @return
     */
    abstract val tableName: String?

    //    /**
    //     * 获取建表SQL
    //     *
    //     * @return
    //     */
    //    abstract String getTableCreateSql();
    /**
     * 将Cursor 转换成 Bean
     *
     * @param cursor
     * @return
     */
    protected abstract fun toBean(cursor: Cursor?): T?

    /**
     * 将bean 转换成 ContentValues
     *
     * @param bean
     * @return
     */
    protected abstract fun toContentValues(bean: T?): ContentValues


    val sQLiteHelper: SQLiteHelper
        get() = SQLiteHelper.getInstance(context)!!

    val readableDatabase: SQLiteDatabase
        get() = this.sQLiteHelper.getReadableDatabase()

    val writableDatabase: SQLiteDatabase
        get() = this.sQLiteHelper.getWritableDatabase()

    // ----------------------
    /***
     * 查询全部
     * @param offset
     * @param length
     * @return
     */
    fun findAll(offset: Int = 0, length: Int = Int.MAX_VALUE): MutableList<T?> {
        var sql = "SELECT * FROM " + this.tableName
        if (offset > -1 && length > 0) {
            sql += " limit $length offset $offset"
        }
        val list: MutableList<T?> = ArrayList<T?>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(sql, null)
        while (cursor.moveToNext()) {
            list.add(toBean(cursor))
        }
        cursor.close()
        db.close()
        return list
    }

    /**
     * 根据 id 获取对象
     *
     * @param id
     * @return
     */
    fun findByOne(id: K?): T? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM " + this.tableName + " WHERE id = " + id, null)
        var bean: T? = null
        if (cursor.moveToNext()) {
            bean = toBean(cursor)
        }
        cursor.close()
        db.close()
        return bean
    }

    /**
     * 根据 id 获取对象
     *
     * @param id
     * @return
     */
    fun exist(id: K?): Boolean {
        // 避免id为null的情况
        id ?: return false
        val db = this.readableDatabase
        var exists = false
        // 使用参数化查询避免SQL注入，同时兼容字符串类型id
        val cursor = db.rawQuery(
            "SELECT 1 FROM ${this.tableName} WHERE id = ?",
            arrayOf(id.toString()) // 将id转为字符串，适配参数占位符
        )
        try {
            // 只要有一条记录，就说明存在
            exists = cursor.moveToNext()
        } finally {
            // 确保资源关闭
            cursor.close()
            db.close()
        }
        return exists
    }

    /**
     * 添加一个记录
     *
     * @param bean
     * @return
     */
    fun add(bean: T?): Boolean {
        val db = this.writableDatabase
        val cv = toContentValues(bean)
        val row = db.insert(this.tableName!!, null, cv)
        db.close()
        return row > 0
    }

    fun update(bean: T?): Int {
        val db = this.writableDatabase
        val cv = toContentValues(bean)
        val row = db.update(this.tableName!!, cv, "id=?", arrayOf<String>(cv.getAsInteger("id").toString()))
        db.close()
        return row
    }

    /**
     * 删除一条记录
     *
     * @param id
     * @return
     */
    fun delete(id: K?): Int {
        val db = this.writableDatabase
        val i = db.delete(this.tableName!!, "id=?", arrayOf<String?>(id.toString()))
        db.close()
        return i
    }

    fun deleteAll(): Int {
        val db = this.writableDatabase
        val i = db.delete(this.tableName!!, null, null)
        db.close()
        return i
    }

    protected fun getColumnString(cursor: Cursor, keyName: String?): String? {
        return cursor.getString(cursor.run { getColumnIndex(keyName) })
    }

    protected fun getColumnInt(cursor: Cursor, keyName: String?): Int {
        return cursor.getInt(cursor.run { getColumnIndex(keyName) })
    }

    protected fun getColumnLong(cursor: Cursor, keyName: String?): Long {
        return cursor.getLong(cursor.run { getColumnIndex(keyName) })
    }

    protected fun getColumnBoolean(cursor: Cursor, keyName: String?): Boolean {
        return cursor.getShort(cursor.run { getColumnIndex(keyName) }).toInt() != 0
    }

    protected fun getColumnFloat(cursor: Cursor, keyName: String?): Float {
        return cursor.getFloat(cursor.run { getColumnIndex(keyName) })
    }

    protected fun addContentValues(cv: ContentValues, key: String?, value: Int?) {
        if (value != null) {
            cv.put(key, value)
        }
    }

    protected fun addContentValues(cv: ContentValues, key: String?, value: Boolean?) {
        if (value != null) {
            cv.put(key, value)
        }
    }

    protected fun addContentValues(cv: ContentValues, key: String?, value: String?) {
        if (value != null) {
            cv.put(key, value)
        }
    }

    protected fun addContentValues(cv: ContentValues, key: String?, value: Float?) {
        if (value != null) {
            cv.put(key, value)
        }
    }
}