package no.nav.arbeidsgiver.toi

import com.mongodb.MongoClient
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun startApp(repository: Repository) = RapidApplication.create(System.getenv()).also { rapid ->
    System.setProperty("java.net.preferIPv4Stack" , "true");
    val behandler =  Behandler( repository, rapid::publish)

    VeilederLytter(rapid, behandler)
    CvLytter(rapid, behandler)
}.start()

val mongoDbUrl = System.getenv("MONGODB_URL")
val mongoClient = MongoClient(mongoDbUrl)

fun main() = startApp(Repository(mongoClient))

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)