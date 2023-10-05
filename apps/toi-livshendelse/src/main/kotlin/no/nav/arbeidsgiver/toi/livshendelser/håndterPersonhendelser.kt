package no.nav.arbeidsgiver.toi.livshendelser

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory

class PersonhendelseService(private val rapidsConnection: RapidsConnection, private val pdlKlient: PdlKlient) {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    fun håndter(personHendelser: List<Personhendelse>) {
        val opplysningstyper = personHendelser.map{it.opplysningstype}.distinct()
        secureLog.info("Håndterer ${personHendelser.size} hendelser med typer: ${opplysningstyper}")

        personHendelser
            .filter { it.opplysningstype.contains("ADRESSEBESKYTTELSE_") }
            .map {
                if (it.personidenter.isNullOrEmpty()) {
                    secureLog.error("Ingen personidenter funnet på hendelse")
                    null
                } else {
                    secureLog.info("personidenter funnet på hendelse med opplysningstype ${it.opplysningstype} ")
                    it.personidenter.first()
                }
            }
            .mapNotNull { it }
            .flatMap(::kallPdl)

            //.forEach(::publiserHendelse)
            .forEach(DiskresjonsHendelse::toSecurelog)
    }

    fun kallPdl(ident: String): List<DiskresjonsHendelse> {
        val resultat = pdlKlient.hentGraderingPerAktørId(ident)
            .map { (aktørId, gradering) ->
                DiskresjonsHendelse(ident = aktørId, gradering = gradering)
            }

        secureLog.info("Resulat fra pdl: " + resultat)
        return resultat
    }

    fun publiserHendelse(diskresjonsHendelse: DiskresjonsHendelse) {
        rapidsConnection.publish(diskresjonsHendelse.ident(), diskresjonsHendelse.toJson())
    }
}