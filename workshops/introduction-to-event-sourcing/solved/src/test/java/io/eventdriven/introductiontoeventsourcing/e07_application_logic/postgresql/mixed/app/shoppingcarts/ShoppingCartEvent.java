package io.eventdriven.introductiontoeventsourcing.e07_application_logic.postgresql.mixed.app.shoppingcarts;

import java.time.OffsetDateTime;
import java.util.UUID;

import static io.eventdriven.introductiontoeventsourcing.e07_application_logic.postgresql.mixed.app.shoppingcarts.productItems.ProductItems.PricedProductItem;

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
