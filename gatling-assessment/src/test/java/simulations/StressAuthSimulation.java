package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class StressAuthSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol =
        http.baseUrl("https://dummyjson.com")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling Stress Auth Test")
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

    ScenarioBuilder stressScenario =
        scenario("Stress Auth Flow")
            .forever().on(
                pace(Duration.ofSeconds(10))
                    .feed(userFeeder)
                    .exec(authFlow)
            );

    {
        setUp(
            stressScenario.injectClosed(
                rampConcurrentUsers(0).to(20).during(30),
                rampConcurrentUsers(20).to(50).during(60)
            ).protocols(httpProtocol)
        ).assertions(
            global().responseTime().percentile(95).lt(3000),
            global().failedRequests().percent().lt(20.0)
        );
    }
}