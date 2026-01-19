package cn.ratnoumi.bcardtools.drive.mifare

import android.nfc.tech.MifareClassic

val defaultKeys = mutableListOf(
    byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
    byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
    byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()),
    byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()),
)

fun findMifareSectorKeyA(classic: MifareClassic, sectorIndex: Int, keys: List<ByteArray> = listOf()): ByteArray? {
    for (keyA in keys) {
        if (classic.authenticateSectorWithKeyA(sectorIndex, keyA)) {
            return keyA
        }
    }
    return null
}

fun findMifareSectorKeyB(classic: MifareClassic, sectorIndex: Int, keys: List<ByteArray>): ByteArray? {
    for (keyB in keys) {
        if (classic.authenticateSectorWithKeyB(sectorIndex, keyB)) {
            return keyB
        }
    }
    return null
}

fun readMifareSector(card: MifareCard, classic: MifareClassic, sectorIndex: Int, keyA: ByteArray?, keyB: ByteArray?) {
    // 认证 A/B Key
    keyA?.let { classic.authenticateSectorWithKeyA(sectorIndex, it) }
    keyB?.let { classic.authenticateSectorWithKeyB(sectorIndex, it) }
    // 读取 扇区
    val start = classic.sectorToBlock(sectorIndex)
    val end = start + classic.getBlockCountInSector(sectorIndex)
    for (i in start..<end) {
        card.blocks.add(i, classic.readBlock(i))
    }
    // 写入Key值
    keyA?.copyInto(card.blocks[end - 1], 0)
    keyB?.copyInto(card.blocks[end - 1], 10)
}

fun writeMifareSector(card: MifareCard, classic: MifareClassic, sectorIndex: Int, keyA: ByteArray?, keyB: ByteArray?) {
    // 认证 A/B Key
    keyA?.let { classic.authenticateSectorWithKeyA(sectorIndex, it) }
    keyB?.let { classic.authenticateSectorWithKeyB(sectorIndex, it) }
    // 读取 扇区
    val start = classic.sectorToBlock(sectorIndex)
    val end = start + classic.getBlockCountInSector(sectorIndex)
    for (i in start..<end) {
        classic.writeBlock(i, card.blocks[i])
    }
}