package com.immoradar.backend.evidence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:sqlite:file:evidence-tests?mode=memory&cache=shared",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DealEvidenceIntegrationTests {
    @LocalServerPort private int port;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void persistsEvidenceAndKeepsItSeparateBetweenProperties() throws Exception {
        var initial = send("GET", "/api/deals/2/evidence", "");
        assertThat(initial.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(initial.body()).path("documentedCount").asInt()).isZero();

        var request = new UpdateEvidenceRequest(EvidenceStatus.DOCUMENTED, "https://example.com/bail",
                "Bail actuel remis par le vendeur", LocalDate.now());
        var updated = send("PUT", "/api/deals/2/evidence/RENT", mapper.writeValueAsString(request));
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(updated.body()).path("documentedCount").asInt()).isEqualTo(1);

        var reread = mapper.readTree(send("GET", "/api/deals/2/evidence", "").body());
        assertThat(reread.path("checks").toString()).contains("Bail actuel remis par le vendeur");
        assertThat(mapper.readTree(send("GET", "/api/deals/3/evidence", "").body()).path("documentedCount").asInt()).isZero();
        assertThat(send("GET", "/api/deals/missing/evidence", "").statusCode()).isEqualTo(404);
    }

    @Test
    void rejectsFutureDatesUnsafeLinksAndUnsupportedStatuses() throws Exception {
        var future = new UpdateEvidenceRequest(EvidenceStatus.DOCUMENTED, "", "Justificatif", LocalDate.now().plusDays(1));
        assertThat(send("PUT", "/api/deals/1/evidence/DPE", mapper.writeValueAsString(future)).statusCode()).isEqualTo(400);
        var unsafe = new UpdateEvidenceRequest(EvidenceStatus.DOCUMENTED, "javascript:alert(1)", "Justificatif", LocalDate.now());
        assertThat(send("PUT", "/api/deals/1/evidence/DPE", mapper.writeValueAsString(unsafe)).statusCode()).isEqualTo(400);
        var waived = new UpdateEvidenceRequest(EvidenceStatus.NOT_APPLICABLE, "", "Sans preuve", LocalDate.now());
        assertThat(send("PUT", "/api/deals/1/evidence/RENT", mapper.writeValueAsString(waived)).statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }
}
