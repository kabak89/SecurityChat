package com.security.chat.multiplatform.common.encryption

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
public object RsaSqueezer {
    private const val PREFIX = "rsasq|"
    private const val TYPE_PKCS1: Byte = 1
    private const val TYPE_PKCS8: Byte = 8

    private data class Field(val value: BigInteger, val derLength: Int)

    public fun squeeze(rawDerBase64: String): String {
        try {
            val derBytes = Base64.decode(rawDerBase64)
            val type = if (isPkcs8(derBytes)) TYPE_PKCS8 else TYPE_PKCS1
            val fields = parseFields(derBytes) ?: return rawDerBase64

            val p = fields["p"]!!.value
            val q = fields["q"]!!.value
            val e = fields["e"]!!.value
            val p1 = p - BigInteger.ONE
            val q1 = q - BigInteger.ONE

            val dOriginal = fields["d"]!!.value
            val d1Original = fields["d1"]!!.value
            val d2Original = fields["d2"]!!.value
            val coeffOriginal = fields["coeff"]!!.value

            val dEuler = try {
                e.modInverse(p1 * q1)
            } catch (_: Exception) {
                null
            }
            val dCarmichael = try {
                e.modInverse((p1 * q1) / p1.gcd(q1))
            } catch (_: Exception) {
                null
            }

            val dFlag: Int = when (dOriginal) {
                dEuler -> 0
                dCarmichael -> 1
                else -> 2
            }

            val d1Calc = dOriginal.remainder(p1)
            val d2Calc = dOriginal.remainder(q1)
            val coeffCalc = q.modInverse(p)

            val hasD1 = d1Original != d1Calc
            val hasD2 = d2Original != d2Calc
            val hasCoeff = coeffOriginal != coeffCalc

            val flags: Int = dFlag or
                    (if (hasD1) 4 else 0) or
                    (if (hasD2) 8 else 0) or
                    (if (hasCoeff) 16 else 0)

            val pBytes = p.toByteArray()
            val qBytes = q.toByteArray()

            val extraData = mutableListOf<ByteArray>()
            if (dFlag == 2) extraData.add(dOriginal.toByteArray())
            if (hasD1) extraData.add(d1Original.toByteArray())
            if (hasD2) extraData.add(d2Original.toByteArray())
            if (hasCoeff) extraData.add(coeffOriginal.toByteArray())

            val fieldNames = listOf("n", "e", "d", "p", "q", "d1", "d2", "coeff")
            val lengths = fieldNames.map { fields[it]!!.derLength.toShort() }

            val bufferSize =
                6 + pBytes.size + qBytes.size + 16 + extraData.sumOf { it.size } + (extraData.size * 2)
            val buffer = ByteArray(bufferSize)
            var o = 0
            buffer[o++] = type
            buffer[o++] = flags.toByte()
            buffer[o++] = (pBytes.size shr 8).toByte()
            buffer[o++] = (pBytes.size and 0xFF).toByte()
            buffer[o++] = (qBytes.size shr 8).toByte()
            buffer[o++] = (qBytes.size and 0xFF).toByte()

            pBytes.copyInto(buffer, o); o += pBytes.size
            qBytes.copyInto(buffer, o); o += qBytes.size

            for (len in lengths) {
                buffer[o++] = (len.toInt() shr 8).toByte()
                buffer[o++] = (len.toInt() and 0xFF).toByte()
            }

            for (data in extraData) {
                buffer[o++] = (data.size shr 8).toByte()
                buffer[o++] = (data.size and 0xFF).toByte()
                data.copyInto(buffer, o); o += data.size
            }

            return PREFIX + Base64.encode(buffer)
        } catch (e: Exception) {
            return rawDerBase64
        }
    }

    public fun expand(squeezed: String): String {
        if (!squeezed.startsWith(PREFIX)) return squeezed
        try {
            val buffer = Base64.decode(squeezed.removePrefix(PREFIX))
            var o = 0
            val type = buffer[o++]
            val flags = buffer[o++].toInt() and 0xFF
            val pLen = ((buffer[o++].toInt() and 0xFF) shl 8) or (buffer[o++].toInt() and 0xFF)
            val qLen = ((buffer[o++].toInt() and 0xFF) shl 8) or (buffer[o++].toInt() and 0xFF)
            val pBytes = buffer.copyOfRange(o, o + pLen); o += pLen
            val qBytes = buffer.copyOfRange(o, o + qLen); o += qLen

            val lengths = mutableListOf<Int>()
            for (i in 0 until 8) {
                lengths.add(((buffer[o++].toInt() and 0xFF) shl 8) or (buffer[o++].toInt() and 0xFF))
            }

            fun readExtra(): BigInteger {
                val len = ((buffer[o++].toInt() and 0xFF) shl 8) or (buffer[o++].toInt() and 0xFF)
                val data = buffer.copyOfRange(o, o + len); o += len
                return BigInteger.fromByteArray(data, Sign.POSITIVE)
            }

            val dFlag = flags and 3
            val hasD1 = (flags and 4) != 0
            val hasD2 = (flags and 8) != 0
            val hasCoeff = (flags and 16) != 0

            val dFull = if (dFlag == 2) readExtra() else null
            val d1Full = if (hasD1) readExtra() else null
            val d2Full = if (hasD2) readExtra() else null
            val coeffFull = if (hasCoeff) readExtra() else null

            val p = BigInteger.fromByteArray(pBytes, Sign.POSITIVE)
            val q = BigInteger.fromByteArray(qBytes, Sign.POSITIVE)
            val e = BigInteger.fromInt(65537)
            val p1 = p - BigInteger.ONE
            val q1 = q - BigInteger.ONE

            val d = dFull ?: if (dFlag == 0) {
                e.modInverse(p1 * q1)
            } else {
                e.modInverse((p1 * q1) / p1.gcd(q1))
            }

            val n = p * q
            val d1 = d1Full ?: d.remainder(p1)
            val d2 = d2Full ?: d.remainder(q1)
            val coeff = coeffFull ?: q.modInverse(p)

            val values = mapOf(
                "n" to n,
                "e" to e,
                "d" to d,
                "p" to p,
                "q" to q,
                "d1" to d1,
                "d2" to d2,
                "coeff" to coeff,
            )
            val pkcs1 = reconstructPkcs1WithLengths(values, lengths)
            val result = if (type == TYPE_PKCS8) wrapInPkcs8(pkcs1) else pkcs1
            return Base64.encode(result)
        } catch (e: Exception) {
            return squeezed
        }
    }

    private fun isPkcs8(derBytes: ByteArray): Boolean {
        return try {
            var offset = 0
            if (derBytes[offset++] != 0x30.toByte()) return false
            readLength(derBytes, offset).let { offset = it.second }
            // PKCS#8 version is always 0
            derBytes[offset] == 0x02.toByte() && derBytes[offset + 1] == 0x01.toByte() && derBytes[offset + 2] == 0x00.toByte()
        } catch (_: Exception) {
            false
        }
    }

    private fun parseFields(derBytes: ByteArray): Map<String, Field>? {
        return try {
            var offset = 0
            if (derBytes[offset++] != 0x30.toByte()) return null
            readLength(derBytes, offset).let { offset = it.second }

            if (derBytes[offset] == 0x02.toByte()) {
                val vLen = readLength(derBytes, offset + 1).let { it.first + it.second - offset }
                var io = offset + vLen
                if (derBytes[io] == 0x30.toByte()) {
                    val aLen = readLength(derBytes, io + 1).let { it.first + it.second - io }
                    io += aLen
                    if (derBytes[io] == 0x04.toByte()) {
                        val p1Len = readLength(derBytes, io + 1).let { it.first to it.second }
                        return parseFields(
                            derBytes.copyOfRange(
                                p1Len.second,
                                p1Len.second + p1Len.first,
                            ),
                        )
                    }
                }
            }

            offset = 0
            if (derBytes[offset++] != 0x30.toByte()) return null
            readLength(derBytes, offset).let { offset = it.second }
            if (derBytes[offset++] != 0x02.toByte()) return null
            val vLen = readLength(derBytes, offset).also { offset = it.second }.first
            offset += vLen

            val fields = mutableMapOf<String, Field>()
            val names = listOf("n", "e", "d", "p", "q", "d1", "d2", "coeff")
            for (name in names) {
                if (derBytes[offset++] != 0x02.toByte()) return null
                val len = readLength(derBytes, offset).also { offset = it.second }.first
                val value = BigInteger.fromByteArray(
                    derBytes.copyOfRange(offset, offset + len),
                    Sign.POSITIVE,
                )
                fields[name] = Field(value, len)
                offset += len
            }
            fields
        } catch (_: Exception) {
            null
        }
    }

    private fun reconstructPkcs1WithLengths(
        values: Map<String, BigInteger>,
        lengths: List<Int>,
    ): ByteArray {
        val fieldNames = listOf("n", "e", "d", "p", "q", "d1", "d2", "coeff")
        var content = byteArrayOf(0x02, 0x01, 0x00)
        for (i in fieldNames.indices) {
            val v = values[fieldNames[i]]!!
            val expectedLen = lengths[i]
            content += toDerIntFixed(v, expectedLen)
        }
        return wrapSequence(content)
    }

    private fun toDerIntFixed(value: BigInteger, expectedLen: Int): ByteArray {
        var bytes = value.toByteArray()
        if (bytes.size < expectedLen) {
            val padded = ByteArray(expectedLen)
            bytes.copyInto(padded, expectedLen - bytes.size)
            bytes = padded
        } else if (bytes.size > expectedLen) {
            bytes = bytes.copyOfRange(bytes.size - expectedLen, bytes.size)
        }
        return byteArrayOf(0x02) + encodeLength(bytes.size) + bytes
    }

    private fun wrapInPkcs8(pkcs1: ByteArray): ByteArray {
        val version = byteArrayOf(0x02, 0x01, 0x00)
        val rsaOid = byteArrayOf(
            0x30,
            0x0D,
            0x06,
            0x09,
            0x2A,
            0x86.toByte(),
            0x48,
            0x86.toByte(),
            0xF7.toByte(),
            0x0D,
            0x01,
            0x01,
            0x01,
            0x05,
            0x00,
        )
        val privateKey = byteArrayOf(0x04) + encodeLength(pkcs1.size) + pkcs1
        return wrapSequence(version + rsaOid + privateKey)
    }

    private fun wrapSequence(content: ByteArray): ByteArray {
        return byteArrayOf(0x30) + encodeLength(content.size) + content
    }

    private fun readLength(bytes: ByteArray, offset: Int): Pair<Int, Int> {
        var o = offset
        val first = bytes[o++].toInt() and 0xFF
        if (first < 128) return first to o
        val lenBytes = first and 0x7F
        var length = 0
        for (i in 0 until lenBytes) {
            length = (length shl 8) or (bytes[o++].toInt() and 0xFF)
        }
        return length to o
    }

    private fun encodeLength(length: Int): ByteArray {
        if (length < 128) return byteArrayOf(length.toByte())
        val bytes = mutableListOf<Byte>()
        var temp = length
        while (temp > 0) {
            bytes.add(0, (temp and 0xFF).toByte())
            temp = temp shr 8
        }
        return byteArrayOf((0x80 or bytes.size).toByte()) + bytes.toByteArray()
    }
}
