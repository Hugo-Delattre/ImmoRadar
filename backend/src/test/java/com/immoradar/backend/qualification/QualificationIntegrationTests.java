package com.immoradar.backend.qualification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.immoradar.backend.market.DvfMarketService;
import com.immoradar.backend.market.DvfMarketAnalysis;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:sqlite:file:qualification-tests?mode=memory&cache=shared",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class QualificationIntegrationTests {
    @LocalServerPort private int port;
    @MockitoBean private DvfMarketService market;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void persistsAndRemovesReferencesWithoutCrossPropertyAccessAndRejectsDuplicates() throws Exception {
        String json = reference("https://agency.fr/location/unique");
        var added = send("POST", "/api/deals/2/rental-references", json);
        assertThat(added.statusCode()).isEqualTo(200);
        String id = mapper.readTree(added.body()).get(0).path("id").asText();
        assertThat(mapper.readTree(send("GET", "/api/deals/2/rental-references", "").body()).size()).isEqualTo(1);
        assertThat(mapper.readTree(send("GET", "/api/deals/3/rental-references", "").body()).size()).isZero();
        assertThat(send("POST", "/api/deals/2/rental-references", reference("https://agency.fr/location/unique?track=2#top")).statusCode()).isEqualTo(400);
        assertThat(send("DELETE", "/api/deals/3/rental-references/" + id, "").statusCode()).isEqualTo(400);
        assertThat(send("DELETE", "/api/deals/2/rental-references/" + id, "").statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(send("GET", "/api/deals/2/rental-references", "").body()).size()).isZero();
        assertThat(send("GET", "/api/deals/missing/rental-references", "").statusCode()).isEqualTo(404);
    }

    @Test
    void positiveHttpEvaluationRechecksStoredInputsAfterRemovingAReference() throws Exception {
        // All sources and amounts below are fictional fixtures; the market is explicitly mocked.
        var created = send("POST", "/api/deals", """
                {"title":"Qualification fixture isolée","price":80000,"monthlyRent":900,"monthlyCharges":40,
                "propertyTax":600,"renovationCost":5000,"location":"Limoges (87)","surface":50,
                "propertyType":"Apartment","description":"Fixture, pas une annonce réelle.","imageUrl":"",
                "sourceUrl":"https://agency.fr/vente/fixture"}
                """);
        assertThat(created.statusCode()).isEqualTo(201);
        String id = mapper.readTree(created.body()).path("id").asText();
        when(market.analyzeDeal(id)).thenReturn(new DvfMarketAnalysis(true, "Limoges", "87085", "Appartement",
                new BigDecimal("1600"), new BigDecimal("3000"), BigDecimal.ZERO, 25, LocalDate.now().getYear() - 1,
                "Fixture", "https://foncierdata.fr/fixture", null, List.of(), null, "Fixture fictive"));
        for (var field : com.immoradar.backend.evidence.EvidenceField.values()) {
            assertThat(send("PUT", "/api/deals/" + id + "/evidence/" + field.name(), """
                    {"status":"DOCUMENTED","sourceUrl":"","note":"Document fictif pour test.","checkedOn":"%s"}
                    """.formatted(LocalDate.now())).statusCode()).isEqualTo(200);
        }
        String referenceId = "";
        for (int i = 0; i < 3; i++) {
            var response = send("POST", "/api/deals/" + id + "/rental-references", reference("https://agency.fr/location/positive-" + i));
            assertThat(response.statusCode()).isEqualTo(200);
            referenceId = mapper.readTree(response.body()).get(0).path("id").asText();
        }
        String request = """
                {"base":{"dealId":"%s","downpayment":30000,"interestRate":3.5,"loanTermYears":20,
                "taxRegime":"REEL_LMNP","marginalTaxRate":30,"vacancyRate":4,"managementRate":0,
                "insuranceAnnual":180,"rentGrowthRate":1.5,"propertyGrowthRate":1.2},"rentalMode":"FURNISHED"}
                """.formatted(id);
        var qualified = send("POST", "/api/deals/" + id + "/qualification", request);
        assertThat(qualified.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(qualified.body()).path("outcome").asText()).isEqualTo("POTENTIAL");
        assertThat(mapper.readTree(qualified.body()).path("certified").asBoolean()).isFalse();
        assertThat(send("DELETE", "/api/deals/" + id + "/rental-references/" + referenceId, "").statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(send("POST", "/api/deals/" + id + "/qualification", request).body()).path("outcome").asText()).isEqualTo("INCOMPLETE");
    }

    @Test
    void rejectsUnsafeUrlsInvalidFinancialValuesAndFutureDates() throws Exception {
        assertThat(send("POST", "/api/deals/2/rental-references", reference("javascript:alert(1)")).statusCode()).isEqualTo(400);
        assertThat(send("POST", "/api/deals/2/rental-references", reference("https://user:pass@agency.fr/x")).statusCode()).isEqualTo(400);
        assertThat(send("POST", "/api/deals/2/rental-references", reference("https://agency.fr/x").replace("\"monthlyRent\":1000", "\"monthlyRent\":0")).statusCode()).isEqualTo(400);
        assertThat(send("POST", "/api/deals/2/rental-references", reference("https://agency.fr/x").replace(LocalDate.now().toString(), LocalDate.now().plusDays(1).toString())).statusCode()).isEqualTo(400);
    }

    @Test
    void realHttpEvaluationNeverCertifiesSeededPropertiesAndValidatesNestedSimulation() throws Exception {
        when(market.analyzeDeal("2")).thenReturn(DvfMarketAnalysis.unavailable("Limoges", new BigDecimal("1700"), "Indisponible"));
        String request = """
                {"base":{"dealId":"2","downpayment":30000,"interestRate":3.5,"loanTermYears":20,
                "taxRegime":"REEL_LMNP","marginalTaxRate":30,"vacancyRate":4,"managementRate":0,
                "insuranceAnnual":180,"rentGrowthRate":1.5,"propertyGrowthRate":1.2},"rentalMode":"FURNISHED"}
                """;
        var response = send("POST", "/api/deals/2/qualification", request);
        assertThat(response.statusCode()).isEqualTo(200);
        var result = mapper.readTree(response.body());
        assertThat(result.path("certified").asBoolean()).isFalse();
        assertThat(result.path("outcome").asText()).isNotEqualTo("POTENTIAL");
        assertThat(result.path("checks").toString()).contains("MISSING");
        assertThat(result.path("assumptions").path("base").path("downpayment").asInt()).isEqualTo(30000);
        assertThat(send("POST", "/api/deals/3/qualification", request).statusCode()).isEqualTo(400);
        assertThat(send("POST", "/api/deals/2/qualification", request.replace("\"interestRate\":3.5", "\"interestRate\":25")).statusCode()).isEqualTo(400);
    }

    private String reference(String source) {
        return """
                {"sourceUrl":"%s","observedOn":"%s","location":"Limoges (87)","propertyType":"Apartment",
                "surface":50,"monthlyRent":1000,"rentalMode":"FURNISHED","kind":"ACTUAL_LEASE",
                "note":"Fixture fictive de bail, aucun document réel."}
                """.formatted(source, LocalDate.now());
    }
    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json")
                .method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }
}
