package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.*
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class KandidatfeedLytter(rapidsConnection: RapidsConnection, private val producer: Producer<String, String>) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktørId")
                it.interestedIn("veileder")
                it.demandKey("cv")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (packet["cv"].isNull) {
            val feilmelding = "cv kan ikke være null for aktørid ${packet["aktorId"]}"
            log.error(feilmelding)
            throw IllegalArgumentException(feilmelding)
        }

        val aktørId = packet["aktørId"].asText()
        val packetUtenMetadata = packet.fjernMetadataOgKonverter()
        val melding = ProducerRecord("toi.kandidat-2", aktørId, packetUtenMetadata.toString())

        producer.send(melding) { _, exception ->
            if (exception == null) {
                log.info("Sendte kandidat med aktørId $aktørId")
            } else {
                log.error("Klarte ikke å sende kandidat med aktørId $aktørId", exception)
            }
        }
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson())
        val alleFelter = jsonNode.fieldNames().asSequence().toList()
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        val aktuelleFelter = alleFelter.filter { !metadataFelter.contains(it) }

        val rotNode = jacksonObjectMapper().createObjectNode()

        aktuelleFelter.forEach {
            rotNode.set<JsonNode>(it, jacksonObjectMapper().valueToTree(this[it]))
        }
        return rotNode
    }
}


