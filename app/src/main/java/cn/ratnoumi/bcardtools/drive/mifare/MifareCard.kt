package cn.ratnoumi.bcardtools.drive.mifare

import android.nfc.tech.MifareClassic
import android.os.Parcel
import android.os.Parcelable

data class MifareCard(
    var mSize: Int,
    var blocks: MutableList<ByteArray> = mutableListOf(),
) : Parcelable {
    constructor(parcel: Parcel) : this(
        mSize = parcel.readInt(),
        blocks = mutableListOf<ByteArray>().apply {
            parcel.readList(this, ByteArray::class.java.classLoader)
        }
    )

    /**
     * Return the number of MIFARE Classic sectors.
     */
    fun getSectorCount(): Int {
        return when (mSize) {
            MifareClassic.SIZE_1K -> 16
            MifareClassic.SIZE_2K -> 32
            MifareClassic.SIZE_4K -> 40
            MifareClassic.SIZE_MINI -> 5
            else -> 0
        }
    }

    /**
     * Return the total number of MIFARE Classic blocks.
     */
    fun getBlockCount(): Int {
        return mSize / MifareClassic.BLOCK_SIZE
    }

    /**
     * Return the number of blocks in the given sector.
     */
    fun getBlockCountInSector(sectorIndex: Int): Int {
        return if (sectorIndex < 32) 4 else 16
    }

    /**
     * Return the sector that contains a given block.
     */
    fun blockToSector(blockIndex: Int): Int {
        return if (blockIndex < 32 * 4) blockIndex / 4 else 32 + (blockIndex - 32 * 4) / 16
    }

    /**
     * Return the first block of a given sector.
     */
    fun sectorToBlock(sectorIndex: Int): Int {
        return if (sectorIndex < 32) {
            sectorIndex * 4
        } else {
            32 * 4 + (sectorIndex - 32) * 16
        }
    }

    /**
     * 获取卡片UID（第0块前4字节）
     */
    fun getId(): ByteArray {
        // 避免空列表崩溃，添加判空处理
        return if (blocks.isNotEmpty() && blocks[0].size >= 4) {
            blocks[0].copyOf(4)
        } else {
            ByteArray(0)
        }
    }

    /**
     * 获取指定扇区的KeyA（尾块前6字节）
     */
    fun getKeyA(sectorIndex: Int): ByteArray {
        val tailBlockIndex = getTailBlockIndex(sectorIndex)
        return if (isValidBlockIndex(tailBlockIndex)) {
            blocks[tailBlockIndex].copyOfRange(0, 6)
        } else {
            ByteArray(6)
        }
    }

    /**
     * 获取指定扇区的控制位（尾块6-10字节）
     */
    fun getAuthority(sectorIndex: Int): ByteArray {
        val tailBlockIndex = getTailBlockIndex(sectorIndex)
        return if (isValidBlockIndex(tailBlockIndex)) {
            blocks[tailBlockIndex].copyOfRange(6, 10)
        } else {
            ByteArray(4)
        }
    }

    /**
     * 获取指定扇区的KeyB（尾块10-16字节）
     */
    fun getKeyB(sectorIndex: Int): ByteArray {
        val tailBlockIndex = getTailBlockIndex(sectorIndex)
        return if (isValidBlockIndex(tailBlockIndex)) {
            blocks[tailBlockIndex].copyOfRange(10, 16)
        } else {
            ByteArray(6)
        }
    }

    /**
     * 计算扇区尾块索引（扇区最后一个块）
     */
    private fun getTailBlockIndex(sectorIndex: Int): Int {
        val sectorFirstBlock = sectorToBlock(sectorIndex)
        val blockCount = getBlockCountInSector(sectorIndex)
        return sectorFirstBlock + (blockCount - 1) // 尾块是扇区最后一个块（索引=块数-1）
    }

    /**
     * 校验块索引是否有效
     */
    private fun isValidBlockIndex(blockIndex: Int): Boolean {
        return blockIndex >= 0 && blockIndex < blocks.size && blocks[blockIndex].size == MifareClassic.BLOCK_SIZE
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(mSize)
        dest.writeList(blocks)
    }

    companion object CREATOR : Parcelable.Creator<MifareCard> {
        override fun createFromParcel(source: Parcel?): MifareCard? {
            return source?.let { MifareCard(it) }
        }

        override fun newArray(size: Int): Array<out MifareCard?>? {
            return arrayOfNulls(size)
        }
    }
}