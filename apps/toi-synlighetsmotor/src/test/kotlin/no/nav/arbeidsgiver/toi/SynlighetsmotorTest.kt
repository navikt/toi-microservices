package no.nav.arbeidsgiver.toi

import no.nav.arbeidsgiver.toi.Testdata.Companion.avsluttetOppfølgingsperiode
import no.nav.arbeidsgiver.toi.Testdata.Companion.arbeidsmarkedCv
import no.nav.arbeidsgiver.toi.Testdata.Companion.arenaFritattKandidatsøk
import no.nav.arbeidsgiver.toi.Testdata.Companion.harCvManglerJobbprofil
import no.nav.arbeidsgiver.toi.Testdata.Companion.harEndreJobbrofil
import no.nav.arbeidsgiver.toi.Testdata.Companion.harOpprettJobbrofil
import no.nav.arbeidsgiver.toi.Testdata.Companion.hendelse
import no.nav.arbeidsgiver.toi.Testdata.Companion.hjemmel
import no.nav.arbeidsgiver.toi.Testdata.Companion.komplettHendelseSomFørerTilSynlighetTrue
import no.nav.arbeidsgiver.toi.Testdata.Companion.kvp
import no.nav.arbeidsgiver.toi.Testdata.Companion.manglendeCV
import no.nav.arbeidsgiver.toi.Testdata.Companion.manglendeHjemmel
import no.nav.arbeidsgiver.toi.Testdata.Companion.måBehandleTidligereCv
import no.nav.arbeidsgiver.toi.Testdata.Companion.oppfølgingsinformasjon
import no.nav.arbeidsgiver.toi.Testdata.Companion.oppfølgingsinformasjonHendelseMedParticipatingService
import no.nav.arbeidsgiver.toi.Testdata.Companion.participatingService
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class SynlighetsmotorTest {
    @Test
    fun `Synlighetsevaluering som følge av melding skal lagres på personen i databasen`() {
        val repository = Repository(TestDatabase().dataSource)

        testProgramMedHendelse(
            komplettHendelseSomFørerTilSynlighetTrue(),
            enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(
                synlighet = true,
                ferdigBeregnet = true
            ),
            repository
        )

        val evalueringFraDb = repository.hentMedAktørid(aktorId = "123456789")
        Assertions.assertThat(evalueringFraDb).isEqualTo(
            evalueringMedAltTrue()
        )
    }

    @Test
    fun `legg på synlighet som sann om all data i hendelse tilsier det`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(true, true)
    )

    @Test
    fun `kandidat med kun oppfølgingsinformasjon skal ikke være synlig`() = testProgramMedHendelse(
        hendelse(oppfølgingsinformasjon = oppfølgingsinformasjon()),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(
            synlighet = false, ferdigBeregnet = false
        )
    )

    @Test
    fun `kandidat med kun cv skal ikke være synlig`() = testProgramMedHendelse(
        hendelse(arbeidsmarkedCv = arbeidsmarkedCv()),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, false)
    )

    @Test
    fun `om CV har meldingstype "SLETT" skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(arbeidsmarkedCv = arbeidsmarkedCv(CvMeldingstype.SLETT)),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, true)
    )

    @Test
    fun `om CV har meldingstype "ENDRE" skal synlighet være true`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(arbeidsmarkedCv = arbeidsmarkedCv(meldingstype = CvMeldingstype.ENDRE)),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(true, true)
    )

    @Test
    fun `om Person er død skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(erDoed = true)),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, true)
    )

    @Test
    fun `om Person er sperret ansatt skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(sperretAnsatt = true)),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, true)
    )

    @Test
    fun `om Person ikke har aktiv oppfølgingsperiode skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(
            oppfølgingsperiode = avsluttetOppfølgingsperiode()
        ),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, true)
    )

    @Test
    fun `om Person ikke har oppfølgingsinformasjon skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = null),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, false)
    )

    @Test
    fun `formidlingsgruppe ARBS skal også anses som gyldig formidlingsgruppe`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(formidlingsgruppe = "ARBS")),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(true, true)
    )

    @Test
    fun `om Person har formidlingsgruppe IARBS skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(formidlingsgruppe = "IARBS")),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, true)
    )

    @Test
    fun `om Person har feil formidlingsgruppe skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(formidlingsgruppe = "IKKEARBSELLERIARBS")),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, true)
    )

    @Test
    fun `om Person har kode 6 skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(diskresjonskode = "6")),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, true)
    )

    @Test
    fun `om Person har kode 7 skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(diskresjonskode = "7")),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, true)
    )

    @Test
    fun `om Person er fritatt fra kandidatsøk skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(arenaFritattKandidatsøk = arenaFritattKandidatsøk(
            fritattKandidatsøk=true,
            fnr="12312312312")),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, true)
    )

    @Test
    fun `om Person ikke har CV skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(arbeidsmarkedCv = manglendeCV()),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = false, false)
    )

    @Test
    fun `om Person ikke har jobbprofil skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(arbeidsmarkedCv = harCvManglerJobbprofil()),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = false, true)
    )

    @Test
    fun `om Person har endrejobbprofil skal synlighet kunne være true`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(arbeidsmarkedCv = harEndreJobbrofil()),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = true, true)
    )

    @Test
    fun `om Person har opprettjobbprofil skal synlighet kunne være true`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(arbeidsmarkedCv = harOpprettJobbrofil()),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = true, ferdigBeregnet = true)
    )

    @Test
    fun `om Person ikke har sett hjemmel skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(hjemmel = manglendeHjemmel()),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = false, ferdigBeregnet = true)
    )

    @Test
    fun `person skal være synlig selv om hjemmelen er opprettet frem i tid`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(hjemmel = hjemmel(
            opprettetDato = ZonedDateTime.now().plusHours(2),
            slettetDato = null
        )),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = true, ferdigBeregnet = true)
    )

    @Test
    fun `om Person har hjemmel for feil ressurs skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(hjemmel = hjemmel(ressurs = "CV_GENERELL")),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = false, ferdigBeregnet = true)
    )

    @Test
    fun `om Person har hjemmel som er avsluttet skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(
            hjemmel = hjemmel(
                opprettetDato = ZonedDateTime.now().minusYears(2),
                slettetDato = ZonedDateTime.now().minusYears(1)
            )
        ),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = false, ferdigBeregnet = true)
    )

    @Test
    fun `om Person må behandle tidligere CV skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(
            måBehandleTidligereCv = måBehandleTidligereCv(
                maaBehandleTidligereCv = true
            )
        ),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = false, true)
    )

    @Test
    fun `om Person spesifikt ikke må behandle tidligere CV skal synlighet være true`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(
            måBehandleTidligereCv = måBehandleTidligereCv(
                maaBehandleTidligereCv = false
            )
        ),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = true, true)
    )

    @Test
    fun `om det er ukjent om en Person ikke må behandle tidligere CV skal synlighet være true`() =
        testProgramMedHendelse(
            komplettHendelseSomFørerTilSynlighetTrue(
                måBehandleTidligereCv = null
            ),
            enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = true, true)
        )

    @Test
    fun `Om person har avsluttet kvp skal synlighet være true`() =
        testProgramMedHendelse(
            komplettHendelseSomFørerTilSynlighetTrue(
                kvp = kvp("2023-06-22T12:21:18.895143217+02:00", "2023-06-22T12:21:19.895143217+02:00", "AVSLUTTET")
            ),
            enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = true, ferdigBeregnet = true)
        )

    @Test
    fun `Om person har startet kvp og ingen avsluttet kvp skal synlighet være false`() =
        testProgramMedHendelse(
            komplettHendelseSomFørerTilSynlighetTrue(
                kvp = kvp("2023-06-22T12:21:18.895143217+02:00", null, "STARTET"),
            ),
            enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(synlighet = false, ferdigBeregnet = true)
        )

    @Test
    fun `ignorer uinteressante hendelser`() {
        testProgramMedHendelse(
            hendelse = """{ "@event_name":"uinteressant_hendelse" }""",
            assertion = {
                assertThat(size).isZero()
            }
        )
    }

    @Test
    fun `produserer ny melding dersom sammenstiller er kjørt`() = testProgramMedHendelse(
        oppfølgingsinformasjonHendelseMedParticipatingService(participatingService = participatingService("toi-sammenstille-kandidat")),
        enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(false, false),
    )

    @Test
    fun `Ingen ny melding dersom sammenstiller ikke er kjørt`() = testProgramMedHendelse(
        oppfølgingsinformasjonHendelseMedParticipatingService(
            participatingService = participatingService("toi-arbeidsmarked-cv")
        ),
        enHendelseErIkkePublisert()
    )
}

private fun evalueringMedAltTrue() = Evaluering(
    harAktivCv = true,
    harJobbprofil = true,
    harSettHjemmel = true,
    maaIkkeBehandleTidligereCv = true,
    arenaIkkeFritattKandidatsøk = true,
    erUnderOppfoelging = true,
    harRiktigFormidlingsgruppe = true,
    erIkkeKode6eller7 = true,
    erIkkeSperretAnsatt = true,
    erIkkeDoed = true,
    erIkkeKvp = true,
    erFerdigBeregnet = true
)
