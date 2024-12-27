package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.Javalin
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SammenstillTest {
    private val alleKandidatfelter = listOf(
        "arbeidsmarkedCv",
        "veileder",
        "oppfølgingsinformasjon",
        "siste14avedtak",
        "oppfølgingsperiode",
        "arenaFritattKandidatsøk",
        "hjemmel",
        "måBehandleTidligereCv",
        "kvp",
    )

    private lateinit var javalin: Javalin

    private fun kunKandidatfelter(melding: JsonNode) =
        melding.fieldNames().asSequence().toList().filter { fieldName -> alleKandidatfelter.contains(fieldName) }

    @BeforeEach
    fun before() {
        javalin = Javalin.create().start(9000)
    }

    @AfterEach
    fun after() {
        javalin.stop()
        TestDatabase().slettAlt()
    }

    @Test
    fun `Når veileder og CV har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(veilederMelding(aktørId))
        testRapid.sendTestMessage(cvMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(2)

        val førsteMelding = rapidInspektør.message(0)
        assertThat(førsteMelding.get("@event_name").asText()).isEqualTo("veileder")
        assertThat(førsteMelding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(førsteMelding.has("arbeidsmarkedCv")).isFalse
        assertThat(førsteMelding.get("veileder").get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(førsteMelding.get("veileder").get("veilederId").asText()).isEqualTo("Z994526")
        assertThat(førsteMelding.get("veileder").get("tilordnet").asText()).isEqualTo("2020-12-21T10:58:19.023+01:00")

        val andreMelding = rapidInspektør.message(1)
        assertThat(andreMelding.get("@event_name").asText()).isEqualTo("arbeidsmarked-cv")
        assertThat(andreMelding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(andreMelding.get("veileder").get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(andreMelding.get("veileder").get("veilederId").asText()).isEqualTo("Z994526")
        assertThat(andreMelding.get("veileder").get("tilordnet").asText()).isEqualTo("2020-12-21T10:58:19.023+01:00")
        assertThat(andreMelding.get("arbeidsmarkedCv").get("meldingstype").asText()).isEqualTo("SLETT")
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("oppfolgingsinformasjon").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("opprettCv").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("endreCv").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("slettCv").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("opprettJobbprofil").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("endreJobbprofil").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("slettJobbprofil").isNull)
        assertThat(andreMelding.get("arbeidsmarkedCv").get("aktoerId").asText()).isEqualTo(aktørId)
    }

    @Test
    fun `Når cv og veileder har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(cvMelding(aktørId))
        testRapid.sendTestMessage(veilederMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(2)

        val førsteMelding = rapidInspektør.message(0)
        assertThat(førsteMelding.get("@event_name").asText()).isEqualTo("arbeidsmarked-cv")
        assertThat(førsteMelding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(førsteMelding)).containsExactlyInAnyOrder("arbeidsmarkedCv")

        assertThat(førsteMelding.get("arbeidsmarkedCv").get("meldingstype").asText()).isEqualTo("SLETT")
        assertTrue(førsteMelding.get("arbeidsmarkedCv").get("oppfolgingsinformasjon").isNull)
        assertTrue(førsteMelding.get("arbeidsmarkedCv").get("opprettCv").isNull)
        assertTrue(førsteMelding.get("arbeidsmarkedCv").get("endreCv").isNull)
        assertTrue(førsteMelding.get("arbeidsmarkedCv").get("slettCv").isNull)
        assertTrue(førsteMelding.get("arbeidsmarkedCv").get("opprettJobbprofil").isNull)
        assertTrue(førsteMelding.get("arbeidsmarkedCv").get("endreJobbprofil").isNull)
        assertTrue(førsteMelding.get("arbeidsmarkedCv").get("slettJobbprofil").isNull)
        assertThat(førsteMelding.get("arbeidsmarkedCv").get("aktoerId").asText()).isEqualTo(aktørId)

        val andreMelding = rapidInspektør.message(1)
        assertThat(andreMelding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(andreMelding.get("@event_name").asText()).isEqualTo("veileder")
        assertThat(andreMelding.get("veileder").get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(andreMelding.get("veileder").get("veilederId").asText()).isEqualTo("Z994526")
        assertThat(andreMelding.get("veileder").get("tilordnet").asText()).isEqualTo("2020-12-21T10:58:19.023+01:00")
        assertThat(andreMelding.get("arbeidsmarkedCv").get("meldingstype").asText()).isEqualTo("SLETT")
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("oppfolgingsinformasjon").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("opprettCv").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("endreCv").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("slettCv").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("opprettJobbprofil").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("endreJobbprofil").isNull)
        assertTrue(andreMelding.get("arbeidsmarkedCv").get("slettJobbprofil").isNull)
        assertThat(andreMelding.get("arbeidsmarkedCv").get("aktoerId").asText()).isEqualTo(aktørId)
    }

    @Test
    fun `Når bare CV har blitt mottatt for kandidat skal ny melding publiseres på rapid uten veileder`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(cvMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("arbeidsmarked-cv")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("arbeidsmarkedCv")

        assertThat(melding.get("arbeidsmarkedCv").get("meldingstype").asText()).isEqualTo("SLETT")
        assertTrue(melding.get("arbeidsmarkedCv").get("oppfolgingsinformasjon").isNull)
        assertTrue(melding.get("arbeidsmarkedCv").get("opprettCv").isNull)
        assertTrue(melding.get("arbeidsmarkedCv").get("endreCv").isNull)
        assertTrue(melding.get("arbeidsmarkedCv").get("slettCv").isNull)
        assertTrue(melding.get("arbeidsmarkedCv").get("opprettJobbprofil").isNull)
        assertTrue(melding.get("arbeidsmarkedCv").get("endreJobbprofil").isNull)
        assertTrue(melding.get("arbeidsmarkedCv").get("slettJobbprofil").isNull)
        assertThat(melding.get("arbeidsmarkedCv").get("aktoerId").asText()).isEqualTo(aktørId)
    }

    @Test
    fun `Når oppfølgingsinformasjon har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(oppfølgingsinformasjonMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("oppfølgingsinformasjon")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("oppfølgingsinformasjon")

        val oppfølgingsinformasjonPåMelding = melding.get("oppfølgingsinformasjon")
        assertThat(oppfølgingsinformasjonPåMelding.get("formidlingsgruppe").asText()).isEqualTo("ARBS")
        assertThat(oppfølgingsinformasjonPåMelding.get("iservFraDato").isNull).isTrue
        assertThat(oppfølgingsinformasjonPåMelding.get("fornavn").asText()).isEqualTo("TULLETE")
        assertThat(oppfølgingsinformasjonPåMelding.get("etternavn").asText()).isEqualTo("TABBE")
        assertThat(oppfølgingsinformasjonPåMelding.get("oppfolgingsenhet").asText()).isEqualTo("0318")
        assertThat(oppfølgingsinformasjonPåMelding.get("kvalifiseringsgruppe").asText()).isEqualTo("BATT")
        assertThat(oppfølgingsinformasjonPåMelding.get("rettighetsgruppe").asText()).isEqualTo("AAP")
        assertThat(oppfølgingsinformasjonPåMelding.get("hovedmaal").asText()).isEqualTo("BEHOLDEA")
        assertThat(oppfølgingsinformasjonPåMelding.get("sikkerhetstiltakType").isNull).isTrue
        assertThat(oppfølgingsinformasjonPåMelding.get("diskresjonskode").isNull).isTrue
        assertThat(oppfølgingsinformasjonPåMelding.get("harOppfolgingssak").asBoolean()).isTrue
        assertThat(oppfølgingsinformasjonPåMelding.get("sperretAnsatt").asBoolean()).isFalse
        assertThat(oppfølgingsinformasjonPåMelding.get("erDoed").asBoolean()).isFalse
        assertThat(oppfølgingsinformasjonPåMelding.get("doedFraDato").isNull).isTrue
        assertThat(
            oppfølgingsinformasjonPåMelding.get("sistEndretDato").asText()
        ).isEqualTo("2020-10-30T14:15:38+01:00")
    }

    @Test
    fun `Når siste14avedtak har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(siste14avedtakMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("siste14avedtak")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("siste14avedtak")

        val siste14avedtakMelding = melding.get("siste14avedtak")

        assertThat(siste14avedtakMelding.get("aktorId").asText()).isEqualTo("12141321")
        assertThat(siste14avedtakMelding.get("innsatsgruppe").asText()).isEqualTo("STANDARD_INNSATS")
        assertThat(siste14avedtakMelding.get("hovedmal").asText()).isEqualTo("SKAFFE_ARBEID")
        assertThat(siste14avedtakMelding.get("fattetDato").asText()).isEqualTo("2021-09-08T09:29:20.398043+02:00")
        assertThat(siste14avedtakMelding.get("fraArena").asBoolean()).isFalse
    }

    @Test
    fun `Når oppfølgingsperiode har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(oppfølgingsperiodeMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("oppfølgingsperiode")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("oppfølgingsperiode")

        val oppfølgingsperiodePåMelding = melding.get("oppfølgingsperiode")

        assertThat(oppfølgingsperiodePåMelding.get("uuid").asText()).isEqualTo("0b0e2261-343d-488e-a70f-807f4b151a2f")
        assertThat(oppfølgingsperiodePåMelding.get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(oppfølgingsperiodePåMelding.get("startDato").asText()).isEqualTo("2021-12-01T18:18:00.435004+01:00")
        assertThat(oppfølgingsperiodePåMelding.get("sluttDato").isNull).isTrue
    }

    @Test
    fun `Når arenaFritattKandidatsøk har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "123"
        val testRapid = TestRapid()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(arenaFritattKandidatsøkMelding(aktørId, true))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("arena-fritatt-kandidatsøk")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("arenaFritattKandidatsøk")

        val fritattKandidatsøkPåMelding = melding.get("arenaFritattKandidatsøk")
        assertThat(fritattKandidatsøkPåMelding.get("erFritattKandidatsøk").asBoolean()).isTrue
    }

    @Test
    fun `Når hjemmel har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "123"
        val testRapid = TestRapid()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(hjemmelMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("hjemmel")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("hjemmel")

        val hjemmelmelding = melding.get("hjemmel")

        assertThat(hjemmelmelding.get("samtykkeId").asInt()).isEqualTo(1)
        assertThat(hjemmelmelding.get("aktoerId").asText()).isEqualTo("AktorId(aktorId=$aktørId)")
        assertThat(hjemmelmelding.get("fnr").asText()).isEqualTo("27075349594")
        assertThat(hjemmelmelding.get("ressurs").asText()).isEqualTo("CV_HJEMMEL")
        assertThat(hjemmelmelding.get("opprettetDato").asText()).isEqualTo("2019-01-09T12:36:06+01:00")
        assertThat(hjemmelmelding.get("slettetDato").isNull).isTrue
        assertThat(hjemmelmelding.get("versjon").asInt()).isEqualTo(1)
        assertThat(hjemmelmelding.get("versjonGjeldendeFra").isNull).isTrue
        assertThat(hjemmelmelding.get("versjonGjeldendeTil").asText()).isEqualTo("2019-04-08")
    }

    @Test
    fun `Når måBehandleTidligereCv har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "123"
        val testRapid = TestRapid()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(måBehandleTidligereCvMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("må-behandle-tidligere-cv")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("måBehandleTidligereCv")

        val måBehandleTidligereCvmelding = melding.get("måBehandleTidligereCv")

        assertThat(måBehandleTidligereCvmelding.get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(måBehandleTidligereCvmelding.get("maaBehandleTidligereCv").asBoolean()).isEqualTo(true)
    }

    @Test
    fun `Når måBehandleTidligereCv har blitt mottatt skal meldingen lagres i databasen`() {
        val aktørId = "123"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")

        testRapid.sendTestMessage(måBehandleTidligereCvMelding(aktørId))

        val lagredeKandidater = testDatabase.hentAlleKandidater()
        assertThat(lagredeKandidater.size).isEqualTo(1)
        assertThat(lagredeKandidater.first().måBehandleTidligereCv).isNotNull
    }

    @Test
    fun `Når siste14avedtak har blitt mottatt skal meldingen lagres i databasen`() {
        val aktørId = "123"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")

        testRapid.sendTestMessage(siste14avedtakMelding(aktørId))

        val lagredeKandidater = testDatabase.hentAlleKandidater()
        assertThat(lagredeKandidater.size).isEqualTo(1)
        assertThat(lagredeKandidater.first().siste14avedtak).isNotNull
    }

    @Test
    fun `Når arbeidsmarkedCv-melding har blitt mottatt skal meldingen lagres i databasen`() {
        val aktørId = "123"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")

        testRapid.sendTestMessage(arbeidsmarkedCvMelding(aktørId))

        val lagredeKandidater = testDatabase.hentAlleKandidater()
        assertThat(lagredeKandidater.size).isEqualTo(1)
        assertThat(lagredeKandidater.first().arbeidsmarkedCv).isNotNull
    }

    @Test
    fun `Når flere CV- og veiledermeldinger mottas for én kandidat skal det være én rad for kandidaten i databasen`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(cvMelding(aktørId))
        testRapid.sendTestMessage(veilederMelding(aktørId))
        testRapid.sendTestMessage(cvMelding(aktørId))
        testRapid.sendTestMessage(cvMelding(aktørId))
        testRapid.sendTestMessage(veilederMelding(aktørId))
        testRapid.sendTestMessage(veilederMelding(aktørId))

        val antallLagredeKandidater = testDatabase.hentAntallKandidater()
        assertThat(antallLagredeKandidater).isEqualTo(1)
    }

    @Test
    fun `Objekter inne i sammenstiltKandidat-melding skal ikke være stringified`() {
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(oppfølgingsinformasjonMelding("12141321"))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        val oppfølgingsinformasjonPåMelding = melding.get("oppfølgingsinformasjon")
        assertThat(oppfølgingsinformasjonPåMelding.isObject).isTrue
    }

    @Test
    fun `Metadata fra opprinnelig melding skal ikke fjernes når sammenstilleren publiserer ny melding på rapid`() {
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(cvMeldingMedSystemParticipatingServices())

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)

        val felterPåMelding = melding.fieldNames().asSequence().toList()
        assertThat(felterPåMelding).contains("system_participating_services")
        assertThat(felterPåMelding).contains("system_read_count")
    }

    @Test
    fun `Sammenstiller øker system_read_count med 1`() {
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(cvMeldingMedSystemParticipatingServices())

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("system_read_count").asInt()).isEqualTo(2)
    }

    @Test
    fun `Sammenstiller skal ikke republisere en melding som allerede har blitt republisert`() {
        val aktørId = "123"
        val testRapid = TestRapid()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(arenaFritattKandidatsøkMelding(aktørId, true))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        testRapid.sendTestMessage(melding.toPrettyString())

        assertThat(testRapid.inspektør.size).isEqualTo(1)
    }

    @Test
    fun `Når kvp event har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "123"
        val testRapid = TestRapid()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
        testRapid.sendTestMessage(kvp(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("kvp")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("kvp")

        val kvp = melding.get("kvp")
        assertThat(kvp.get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(kvp.get("event").asText()).isEqualTo("AVSLUTTET")
        assertThat(kvp.get("enhetId").asText()).isEqualTo("1860")
        assertThat(kvp.get("startet").get("opprettetDato").asText()).isEqualTo("2023-01-03T09:44:23.394628+01:00")
        assertThat(kvp.get("avsluttet").get("avsluttetDato").asText()).isEqualTo("2023-01-03T09:44:48.891877+01:00")

    }

    @Test
    fun `Når kvp-melding har blitt mottatt skal meldingen lagres i databasen`() {
        val aktørId = "123"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")

        testRapid.sendTestMessage(kvp(aktørId))

        val lagredeKandidater = testDatabase.hentAlleKandidater()
        assertThat(lagredeKandidater.size).isEqualTo(1)
        assertThat(lagredeKandidater.first().kvp).isNotNull
    }
}
