package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class UferdigKandidatLytter(
    rapidsConnection: RapidsConnection
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktørId")
                it.demandValue("synlighet.erSynlig", true)
                it.demandValue("synlighet.ferdigBeregnet", true)
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet")
                it.interestedIn("@behov")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if(packet.rejectOnAll("@behov", behovsListe)) return

        val aktørId = packet["aktørId"].asText()
        packet["@behov"] = packet["@behov"].toSet() + behovsListe

        val opprettCv = packet["arbeidsmarkedsCv.opprettCv"]
        val endreCv = packet["arbeidsmarkedsCv.endreCv"]

        val cvMelding =
            if(!opprettCv.isMissingOrNull()) opprettCv
            else if(!endreCv.isMissingOrNull()) endreCv
            else throw RuntimeException("Cv må finnes i UferdigKandidatLytter")

        val arbeidserfaringNode = cvMelding["cv"]["arbeidserfaring"]


        val opprettJobbrofil = packet["arbeidsmarkedsCv.opprettJobbprofil"]
        val endreJobbprofil = packet["arbeidsmarkedsCv.endreJobbprofil"]

        val jobbMelding =
            if(!opprettJobbrofil.isMissingOrNull()) opprettJobbrofil
            else if(!endreJobbprofil.isMissingOrNull()) endreJobbprofil
            else throw RuntimeException("Jobbprofil må finnes i UferdigKandidatLytter")

        val kompteanseNode = jobbMelding["jobbprofil"]["kompetanser"]
        val jobbønsker = jobbMelding["jobbprofil"]["stillinger"]


        val arbeidserfaringListe = arbeidserfaringNode.toList().map {
            it["stillingstittel"].asText()
        }

        val kompetanseListe = kompteanseNode.map {
            it.asText()
        }

        val jobbønskeListe = jobbønsker.map {
            it.asText()
        }

        val stillingsTitler = arbeidserfaringListe.union(jobbønskeListe)

        packet["kompetanse"] = kompetanseListe
        packet["stillingstittel"] = stillingsTitler

        log.info("Sender behov for $aktørId")
        context.publish(aktørId, packet.toJson())
    }



    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }

}

private fun JsonMessage.rejectOnAll(key: String, values: List<String>) = get(key).let { node ->
    !node.isMissingNode && node.isArray && node.map(JsonNode::asText).containsAll(values)
}
