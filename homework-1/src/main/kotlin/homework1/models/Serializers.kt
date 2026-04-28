package homework1.models

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

typealias Money = @kotlinx.serialization.Serializable(with = BigDecimalAsStringSerializer::class) BigDecimal

object CurrencyCodeSerializer : KSerializer<CurrencyCode> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("CurrencyCode", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CurrencyCode) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): CurrencyCode {
        val raw = decoder.decodeString().uppercase()
        return runCatching { CurrencyCode.valueOf(raw) }.getOrElse {
            throw SerializationException("Invalid currency code: $raw")
        }
    }
}

object BigDecimalAsStringSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimalAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        val text = if (decoder is JsonDecoder) {
            val primitive = decoder.decodeJsonElement().jsonPrimitive
            parseJsonPrimitive(primitive)
        } else {
            decoder.decodeString()
        }
        return runCatching { BigDecimal(text) }.getOrElse {
            throw SerializationException("Invalid decimal amount: $text")
        }
    }

    private fun parseJsonPrimitive(primitive: JsonPrimitive): String {
        return primitive.content
    }
}
