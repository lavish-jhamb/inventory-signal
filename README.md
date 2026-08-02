# inventory-signal

Polls product pages for stock availability at chosen pincodes and logs a notification when a
tracked product comes back in stock.

Currently supports **Croma** only. Designed so **Amazon**/**Flipkart** support can be added later
without touching the orchestration logic.

## Running

```
./gradlew bootRun
```

By default the first check runs ~30s after startup, then repeats every ~60s (see
`app.scheduler.*` in [application.yaml](src/main/resources/application.yaml)). When a tracked
product transitions from out-of-stock to in-stock at a pincode, a message is logged to the
console, e.g.:

```
Croma Stock ALERT
Product : iPhone 17e 512GB Black
Pincode : 400049
Buy now : https://www.croma.com/apple-iphone-17e-512gb-black-/p/317577
```

## Configuring products and pincodes

Edit [src/main/resources/products.yaml](src/main/resources/products.yaml):

```yaml
app:
  tracking:
    pincodes:
      - "400049"
      - "110001"

    products:
      - site: croma
        itemId: "317577"          # the numeric code in the product's PDP URL, e.g. /p/317577
        name: "iPhone 17e 512GB Black"
        url: "https://www.croma.com/apple-iphone-17e-512gb-black-/p/317577"  # shown as the buy link in notifications
        # pincodes:               # optional - overrides the global list above for this product
        #   - "560001"
```

- `pincodes` (top-level) is the default list checked for every product.
- Each product can optionally set its own `pincodes` list to override the default.

## Project structure

```
com.notify.inventory.signal
├── InventorySignalApplication   entry point (@EnableScheduling, @ConfigurationPropertiesScan)
├── tracking/                    config model (TrackedProduct, TrackingProperties)
├── provider/                    StockProvider interface + StockCheckResult
│   └── croma/                   CromaStockProvider (calls Croma's inventory API)
├── notification/                Notifier interface + ConsoleNotifier
└── service/                     StockCheckService (scheduled orchestration)
```

## Extending to a new site (e.g. Amazon, Flipkart)

1. Implement `StockProvider`, e.g. `provider/amazon/AmazonStockProvider.java`, and annotate it
   `@Component`. Return a unique `siteName()` (e.g. `"amazon"`).
2. Add products to `products.yaml` with `site: amazon` and whatever ID your provider needs as
   `itemId`.

`StockCheckService` looks providers up by `siteName()` automatically — no other code changes
needed.

## Adding a real notification channel (e.g. Telegram)

1. Implement `Notifier`, e.g. `notification/TelegramNotifier.java`, and annotate it `@Component`.
2. If you want to keep both `ConsoleNotifier` and a new notifier active, either make
   `StockCheckService` accept `List<Notifier>` and call each one, or mark the one you want
   disabled with `@ConditionalOnProperty`/remove `@Component` from it.

`telegram.*` settings already exist in [application.yaml](src/main/resources/application.yaml)
(bot token, chat id) ready for a `TelegramNotifier` to read via `@ConfigurationProperties` or
`@Value`.

## Tests

```
./gradlew test
```

Covers config binding, Croma response parsing (in-stock/out-of-stock/unrecognized shapes), and
the scheduled service's transition-detection and error-isolation logic.
