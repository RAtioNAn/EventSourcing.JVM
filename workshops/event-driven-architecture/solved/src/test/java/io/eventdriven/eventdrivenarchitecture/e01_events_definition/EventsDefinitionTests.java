package io.eventdriven.eventdrivenarchitecture.e01_events_definition;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventsDefinitionTests {
  // 1. Define your events and entity here
  record ShoppingCartOpened(
    UUID shoppingCartId,
    UUID clientId
  ) {
  }

  record ProductItemAddedToShoppingCart(
    UUID shoppingCartId,
    PricedProductItem productItem
  ) {
  }

  record ProductItemRemovedFromShoppingCart(
    UUID shoppingCartId,
    PricedProductItem productItem
  ) {
  }

  record ShoppingCartConfirmed(
    UUID shoppingCartId,
    OffsetDateTime confirmedAt
  ) {
  }

  record ShoppingCartCanceled(
    UUID shoppingCartId,
    OffsetDateTime canceledAt
  ) {
  }

  // VALUE OBJECTS
  public static class PricedProductItem {
    private UUID productId;
    private double unitPrice;
    private int quantity;

    public PricedProductItem() {
    }

    public PricedProductItem(UUID productId, int quantity, double unitPrice) {
      this.setProductId(productId);
      this.setUnitPrice(unitPrice);
      this.setQuantity(quantity);
    }

    private double gettotalAmount() {
      return quantity() * unitPrice();
    }

    public UUID productId() {
      return productId;
    }

    public void setProductId(UUID productId) {
      this.productId = productId;
    }

    public double unitPrice() {
      return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
      this.unitPrice = unitPrice;
    }

    public int quantity() {
      return quantity;
    }

    public void setQuantity(int quantity) {
      this.quantity = quantity;
    }
  }

  public record ImmutablePricedProductItem(
    UUID productId,
    int quantity,
    double unitPrice
  ) {
    public double totalAmount() {
      return quantity * unitPrice;
    }
  }

  // ENTITY
  // regular one
  public static class ShoppingCart {
    private UUID id;
    private UUID clientId;
    private ShoppingCartStatus status;
    private List<PricedProductItem> productItems;
    private OffsetDateTime confirmedAt;
    private OffsetDateTime canceledAt;

    public ShoppingCart(UUID id, UUID clientId, ShoppingCartStatus status, List<PricedProductItem> productItems, OffsetDateTime confirmedAt, OffsetDateTime canceledAt) {
      this.id = id;
      this.clientId = clientId;
      this.status = status;
      this.productItems = productItems;
      this.confirmedAt = confirmedAt;
      this.canceledAt = canceledAt;
    }

    public UUID id() {
      return id;
    }

    public void setId(UUID id) {
      this.id = id;
    }

    public UUID clientId() {
      return clientId;
    }

    public void setClientId(UUID clientId) {
      this.clientId = clientId;
    }

    public ShoppingCartStatus status() {
      return status;
    }

    public void setStatus(ShoppingCartStatus status) {
      this.status = status;
    }

    public List<PricedProductItem> productItems() {
      return productItems;
    }

    public void setProductItems(List<PricedProductItem> productItems) {
      this.productItems = productItems;
    }

    public OffsetDateTime confirmedAt() {
      return confirmedAt;
    }

    public void setConfirmedAt(OffsetDateTime confirmedAt) {
      this.confirmedAt = confirmedAt;
    }

    public OffsetDateTime canceledAt() {
      return canceledAt;
    }

    public void setCanceledAt(OffsetDateTime canceledAt) {
      this.canceledAt = canceledAt;
    }
  }

  // immutable one
  public record ImmutableShoppingCart(
    UUID id,
    UUID clientId,
    ShoppingCartStatus status,
    PricedProductItem[] productItems,
    OffsetDateTime confirmedAt,
    OffsetDateTime canceledAt) {
  }

  public enum ShoppingCartStatus {
    Pending,
    Confirmed,
    Canceled
  }

  @Test
  public void guestStayAccountEventTypes_AreDefined() {
    // Given
    var guestStayId = UUID.randomUUID();
    var groupCheckoutId = UUID.randomUUID();


    // When
    var events = new Object[]
      {
        new GuestStayAccountEvent.GuestCheckedIn(guestStayId, OffsetDateTime.now()),
        new GuestStayAccountEvent.ChargeRecorded(guestStayId, 100, OffsetDateTime.now()),
        new GuestStayAccountEvent.PaymentRecorded(guestStayId, 100, OffsetDateTime.now()),
        new GuestStayAccountEvent.GuestCheckedOut(guestStayId, OffsetDateTime.now(), groupCheckoutId),
        new GuestStayAccountEvent.GuestCheckoutFailed(guestStayId,
          GuestStayAccountEvent.GuestCheckoutFailed.Reason.InvalidState, OffsetDateTime.now(),
          groupCheckoutId)
      };

    // Then
    final int expectedEventTypesCount = 5;
    assertEquals(expectedEventTypesCount, events.length);
    assertEquals(expectedEventTypesCount, Arrays.stream(events).collect(Collectors.groupingBy(Object::getClass)).size());
  }


  @Test
  public void groupCheckoutEventTypes_AreDefined() {
    // Given
    var groupCheckoutId = UUID.randomUUID();
    var guestStayIds = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
    var clerkId = UUID.randomUUID();

    // When
    var events = new Object[]
      {
        new GroupCheckoutEvent.GroupCheckoutInitiated(groupCheckoutId, clerkId, guestStayIds, OffsetDateTime.now()),
        new GroupCheckoutEvent.GuestCheckoutCompleted(groupCheckoutId, guestStayIds[0], OffsetDateTime.now()),
        new GroupCheckoutEvent.GuestCheckoutFailed(groupCheckoutId, guestStayIds[1], OffsetDateTime.now()),
        new GroupCheckoutEvent.GroupCheckoutFailed(groupCheckoutId, new UUID[]{guestStayIds[0]}, new UUID[]{guestStayIds[1]}, OffsetDateTime.now()),
        new GroupCheckoutEvent.GroupCheckoutCompleted(groupCheckoutId, guestStayIds, OffsetDateTime.now())
      };

    // Then
    final int expectedEventTypesCount = 5;
    assertEquals(expectedEventTypesCount, events.length);
    assertEquals(expectedEventTypesCount, Arrays.stream(events).collect(Collectors.groupingBy(Object::getClass)).size());
  }
}
