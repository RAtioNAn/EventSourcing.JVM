package io.eventdriven.introductiontoeventsourcing.e07_application_logic.mongodb.mutable.tests.api.builders;

import io.eventdriven.introductiontoeventsourcing.e07_application_logic.mongodb.mutable.app.api.ShoppingCartsRequests.AddProduct;
import io.eventdriven.introductiontoeventsourcing.e07_application_logic.mongodb.mutable.app.api.ShoppingCartsRequests.Open;
import io.eventdriven.introductiontoeventsourcing.e07_application_logic.mongodb.mutable.app.api.ShoppingCartsRequests.ProductItemRequest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class ShoppingCartRestBuilder {
  private final String apiPrefix = "api/shopping-carts";
  private final TestRestTemplate restTemplate;
  private final int port;
  private UUID clientId;
  private boolean isConfirmed;
  private boolean isCanceled;
  private final List<ProductItemRequest> products = new ArrayList<>();

  public String getApiUrl() {
    return "http://localhost:%s/%s".formatted(port, apiPrefix);
  }

  private ShoppingCartRestBuilder(TestRestTemplate restTemplate, int port) {
    this.restTemplate = restTemplate;
    this.port = port;
  }

  public static ShoppingCartRestBuilder of(TestRestTemplate restTemplate, int port) {
    return new ShoppingCartRestBuilder(restTemplate, port);
  }

  public ShoppingCartRestBuilder withClientId(UUID clientId) {
    this.clientId = clientId;

    return this;
  }

  public ShoppingCartRestBuilder withProduct(ProductItemRequest product) {
    this.products.add(product);

    return this;
  }

  public ShoppingCartRestBuilder confirmed() {
    this.isConfirmed = true;

    return this;
  }

  public ShoppingCartRestBuilder canceled() {
    this.isCanceled = true;

    return this;
  }

  public BuilderResult build(Consumer<ShoppingCartRestBuilder> with) {
    with.accept(this);

    return execute();
  }

  public BuilderResult execute() {
    BuilderResult result = open();

    if (isConfirmed) {
      result = confirm(result);
    }

    if (isCanceled) {
      result = cancel(result);
    }

    for (var product: products){
      result = addProduct(product, result);
    }

    return result;
  }

  private BuilderResult open() {
    var response = this.restTemplate
      .postForEntity(getApiUrl(), new Open(clientId), Void.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var locationHeader = response.getHeaders().getLocation();

    assertNotNull(locationHeader);

    var location = locationHeader.toString();

    assertTrue(location.startsWith(apiPrefix));
    var newId = assertDoesNotThrow(() -> UUID.fromString(location.substring(apiPrefix.length() + 1)));

    return getResult(response, newId);
  }

  private BuilderResult addProduct(ProductItemRequest product, BuilderResult result) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    var request = new HttpEntity<>(new AddProduct(product), headers);

    var response = this.restTemplate
      .postForEntity(getApiUrl() + "/%s/products".formatted(result.id()), request, Void.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    return getResult(response, result.id());
  }


  private BuilderResult confirm(BuilderResult result) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    var request = new HttpEntity<Void>(null, headers);

    var response = this.restTemplate
      .exchange("%s/%s".formatted(getApiUrl(), result.id()), HttpMethod.PUT, request, Void.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    return getResult(response, result.id());
  }

  private BuilderResult cancel(BuilderResult result) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    var request = new HttpEntity<Void>(null, headers);

    var response = this.restTemplate
      .exchange("%s/%s".formatted(getApiUrl(), result.id()), HttpMethod.DELETE, request, Void.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    return getResult(response, result.id());
  }

  private BuilderResult getResult(ResponseEntity<Void> response, UUID newId) {
    return new BuilderResult(newId);
  }

  public record BuilderResult(
    UUID id
  ) {
  }
}
