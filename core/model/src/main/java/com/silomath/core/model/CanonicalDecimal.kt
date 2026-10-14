package com.silomath.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            "BigDecimal",
            PrimitiveKind.STRING
        )

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal
    ) {
        encoder.encodeString(
            value
                .setScale(6, RoundingMode.HALF_UP)
                .toPlainString()
        )
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
            .setScale(6, RoundingMode.HALF_UP)
    }
}

@Serializable
data class CanonicalDecimal(
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal
) {
    init {
        require(value.scale() == 6) {
            "CanonicalDecimal must have scale 6"
        }
    }

    fun toPlainString(): String = value.toPlainString()

    companion object {

        fun from(value: String): CanonicalDecimal {
            val bd = BigDecimal(value.trim())
                .setScale(6, RoundingMode.HALF_UP)

            return CanonicalDecimal(bd)
        }

        fun from(value: BigDecimal): CanonicalDecimal {
            val bd = value
                .setScale(6, RoundingMode.HALF_UP)

            return CanonicalDecimal(bd)
        }

        fun from(value: Long): CanonicalDecimal {
            val bd = BigDecimal.valueOf(value)
                .setScale(6, RoundingMode.HALF_UP)

            return CanonicalDecimal(bd)
        }
    }
}