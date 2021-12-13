package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class SynlighetsLytter(rapidsConnection: RapidsConnection) : River.PacketListener {
    private val interessanteFelt = listOf("oppfølgingsinformasjon", "cv", "oppfølgingsperiode")

    init {
        River(rapidsConnection).apply {
            validate {
                it.interestedIn(*interessanteFelt.toTypedArray())
                it.rejectKey("synlighet")
            }
        }.register(this)
    }

    private val publish: (String) -> Unit = rapidsConnection::publish

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val harIngenInteressanteFelter = interessanteFelt.map(packet::get).all(JsonNode::isMissingNode)
        if (harIngenInteressanteFelter) return

        val kandidat = Kandidat.fraJson(packet)
        packet["synlighet"] = Synlighet(erSynlig(kandidat), harBeregningsgrunnlag(kandidat))

        publish(packet.toJson())
    }

    private class Synlighet(val erSynlig: Boolean, val ferdigBeregnet: Boolean)
}
