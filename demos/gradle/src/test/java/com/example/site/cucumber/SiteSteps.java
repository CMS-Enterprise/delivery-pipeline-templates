package com.example.site.cucumber;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;

public class SiteSteps {

    private final HttpClient client = HttpClient.newHttpClient();

    // local.server.port is published by the RANDOM_PORT web environment.
    @Value("${local.server.port}")
    private int port;

    private HttpResponse<String> response;

    @When("a client requests {string}")
    public void aClientRequests(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(response.statusCode()).isEqualTo(status);
    }

    @Then("the response body contains {string}")
    public void theResponseBodyContains(String text) {
        assertThat(response.body()).contains(text);
    }
}
