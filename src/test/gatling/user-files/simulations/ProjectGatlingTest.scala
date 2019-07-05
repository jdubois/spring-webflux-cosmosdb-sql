import _root_.io.gatling.core.scenario.Simulation
import ch.qos.logback.classic.{Level, LoggerContext}
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

/**
  * Performance test for the Project entity.
  */
class ProjectGatlingTest extends Simulation {

    val context: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    // Log all HTTP requests
    //context.getLogger("io.gatling.http").setLevel(Level.valueOf("TRACE"))
    // Log failed HTTP requests
    //context.getLogger("io.gatling.http").setLevel(Level.valueOf("DEBUG"))

    val baseURL = Option(System.getProperty("baseURL")) getOrElse """https://judubois-demo.azurewebsites.net/"""

    val httpConf = http
        .baseUrl(baseURL)
        .inferHtmlResources()
        .acceptHeader("*/*")
        .acceptEncodingHeader("gzip, deflate")
        .acceptLanguageHeader("fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3")
        .connectionHeader("keep-alive")
        .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:33.0) Gecko/20100101 Firefox/33.0")
        .silentResources // Silence all resources like css or css so they don't clutter the results

    val headers_http = Map(
        "Accept" -> """application/json"""
    )

    val scn = scenario("Test the Project entity")
        .repeat(2) {
            exec(http("Get all projects")
                .get("/api/projects")
                .check(status.is(200)))
                .pause(10 seconds, 20 seconds)
                .exec(http("Create new project")
                    .post("/api/projects")
                    .body(StringBody("""{
                "id":null
                , "name":"SAMPLE_TEXT"
                }""")).asJson
                    .check(status.is(200))
                    .check(jsonPath("$.id").saveAs("new_project_id"))).exitHereIfFailed
                .pause(10)
                .repeat(5) {
                    exec(http("Get created project")
                        .get("/api/projects/${new_project_id}"))
                        .pause(10)
                }
                .exec(http("Delete created project")
                    .delete("/api/projects/${new_project_id}"))
                .pause(10)
        }

    val users = scenario("Users").exec(scn)

    setUp(
        users.inject(rampUsers(Integer.getInteger("users", 100)) during (Integer.getInteger("ramp", 1) minutes))
    ).protocols(httpConf)
}
