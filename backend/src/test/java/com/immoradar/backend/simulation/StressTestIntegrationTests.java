package com.immoradar.backend.simulation;

import com.immoradar.backend.deal.DealService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:sqlite:file:stress-tests?mode=memory&cache=shared",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StressTestIntegrationTests {
    @LocalServerPort private int port;
    @Autowired private DealService deals;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void returnsRealCalculationsWithoutChangingStoredRent() throws Exception {
        var base = StressTestServiceTests.base();
        var request = new SimulationRequest("2", base.downpayment(), base.interestRate(), base.loanTermYears(),
                base.taxRegime(), base.marginalTaxRate(), base.vacancyRate(), base.managementRate(),
                base.insuranceAnnual(), base.rentGrowthRate(), base.propertyGrowthRate());
        var rentBefore = deals.getEntity("2").getMonthlyRent();
        var response = send(mapper.writeValueAsString(StressTestServiceTests.stress(request, "10", "10", "15", "5")));
        assertThat(response.statusCode()).isEqualTo(200);
        var result = mapper.readValue(response.body(), StressTestResponse.class);
        assertThat(result.scenarios()).hasSize(3);
        assertThat(result.scenarios().get(1).monthlyRent()).isLessThan(rentBefore);
        assertThat(deals.getEntity("2").getMonthlyRent()).isEqualByComparingTo(rentBefore);
    }

    @Test
    void validatesNestedFinancingAndShockBounds() throws Exception {
        var invalid = StressTestServiceTests.stress(StressTestServiceTests.base(), "41", "10", "15", "5");
        assertThat(send(mapper.writeValueAsString(invalid)).statusCode()).isEqualTo(400);
        String valid = mapper.writeValueAsString(StressTestServiceTests.stress(StressTestServiceTests.base(), "10", "10", "15", "5"));
        assertThat(send(valid.replace("\"loanTermYears\":20", "\"loanTermYears\":0")).statusCode()).isEqualTo(400);
        assertThat(send("{}").statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> send(String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/simulations/stress-test"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
