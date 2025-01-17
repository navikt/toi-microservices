package no.nav.arbeidsgiver.toi.livshendelser

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory

class PersonhendelseService(private val rapidsConnection: RapidsConnection, private val pdlKlient: PdlKlient) {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    fun håndter(personHendelser: List<Personhendelse>) {

        log.info("Håndterer ${personHendelser.size} hendelser")

        if(personHendelser.isNotEmpty()) {
            val opplysningstyper = personHendelser.map{it.opplysningstype}.distinct()
            secureLog.info("Håndterer hendelser med typer: $opplysningstyper")
        }

        personHendelser
            .filter { it.opplysningstype.contains("ADRESSEBESKYTTELSE_") }
            .mapNotNull {
                it.personidenter?.firstOrNull()
            }
            .flatMap(::kallPdl)
            .forEach(::publiserHendelse)

        if(personHendelser.isEmpty()) {
            Thread.sleep(1000)
        }
    }

    fun kallPdl(ident: String): List<DiskresjonsHendelse> {
        return pdlKlient.hentGraderingPerAktørId(ident)
            .map { (aktørId, gradering) ->
                DiskresjonsHendelse(ident = aktørId, gradering = gradering)
            }
    }

    fun publiserHendelse(diskresjonsHendelse: DiskresjonsHendelse) {
        rapidsConnection.publish(diskresjonsHendelse.ident(), diskresjonsHendelse.toJson())
    }
}