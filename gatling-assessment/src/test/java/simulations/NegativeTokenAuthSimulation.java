package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class NegativeTokenAuthSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol =
        http.baseUrl("https://dummyjson.com")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling Negative Token Test")
            .disableCaching();

    ChainBuilder accessProtectedEndpointWithoutToken =
        exec(
            http("Get Current User Without Token")
                .get("/auth/me")
                .check(status().in(401, 403))
        );

    ScenarioBuilder negativeTokenScenario =
        scenario("Negative Missing Token Flow")
            .exec(accessProtectedEndpointWithoutToken);

    {
        setUp(
            negativeTokenScenario.injectClosed(
                constantConcurrentUsers(1).during(1)
            ).protocols(httpProtocol)
        ).assertions(
            global().successfulRequests().percent().gt(95.0)
        );
    }
}