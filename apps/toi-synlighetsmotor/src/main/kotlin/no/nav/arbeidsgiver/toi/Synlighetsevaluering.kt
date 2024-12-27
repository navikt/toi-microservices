package no.nav.arbeidsgiver.toi

import org.slf4j.LoggerFactory
import java.time.Instant

val secureLog = LoggerFactory.getLogger("secureLog")

/**
 * Reglene for synligheten spesifiseres i denne klassen. Ved endringer i denne klassen, pass på at dokumentasjonen i microsoft loop er oppdatert med endringene.
 */
fun lagEvalueringsGrunnlag(kandidat: Kandidat): Evaluering =
    Evaluering(
        harAktivCv = kandidat.arbeidsmarkedCv?.meldingstype.let {
            listOf(CvMeldingstype.OPPRETT, CvMeldingstype.ENDRE).contains(it)
        },
        harJobbprofil = kandidat.arbeidsmarkedCv?.endreJobbprofil != null || kandidat.arbeidsmarkedCv?.opprettJobbprofil != null,
        harSettHjemmel = harSettHjemmel(kandidat),
        maaIkkeBehandleTidligereCv = kandidat.måBehandleTidligereCv?.maaBehandleTidligereCv != true,
        arenaIkkeFritattKandidatsøk = kandidat.arenaFritattKandidatsøk == null || !kandidat.arenaFritattKandidatsøk.erFritattKandidatsøk,
        erUnderOppfoelging = erUnderOppfølging(kandidat),
        harRiktigFormidlingsgruppe = kandidat.oppfølgingsinformasjon?.formidlingsgruppe in listOf(
            Formidlingsgruppe.ARBS
        ),
        erIkkeKode6eller7 = erIkkeKode6EllerKode7(kandidat),
        erIkkeSperretAnsatt = kandidat.oppfølgingsinformasjon?.sperretAnsatt == false,
        erIkkeDoed = kandidat.oppfølgingsinformasjon?.erDoed == false,
        erIkkeKvp = !kandidat.erKvp,
        erFerdigBeregnet = beregningsgrunnlag(kandidat)
    )


private fun harSettHjemmel(kandidat: Kandidat): Boolean {
    return if (kandidat.hjemmel != null && kandidat.hjemmel.ressurs == Samtykkeressurs.CV_HJEMMEL) {
        val opprettetDato = kandidat.hjemmel.opprettetDato
        val slettetDato = kandidat.hjemmel.slettetDato

        opprettetDato != null && slettetDato == null
    } else {
        false
    }
}

private fun erUnderOppfølging(kandidat: Kandidat): Boolean {
    if (kandidat.oppfølgingsperiode == null) return false

    val now = Instant.now()
    val startDato = kandidat.oppfølgingsperiode.startDato.toInstant()
    val sluttDato = kandidat.oppfølgingsperiode.sluttDato?.toInstant()
    sanityCheck(now, kandidat, startDato, sluttDato)
    return startDato.isBefore(now) && (sluttDato == null || sluttDato.isAfter(now))
}

private fun sanityCheck(
    now: Instant?,
    kandidat: Kandidat,
    startDatoOppfølging: Instant,
    sluttDatoOppfølging: Instant?
) {
    if (startDatoOppfølging.isAfter(now)) {
        log("erUnderOppfølging").error("startdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: se secure log")
        secureLog.error("startdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: ${kandidat.aktørId}")
    }
    if (sluttDatoOppfølging?.isAfter(now) == true) {
        log("erUnderOppfølging").error("sluttdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: se secure log")
        secureLog.error("sluttdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: ${kandidat.aktørId}")
    }
    if(kandidat.arenaFritattKandidatsøk?.erFritattKandidatsøk == true && !kandidat.erAAP) {
        log("erUnderOppfølging").info("kandidat er fritatt for kandidatsøk, men har ikke aap Aktørid: se securelog")
        secureLog.info("kandidat er fritatt for kandidatsøk, men har ikke aap Aktørid: ${kandidat.aktørId}, fnr: ${kandidat.fødselsNummer()}, hovedmål: ${kandidat.oppfølgingsinformasjon?.hovedmaal} formidlingsgruppe: ${kandidat.oppfølgingsinformasjon?.formidlingsgruppe}, rettighetsgruppe: ${kandidat.oppfølgingsinformasjon?.rettighetsgruppe}")
    }
}

private fun erIkkeKode6EllerKode7(kandidat: Kandidat): Boolean =
    kandidat.oppfølgingsinformasjon != null &&
            (kandidat.oppfølgingsinformasjon.diskresjonskode == null
                    || kandidat.oppfølgingsinformasjon.diskresjonskode !in listOf("6", "7"))

fun beregningsgrunnlag(kandidat: Kandidat) =
    kandidat.arbeidsmarkedCv != null && kandidat.oppfølgingsperiode != null && kandidat.oppfølgingsinformasjon != null
