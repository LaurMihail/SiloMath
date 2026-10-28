package com.silomath.core.data.network.dto

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

object NullableBigDecimalJsonSerializer :
    KSerializer<BigDecimal?> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            serialName = "NullableBigDecimal",
            kind = PrimitiveKind.STRING
        )

    override fun deserialize(
        decoder: Decoder
    ): BigDecimal? {
        if (decoder is JsonDecoder) {
            val element =
                decoder.decodeJsonElement()

            if (element is JsonNull) {
                return null
            }

            val primitive =
                element as? JsonPrimitive
                    ?: throw IllegalArgumentException(
                        "Expected decimal JSON primitive."
                    )

            return primitive.content.toBigDecimal()
        }

        return decoder.decodeString().toBigDecimal()
    }

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal?
    ) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(
                value
                    ?.let {
                        JsonPrimitive(
                            it.toPlainString()
                        )
                    }
                    ?: JsonNull
            )
            return
        }

        requireNotNull(value) {
            "Non-JSON encoding does not support null decimal."
        }

        encoder.encodeString(
            value.toPlainString()
        )
    }
}
