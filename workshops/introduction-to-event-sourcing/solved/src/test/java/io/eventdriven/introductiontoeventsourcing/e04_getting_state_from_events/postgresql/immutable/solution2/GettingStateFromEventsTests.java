package io.eventdriven.introductiontoeventsourcing.e04_getting_state_from_events.postgresql.immutable.solution2;

import io.eventdriven.eventstores.EventStore;
import io.eventdriven.eventstores.StreamName;
import io.eventdriven.eventstores.postgresql.PostgreSQLEventStore;
import io.eventdriven.eventstores.testing.tools.postgresql.PostgreSQLTest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static io.eventdriven.introductiontoeventsourcing.e04_getting_state_from_events.postgresql.immutable.FunctionalTools.groupingByOrdered;
import static io.eventdriven.introductiontoeventsourcing.e04_getting_state_from_events.postgresql.immutable.solution2.GettingStateFromEventsTests.ShoppingCartEvent.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GettingStateFromEventsTests extends PostgreSQLTest {
  public sealed interface ShoppingCartEvent {
    record ShoppingCartOpened(
      UUID shoppingCartId,
      UUID clientId
    ) implements ShoppingCartEvent {
    }

    record ProductItemAddedToShoppingCart(
      UUID shoppingCartId,
      PricedProductItem productItem
    ) implements ShoppingCartEvent {
    }

    record ProductItemRemovedFromShoppingCart(
      UUID shoppingCartId,
      PricedProductItem productItem
    ) implements ShoppingCartEvent {
    }

    record ShoppingCartConfirmed(
      UUID shoppingCartId,
      OffsetDateTime confirmedAt
    ) implements ShoppingCartEvent {
    }

    record ShoppingCartCanceled(
      UUID shoppingCartId,
      OffsetDateTime canceledAt
    ) implements ShoppingCartEvent {
    }
  }

  public record PricedProductItem(
    UUID productId,
    int quantity,
    double unitPrice
  ) {
    public double totalAmount() {
      return quantity * unitPrice;
    }
  }

  public record ProductItems(
    PricedProductItem[] values
  ) {
    public static ProductItems empty() {
      return new ProductItems(new PricedProductItem[]{});
    }

    public ProductItems add(PricedProductItem productItem) {
      return new ProductItems(
        Stream.concat(Arrays.stream(values), Stream.of(productItem))
          .collect(groupingByOrdered(PricedProductItem::productId))
          .entrySet().stream()
          .map(group -> group.getValue().size() == 1 ?
            group.getValue().get(0) :
            new PricedProductItem(
              group.getKey(),
              group.getValue().stream().mapToInt(PricedProductItem::quantity).sum(),
              group.getValue().get(0).unitPrice()
            )
          )
          .toArray(PricedProductItem[]::new)
      );
    }

    public ProductItems remove(PricedProductItem productItem) {
      return new ProductItems(
        Arrays.stream(values())
          .map(pi -> pi.productId().equals(productItem.productId()) ?
            new PricedProductItem(
              pi.productId(),
              pi.quantity() - productItem.quantity(),
              pi.unitPrice()
            )
            : pi
          )
          .filter(pi -> pi.quantity > 0)
          .toArray(PricedProductItem[]::new)
      );
    }
  }

  // ENTITY
  sealed public interface ShoppingCart {
    UUID id();

    UUID clientId();

    ProductItems productItems();

    record PendingShoppingCart(
      UUID id,
      UUID clientId,
      ProductItems productItems
    ) implements ShoppingCart {
    }

    record ConfirmedShoppingCart(
      UUID id,
      UUID clientId,
      ProductItems productItems,
      OffsetDateTime confirmedAt
    ) implements ShoppingCart {
    }

    record CanceledShoppingCart(
      UUID id,
      UUID clientId,
      ProductItems productItems,
      OffsetDateTime canceledAt
    ) implements ShoppingCart {
    }

    default ShoppingCartStatus status() {
      return switch (this) {
        case PendingShoppingCart ignored:
          yield ShoppingCartStatus.Pending;
        case ConfirmedShoppingCart ignored:
          yield ShoppingCartStatus.Confirmed;
        case CanceledShoppingCart ignored:
          yield ShoppingCartStatus.Canceled;
      };
    }

    static ShoppingCart evolve(ShoppingCart current, ShoppingCartEvent event) {
      return switch (event) {
        case ShoppingCartOpened shoppingCartOpened:
          yield new PendingShoppingCart(
            shoppingCartOpened.shoppingCartId(),
            shoppingCartOpened.clientId(),
            ProductItems.empty()
          );
        case ProductItemAddedToShoppingCart productItemAddedToShoppingCart:
          yield new PendingShoppingCart(
            current.id(),
            current.clientId(),
            current.productItems().add(productItemAddedToShoppingCart.productItem())
          );
        case ProductItemRemovedFromShoppingCart productItemRemovedFromShoppingCart:
          yield new PendingShoppingCart(
            current.id(),
            current.clientId(),
            current.productItems().remove(productItemRemovedFromShoppingCart.productItem())
          );
        case ShoppingCartConfirmed shoppingCartConfirmed:
          yield new ConfirmedShoppingCart(
            current.id(),
            current.clientId(),
            current.productItems(),
            shoppingCartConfirmed.confirmedAt()
          );
        case ShoppingCartCanceled shoppingCartCanceled:
          yield new CanceledShoppingCart(
            current.id(),
            current.clientId(),
            current.productItems(),
            shoppingCartCanceled.canceledAt()
          );
      };
    }

    static ShoppingCart empty() {
      return new PendingShoppingCart(null, null, null);
    }
  }

  public enum ShoppingCartStatus {
    Pending,
    Confirmed,
    Canceled
  }

  static EventStore.AppendResult appendEvents(PostgreSQLEventStore eventStore, StreamName streamName, Object[] events) {
    // 1. Add logic here
    return eventStore.appendToStream(streamName, Arrays.stream(events).toList());
  }

  static ShoppingCart getShoppingCart(PostgreSQLEventStore eventStore, StreamName streamName) {
    // 1. Add logic here
    return eventStore.aggregateStream(
      ShoppingCart::empty,
      ShoppingCart::evolve,
      streamName
    ).state();
  }

  @Test
  public void appendingEvents_forSequenceOfEvents_shouldSucceed() {
    var shoppingCartId = UUID.randomUUID();
    var clientId = UUID.randomUUID();
    var shoesId = UUID.randomUUID();
    var tShirtId = UUID.randomUUID();
    var twoPairsOfShoes = new PricedProductItem(shoesId, 2, 100);
    var pairOfShoes = new PricedProductItem(shoesId, 1, 100);
    var tShirt = new PricedProductItem(tShirtId, 1, 50);

    var events = new ShoppingCartEvent[]
      {
        new ShoppingCartOpened(shoppingCartId, clientId),
        new ProductItemAddedToShoppingCart(shoppingCartId, twoPairsOfShoes),
        new ProductItemAddedToShoppingCart(shoppingCartId, tShirt),
        new ProductItemRemovedFromShoppingCart(shoppingCartId, pairOfShoes),
        new ShoppingCartConfirmed(shoppingCartId, OffsetDateTime.now()),
        new ShoppingCartCanceled(shoppingCartId, OffsetDateTime.now())
      };

    var streamName = new StreamName("shopping_cart", shoppingCartId.toString());

    var eventStore = getPostgreSQLEventStore();

    appendEvents(eventStore, streamName, events);

    var shoppingCart = getShoppingCart(eventStore, streamName);

    assertEquals(shoppingCartId, shoppingCart.id());
    assertEquals(clientId, shoppingCart.clientId());
    assertEquals(2, shoppingCart.productItems().values().length);

    assertEquals(pairOfShoes, shoppingCart.productItems().values()[0]);
    assertEquals(tShirt, shoppingCart.productItems().values()[1]);
  }
}
