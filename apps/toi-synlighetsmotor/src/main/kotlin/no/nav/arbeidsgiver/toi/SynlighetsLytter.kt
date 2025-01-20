package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.Evaluering.Companion.invoke

class SynlighetsLytter(private val rapidsConnection: RapidsConnection, private val repository: Repository) :
    River.PacketListener {
    private val interessanteFelt = listOf(
        "arbeidsmarkedCv",
        "oppfølgingsinformasjon",
        "oppfølgingsperiode",
        "fritattKandidatsøk",
        "hjemmel",
        "måBehandleTidligereCv"
    )

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("system_participating_services")
                it.forbid("synlighet")
                it.interestedIn(*interessanteFelt.toTypedArray())
                it.interestedIn("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val harIngenInteressanteFelter = interessanteFelt.map(packet::get).all(JsonNode::isMissingNode)
        val erSammenstillt = packet["system_participating_services"]
            .map { it.get("service")?.asText() }
            .contains("toi-sammenstille-kandidat")

        if (harIngenInteressanteFelter || !erSammenstillt) return

        val kandidat = Kandidat.fraJson(packet)

        val synlighetsevaluering = lagEvalueringsGrunnlag(kandidat)
        securelogAdressebeskyttelse(kandidat, synlighetsevaluering)
        repository.lagre(
            evaluering = synlighetsevaluering,
            aktørId = kandidat.aktørId,
            fødselsnummer = kandidat.fødselsNummer()
        )

        val synlighet = synlighetsevaluering()
        packet["synlighet"] = synlighet
        rapidsConnection.publish(kandidat.aktørId, packet.toJson())
    }

    fun securelogAdressebeskyttelse(kandidat: Kandidat, synlighetsevaluering: Evaluering) {
        val synligUtenomGradering = synlighetsevaluering.run {
            harAktivCv &&
                    harJobbprofil &&
                    harSettHjemmel &&
                    maaIkkeBehandleTidligereCv &&
                    arenaIkkeFritattKandidatsøk &&
                    erUnderOppfoelging &&
                    harRiktigFormidlingsgruppe &&
                    erIkkeSperretAnsatt &&
                    erIkkeDoed &&
                    erFerdigBeregnet &&
                    erIkkeKvp
        }

        val ukjentAdressebeskyttelse = kandidat.adressebeskyttelse?.let { beskyttelse ->
            beskyttelse !in listOf("STRENGT_FORTROLIG_UTLAND", "STRENGT_FORTROLIG", "FORTROLIG", "UGRADERT")
        } ?: false

        val kandidatMenGraderingNull = synligUtenomGradering && kandidat.adressebeskyttelse == null

        val stoppetAvFortrolig = synligUtenomGradering && kandidat.adressebeskyttelse != "FORTROLIG"
        val stoppetAvStrengtFortrolig = synligUtenomGradering && kandidat.adressebeskyttelse != "STRENGT_FORTROLIG"
        val stoppetAvStrengtFortroligUtland = synligUtenomGradering && kandidat.adressebeskyttelse != "STRENGT_FORTROLIG_UTLAND"

        val uenigOmKode7 = synligUtenomGradering && (
                (kandidat.adressebeskyttelse != "FORTROLIG" && kandidat.oppfølgingsinformasjon?.diskresjonskode == "7") ||
                        (kandidat.adressebeskyttelse == "FORTROLIG" && kandidat.oppfølgingsinformasjon?.diskresjonskode != "7")
                )

        val uenigOmKode6 = synligUtenomGradering && (
                (kandidat.adressebeskyttelse != "STRENGT_FORTROLIG" && kandidat.oppfølgingsinformasjon?.diskresjonskode == "6") ||
                        (kandidat.adressebeskyttelse == "STRENGT_FORTROLIG" && kandidat.oppfølgingsinformasjon?.diskresjonskode != "6")
                )

        val adressebeskyttelseMenStoppetAvAndre = !synligUtenomGradering && kandidat.adressebeskyttelse != null && kandidat.adressebeskyttelse != "UGRADERT"

        val betingelser = listOf(
            ukjentAdressebeskyttelse,
            kandidatMenGraderingNull,
            stoppetAvFortrolig,
            stoppetAvStrengtFortrolig,
            stoppetAvStrengtFortroligUtland,
            uenigOmKode7,
            uenigOmKode6,
            adressebeskyttelseMenStoppetAvAndre
        )

        if (betingelser.any { it }) {
            secureLog.info("Adressebeskyttelse vurdering for aktørId=${kandidat.aktørId}: " +
                    "ukjentAdressebeskyttelse=$ukjentAdressebeskyttelse, " +
                    "kandidatMenGraderingNull=$kandidatMenGraderingNull, " +
                    "stoppetAvFortrolig=$stoppetAvFortrolig, " +
                    "stoppetAvStrengtFortrolig=$stoppetAvStrengtFortrolig, " +
                    "stoppetAvStrengtFortroligUtland=$stoppetAvStrengtFortroligUtland, " +
                    "uenigOmKode7KanSkyldesForsinkelse=$uenigOmKode7, " +
                    "uenigOmKode6KanSkyldesForsinkelse=$uenigOmKode6, " +
                    "adressebeskyttelseMenIkkeKandidat=$adressebeskyttelseMenStoppetAvAndre"
            )
        }
    }
}
