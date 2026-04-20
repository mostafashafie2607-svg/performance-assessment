package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class SteadyAuthSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol =
        http.baseUrl("https://dummyjson.com")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling Steady Auth Test")
            .disableCaching();

    FeederBuilder<String> userFeeder =
        csv("data/users.csv").circular();

    ChainBuilder login =
        exec(
            http("Login Request")
                .post("/auth/login")
                .asJson()
                .body(StringBody(
                    "{ \"username\": \"#{username}\", \"password\": \"#{password}\", \"expiresInMins\": 30 }"
                ))
                .check(status().is(200))
                .check(jsonPath("$.accessToken").saveAs("token"))
                .check(jsonPath("$.refreshToken").saveAs("refreshToken"))
        );

    ChainBuilder getCurrentUser =
        exec(
            http("Get Current User")
                .get("/auth/me")
                .header("Authorization", "Bearer #{token}")
                .check(status().is(200))
                .check(jsonPath("$.id").exists())
                .check(jsonPath("$.username").isEL("#{username}"))
        );

    ChainBuilder refreshSession =
        exec(
            http("Refresh Session")
                .post("/auth/refresh")
                .asJson()
                .body(StringBody(
                    "{ \"refreshToken\": \"#{refreshToken}\", \"expiresInMins\": 30 }"
                ))
                .check(status().is(200))
                .check(jsonPath("$.accessToken").saveAs("token"))
                .check(jsonPath("$.refreshToken").saveAs("refreshToken"))
        );

    ChainBuilder authFlow =
        exec(login)
            .exitHereIfFailed()
            .pause(2)
            .exec(getCurrentUser)
            .exitHereIfFailed()
            .pause(2)
            .exec(refreshSession)
            .exitHereIfFailed()
            .pause(2)
            .exec(getCurrentUser);

    ScenarioBuilder steadyScenario =
        scenario("Steady Auth Flow")
            .forever().on(
                pace(Duration.ofSeconds(20))
                    .feed(userFeeder)
                    .exec(authFlow)
            );

    {
        setUp(
            steadyScenario.injectClosed(
                rampConcurrentUsers(0).to(10).during(20),
                constantConcurrentUsers(10).during(560),
                rampConcurrentUsers(10).to(0).during(20)
            ).protocols(httpProtocol)
        ).assertions(
            global().successfulRequests().percent().gt(85.0),
            global().responseTime().percentile(95).lt(2500),
            details("Login Request").failedRequests().percent().lt(15.0),
            details("Get Current User").failedRequests().percent().lt(5.0),
            details("Refresh Session").failedRequests().percent().lt(5.0)
        );
    }
}