---
name: spring-boot-4-enterprise
description: Enterprise-grade development guidelines for Java 25 and Spring Boot 4.1+. Optimized for maintainability, readability, AOT compatibility, GraalVM Native Images, cloud-native applications, and modern software architecture.
license: MIT

---

# Java 25 & Spring Boot 4 Enterprise Guidelines

> Generate production-ready, maintainable, idiomatic Java code following modern Spring Boot best practices.
>
> Prefer clarity over cleverness.
> Favor maintainability over micro-optimizations.
> Default to modern Java features instead of legacy idioms.
> When multiple valid approaches exist, choose the simplest solution that remains scalable.

---

# Core Philosophy

Every generated piece of code should be:

- Readable before being clever.
- Immutable whenever possible.
- Easy to test.
- Easy to maintain.
- Native-image friendly.
- Thread-safe whenever applicable.
- Explicit rather than implicit.
- Consistent with the existing codebase.

Code should look like it was written by an experienced senior backend engineer.

---

# Decision Priority

When multiple solutions are possible, prioritize in this order:

1. Correctness
2. Readability
3. Maintainability
4. Simplicity
5. Performance
6. Conciseness

Never sacrifice readability for fewer lines of code.

---

# Java Version

Target Java 25.
Always prefer Java 25 language features over legacy Java idioms whenever they improve readability or safety.
Avoid generating code compatible with Java 8 unless explicitly requested.

If virtual threads are needed, prefer Java 25 over Java 21 to avoid thread pinning on `synchronized` blocks.

---

# General Code Style

Prefer
- Small focused classes.
- Small focused methods.
- Explicit naming.
- Immutable data.
- Composition over inheritance.
- Constructor injection.
- Final variables whenever practical.
- Descriptive method names.
- Self-documenting code.

Avoid
- God classes.
- Utility classes containing unrelated methods.
- Deep inheritance trees.
- Static mutable state.
- Clever one-liners.
- Excessive comments explaining obvious code.

Comments should explain **why**, not **what**.

---

# Naming Conventions

Classes

Use nouns.

Examples

CustomerService

InvoiceRepository

OrderController

Method names

Use verbs.

calculatePrice()

findCustomer()

publishEvent()

Boolean methods

Use affirmative names.

isExpired()

hasPermission()

canDelete()

Avoid

check()

process()

handle()

doStuff()

foo()

bar()

---

# Immutability

Prefer immutable objects.

Use records whenever the object represents data.

Prefer immutable collections.

```java
List.copyOf(list)
Map.copyOf(map)
Set.copyOf(set)
```

Prefer

```java
List.of(...)
```

instead of

```java
new ArrayList<>()
```

when mutation is unnecessary.

---

# Local Variables

Use `var` only when the inferred type is obvious.

Good

```java
var customer = repository.findById(id);
```

Good

```java
var response = client.getCustomer(id);
```

Avoid

```java
var result = calculate(a, b, c);
```

where the type is unclear.

Readability always wins.

---

# Records

Prefer Java records for immutable data carriers.

Use records for

- DTOs
- API responses
- Request bodies
- Events
- Value objects without behavior

Example

```java
public record CustomerDto(
    UUID id,
    String name,
    String email
) {}
```

Avoid mutable DTOs.

Avoid Lombok @Data for DTOs.

Lombok is allowed when it improves readability without hiding important behavior.

Preferred

- @RequiredArgsConstructor
- @Slf4j
- @Builder for complex immutable objects
- @With for immutable models
- @Getter when records cannot be used

Avoid

- @Data on JPA entities
- @Setter on domain models
- @EqualsAndHashCode on JPA entities without careful consideration
- @ToString on entities with lazy relationships
- Excessive annotation stacking that obscures generated code

Prefer native Java language features when available.

Examples

Use records instead of Lombok DTOs whenever possible.

Use constructor injection via @RequiredArgsConstructor.

Use explicit code when generated behavior would be unclear.

Note: Lombok's generated code may not carry JSpecify null annotations through correctly.
Prefer records and explicit constructors for null-safe APIs.

---

# Sealed Classes

Prefer sealed classes or sealed interfaces for closed hierarchies.

Example

```java
public sealed interface PaymentResult
    permits Success, Failure {}
```

Use sealed hierarchies whenever all implementations are known.

---

# Pattern Matching

Prefer pattern matching over explicit casts.

Good

```java
if (object instanceof Customer customer) {
    ...
}
```

Prefer switch pattern matching whenever applicable.

```java
return switch (command) {

    case CreateOrder c -> ...

    case CancelOrder c -> ...

};
```

Switch expressions should be exhaustive.

Avoid traditional switch statements when an expression is sufficient.

---

# Optional

Use Optional only as a return type.

Good

```java
Optional<Customer> findById(...)
```

Avoid

```java
Optional<Customer> field;
```

Avoid

```java
void process(Optional<Customer>)
```

Never serialize Optional.

Never use Optional inside DTOs or JPA entities.

---

# Null Handling

Spring Boot 4 adopts JSpecify as the standard null-safety mechanism across the entire Spring portfolio.

Prefer JSpecify over `Objects.requireNonNull()` for API contracts.

Declare `@NullMarked` at the package level via `package-info.java`.

```java
@NullMarked
package com.example.myapp.orders;

import org.jspecify.annotations.NullMarked;
```

Inside a `@NullMarked` package, all types are non-null by default.
Mark exceptions explicitly with `@Nullable` from `org.jspecify.annotations`.

```java
@Service
public class OrderService {

    public Order createOrder(String email, @Nullable String promoCode) {
        ...
    }

    @Nullable
    public Customer findCustomer(UUID id) {
        return repository.findById(id).orElse(null);
    }
}
```

Use `@NullUnmarked` as a temporary escape hatch on legacy classes that cannot be fixed immediately.
Treat `@NullUnmarked` as technical debt with a deadline, not a permanent exception.

Enable NullAway in the build to turn annotation violations into compile errors.
NullAway requires at least JDK 21; use a Java 25 toolchain for best results.

Prefer empty collections over null collections.
Never return null collections.

Avoid `@Nullable` on fields injected by Spring — use constructor injection which guarantees non-null.

---

# Exceptions

Throw specific exceptions.

Prefer

CustomerNotFoundException

OrderAlreadyPaidException

ValidationException

Avoid

RuntimeException

Exception

Throwable

Never catch Exception unless implementing an application boundary.

Do not silently swallow exceptions.

---

# Checked vs Unchecked Exceptions

Business errors

→ unchecked exceptions

Infrastructure failures

→ wrap appropriately

Avoid excessive checked exceptions.

---

# Collections

Prefer interfaces.

```java
List<Customer>
```

instead of

```java
ArrayList<Customer>
```

Prefer immutable collections.

Avoid exposing mutable internal collections.

---

# Streams

Prefer Streams for transformations.

```java
customers.stream()
    .map(CustomerMapper::toDto)
    .toList();
```

Prefer

```java
toList()
```

instead of

```java
collect(Collectors.toList())
```

Avoid streams with heavy side effects.

Prefer classic loops when they are significantly easier to understand.

---

# Functional Style

Use functional programming where it improves readability.

Do not force functional style.

Avoid deeply nested stream pipelines.

---

# Date & Time

Use java.time.

Preferred

- Instant
- Duration
- Period
- LocalDate
- LocalDateTime
- OffsetDateTime
- ZonedDateTime

Avoid

Date

Calendar

Timestamp

Jackson 3 (mandatory in Spring Boot 4) serializes dates as ISO-8601 strings by default.

`WRITE_DATES_AS_TIMESTAMPS` is `false` by default.

Do not rely on Unix timestamp format in API responses or test assertions.

---

# Enums

Prefer enums instead of String constants.

Use switch expressions with enums.

Avoid ordinal().

Persist enum names instead of ordinals.

---

# Constants

Declare constants as

```java
private static final
```

Avoid magic numbers.

---

# Text Blocks

Prefer Java text blocks for multiline strings.

```java
String query = """
SELECT *
FROM customer
WHERE active = true
""";
```

---

# String Formatting

Prefer

```java
STR."Hello \{name}"
```

when appropriate.

Avoid string concatenation for complex formatting.

---

# Jackson 3

Spring Boot 4 mandates Jackson 3.

Use `JsonMapper` instead of `ObjectMapper`.

`JsonMapper` is immutable and provides format-specific mappers for JSON, XML, and CBOR.

```java
JsonMapper mapper = JsonMapper.builder().build();
```

Default behavior changes from Jackson 2:

- `WRITE_DATES_AS_TIMESTAMPS` → `false` (dates serialized as ISO-8601)
- `SORT_PROPERTIES_ALPHABETICALLY` → `true`

Audit all fields of type `LocalDate`, `LocalDateTime`, `ZonedDateTime`, `Instant`, and `OffsetDateTime`
in API responses when migrating from Spring Boot 3.

Do not assert on Unix timestamp format or on specific property ordering in snapshot tests.

Use the unified Jackson property namespaces introduced in Spring Boot 4.1:

```yaml
spring:
  jackson:
    read:
      default-view-inclusion: true
    write:
      indent-output: false
```

`spring.jackson.read.*` and `spring.jackson.write.*` apply consistently across all auto-configured
mappers (JSON, XML, CBOR). Prefer these properties over `JacksonCustomizer` beans for
cross-format configuration.

---

# API Versioning

Spring Boot 4 provides first-class API versioning via Spring Framework 7.

Declare versions directly on request mappings.

```java
@GetMapping(url = "/accounts/{id}", version = "1.1")
public AccountDto getAccount(@PathVariable UUID id) { ... }
```

Configure the versioning strategy once.

```java
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useRequestHeader("API-Version");
    }
}
```

Handle deprecation with `StandardApiVersionDeprecationHandler`.

```java
StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
handler.addDeprecation("1.0",
    ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
    URI.create("https://docs.example.com/api/migration"));
configurer.setVersionDeprecationHandler(handler);
```

Deprecated versions automatically emit RFC 9745-compliant `Deprecation` and `Sunset` headers.

Avoid stringly-typed version strings scattered across controllers.
Centralize versioning configuration.

---

# Null Safety

See [Null Handling](#null-handling) for the full JSpecify guidance.

Replace all usages of Spring's own `@Nullable` and `@NonNull` annotations
(`org.springframework.lang`) with their JSpecify equivalents
(`org.jspecify.annotations`).

Spring's own annotations are deprecated in Spring Boot 4.

---

# Resilience

Spring Framework 7 integrates retry and concurrency control into the core context.
No separate `spring-retry` dependency or `@EnableRetry` is needed.

Use `@Retryable` for declarative retry with exponential back-off.

```java
@Service
public class PaymentService {

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PaymentResult charge(PaymentRequest request) { ... }
}
```

Use `@ConcurrencyLimit` to protect resources under high concurrency, especially with virtual threads.

```java
@ConcurrencyLimit(10)
public Report generateReport(ReportRequest request) { ... }
```

---

# gRPC

Spring Boot 4.1 provides first-class gRPC support via three dedicated modules:
`spring-boot-grpc-server`, `spring-boot-grpc-client`, `spring-boot-grpc-test`.

Prefer Spring gRPC over manual wiring or third-party starters.

Use `@GrpcAdvice` for centralized gRPC exception handling, mirroring `@ControllerAdvice` for REST.

```java
@GrpcAdvice
public class GrpcExceptionHandler {

    @GrpcExceptionHandler(ResourceNotFoundException.class)
    public StatusException handleNotFound(ResourceNotFoundException ex) {
        return Status.NOT_FOUND.withDescription(ex.getMessage()).asException();
    }
}
```

The `ObservationGrpcServerInterceptor` is auto-configured and supports custom server-side
observation conventions for metrics and tracing without extra wiring.

Use Servlet integration to expose gRPC over HTTP/2 on the same port as REST when operating
behind load balancers, sidecar proxies, or Kubernetes services.

---

# Security

## SSRF Mitigation

Spring Boot 4.1 provides `InetAddressFilter` for server-side request forgery protection.

Always configure an `InetAddressFilter` when the application makes outbound HTTP calls
based on user-supplied or externally-sourced URLs.

```java
@Bean
public InetAddressFilter ssrfProtectionFilter() {
    return InetAddressFilter.builder()
        .denyPrivateAddresses()
        .denyLinkLocalAddresses()
        .build();
}
```

The filter is auto-wired into both the reactive `WebClient` and the blocking `RestClient`
when detected as a bean.

At minimum, block RFC 1918 private ranges (`10.x`, `172.16–31.x`, `192.168.x`) and
link-local addresses (`169.254.x`).

---

# HTTP Clients

## Declarative HTTP Interface Clients

Spring Framework 7 / Spring Boot 4 introduces first-class declarative HTTP clients.
This is the recommended approach for consuming external REST APIs.
It replaces `RestTemplate`, verbose `RestClient` wiring, and third-party libraries like OpenFeign.

Define an interface annotated with `@HttpExchange` (and its method-level variants).
Spring generates the runtime proxy automatically — no implementation code needed.

```java
@HttpExchange("/users")
public interface UserClient {

    @GetExchange("/{id}")
    UserDto findById(@PathVariable UUID id);

    @PostExchange
    UserDto create(@RequestBody CreateUserRequest request);

    @GetExchange
    List<UserDto> findAll(@RequestParam String type);

    @DeleteExchange("/{id}")
    void deleteById(@PathVariable UUID id);
}
```

Use `@GetExchange`, `@PostExchange`, `@PutExchange`, `@PatchExchange`, `@DeleteExchange`
instead of `@HttpExchange` with an explicit method when clarity matters.

### Registration with @ImportHttpServices

Register HTTP interface proxies as beans via `@ImportHttpServices`.
Do not manually create `HttpServiceProxyFactory` or individual `@Bean` methods for each interface.

```java
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "users", types = UserClient.class)
public class HttpClientConfig {}
```

Group interfaces that share the same base URL and HTTP client configuration.
Use `basePackages` for scanning when the group has many interfaces.

```java
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "users", basePackages = "com.example.client.users")
@ImportHttpServices(group = "payments", basePackages = "com.example.client.payments")
public class HttpClientConfig {}
```

### Configuration via application.properties

Configure each group's base URL, timeouts, and headers in `application.properties` or `application.yml`.
Avoid hard-coding base URLs in annotations or `@Bean` methods.

```yaml
spring:
  http:
    client:
      service:
        read-timeout: 5s            # applies to all groups
        group:
          users:
            base-url: https://user-service.internal
          payments:
            base-url: https://payment-service.internal
```

Available per-group properties: `base-url`, default headers, API versioning configuration,
redirect settings, connection and read timeouts, SSL bundles.

### Programmatic group configuration

Use `RestClientHttpServiceGroupConfigurer` for cross-cutting concerns that cannot be expressed
in properties (custom interceptors, authentication, dynamic headers).

```java
@Bean
RestClientHttpServiceGroupConfigurer groupConfigurer() {
    return groups -> {
        groups.filterByName("payments")
              .forEachClient((group, builder) -> builder
                  .defaultHeader("X-Api-Key", paymentApiKey)
                  .defaultStatusHandler(
                      HttpStatusCode::is4xxClientError,
                      (req, resp) -> { throw new PaymentClientException(resp.getStatusCode()); }
                  ));
    };
}
```

### Injection

Inject the interface directly. The proxy is a regular Spring bean.

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserClient userClient;

    public OrderDto createOrder(UUID userId, CreateOrderRequest request) {
        var user = userClient.findById(userId);
        ...
    }
}
```

If the same interface type appears in multiple groups, resolve it via `HttpServiceProxyRegistry`
instead of autowiring by type.

```java
@Service
public class MultiTenantService {

    private final UserClient tenantAClient;
    private final UserClient tenantBClient;

    public MultiTenantService(HttpServiceProxyRegistry registry) {
        this.tenantAClient = registry.getClient("tenant-a", UserClient.class);
        this.tenantBClient = registry.getClient("tenant-b", UserClient.class);
    }
}
```

### Testing

Use `@RestClientTest` to test HTTP interface clients in isolation.
The mock server replaces the real HTTP call — no Spring context overhead.

### When NOT to use HTTP Interface Clients

Prefer direct `WebClient` or `RestClient` for:

- Streaming responses (Server-Sent Events, large file downloads).
- Dynamic base URLs that change per-request (e.g. multi-tenant routing by subdomain).
- Complex per-call retry logic requiring Resilience4j circuit breakers.

### Migrating from OpenFeign

`@FeignClient(name = "users", url = "...")` → `@HttpExchange` on the interface
+ `@ImportHttpServices(group = "users", ...)` in configuration.

Spring Cloud OpenFeign still works but the Spring team recommends migrating to
`@HttpExchange` for new projects. The native solution has no external dependency
and integrates better with Spring Security 7 OAuth and Spring Boot 4 observability.

---

## Imperative HTTP Clients

Prefer `RestClient` (blocking) or `WebClient` (reactive) over `RestTemplate` for
imperative usage where declarative clients are not appropriate.

`RestTemplate` is deprecated.

Do not configure `proxyWithSystemProperties()` on `ReactorClientHttpRequestFactoryBuilder`
or `ReactorClientHttpConnectorBuilder` unless explicitly required.
This is no longer the default as of Spring Boot 4.1.

Apply `InetAddressFilter` (see [Security](#security)) to all outbound HTTP clients
when the target URL originates from untrusted input.

---

# Observability

## OpenTelemetry

Spring Boot 4.1 delivers the most comprehensive OpenTelemetry integration to date.

Use `management.opentelemetry.enabled=false` in integration tests where trace context must
flow but actual telemetry must not be exported.

Configure the full OTLP pipeline via properties.

```yaml
management:
  opentelemetry:
    enabled: true
    tracing:
      sampler: parentbased_always_on
      limits:
        max-number-of-attributes: 128
        max-number-of-events: 128
    logging:
      limits:
        max-number-of-attributes: 64
```

Standard OpenTelemetry SDK environment variables (`OTEL_SERVICE_NAME`,
`OTEL_EXPORTER_OTLP_ENDPOINT`, etc.) are now automatically mapped to Spring Boot
configuration properties. Do not duplicate them as explicit properties when they are
already provided by the platform.

Signal-specific variants (e.g. `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`) take precedence
over general variants (`OTEL_EXPORTER_OTLP_ENDPOINT`).

Prefer the Micrometer Observation or Tracing API over the OpenTelemetry API directly.

## @Async Context Propagation

As of Spring Boot 4.1, the trace and security context is automatically propagated to
methods running on a separate thread via `@Async`.

Do not manually thread context through `ThreadLocal` or `MDC` for standard observability needs.

## Custom Observation Conventions

`KafkaListenerObservationConvention`, `KafkaTemplateObservationConvention`,
`RabbitListenerObservationConvention`, and `RabbitStreamListenerObservationConvention`
beans are auto-applied to their respective components.

Declare a custom convention bean to override the default naming or tagging without
touching container configuration.

---

# Data Access

## Lazy JDBC Connection Fetching

Prefer lazy datasource connections in applications that do not always execute SQL
on every request (e.g. applications with heavy in-memory caching or gRPC endpoints
that bypass the database on cache hits).

```yaml
spring:
  datasource:
    connection-fetch: lazy
```

This wraps the pooled `DataSource` with `LazyConnectionDataSourceProxy`, deferring
the physical connection acquisition until a JDBC statement is actually executed.
This reduces connection pool pressure and improves startup time.

## JPA

Avoid `@Data` on JPA entities.

Avoid `@ToString` on entities with lazy relationships.

Define equals/hashCode based on a natural business key or a surrogate key, not on all fields.

Use schema migration tools.

Prefer Flyway or Liquibase.

## jOOQ

jOOQ 3.20+ (bundled with Spring Boot 4.1) requires Java 21 or later.

Use type-safe property paths for entity references when using Spring Data.

---

# Redis

Spring Boot 4.1 auto-configures `@RedisListener` endpoints.

If the application does not define a `RedisMessageListenerContainer`, a default container
is registered automatically.

Use `spring.data.redis.listener.*` to configure the default container.

Use `RedisMessageListenerContainerConfigurer` when multiple containers are needed.

---

# Virtual Threads

Prefer virtual threads for blocking I/O workloads.

Examples

- HTTP calls
- JDBC
- File I/O

Avoid virtual threads for CPU-bound parallel computations.

Prefer Java 25 to avoid thread pinning issues on `synchronized` blocks present in
some third-party libraries.

---

# Concurrency

Prefer structured concurrency when available.

Use `@ConcurrencyLimit` for declarative concurrency control on methods exposed
to high load, especially when backed by virtual threads.

Avoid manually managing thread pools unless required.

Never share mutable state across threads without synchronization.

---

# AOT & GraalVM Native Image

Avoid manual reflection APIs.

Avoid

Class.forName()

Method.invoke()

Field.set()

Prefer compile-time mechanisms.

If reflection is unavoidable, ensure compatibility with Spring AOT and GraalVM `RuntimeHints`.

Do not pass `-DskipTests` to Maven to skip AOT test processing.
AOT processing of tests is a separate step as of Spring Boot 4.1.

Spring Boot 4.1 is fully tested with Java 25 for AOT compilation and GraalVM native images.
Target Java 25 for native image builds.

---

# Modularity

Spring Boot 4 splits the monolithic `spring-boot-autoconfigure` JAR into 70+ focused modules.

Declare only the starters your application actually uses.

Do not import `spring-boot-autoconfigure` directly — use the technology-specific starters
(e.g. `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`).

This reduces the application footprint and prevents IDE auto-complete from surfacing
unused configuration classes.

---

# Serialization

Prefer `JsonMapper` (Jackson 3) over `ObjectMapper`.

Prefer Jackson support for records.

Avoid custom serializers unless required.

Generated models should remain serialization-friendly.

Never serialize Optional.

---

# Equality

Use record-generated equals/hashCode whenever possible.

For entities, equality should follow the project's persistence strategy.

Do not generate equals/hashCode blindly on mutable entities.

---

# Logging

Use SLF4J.

Never use

System.out.println()

Log meaningful events.

Avoid excessive debug logging.

Never log secrets.

Never log passwords.

Never log tokens.

Never log API keys.

For Log4j users, Spring Boot 4.1 provides native log rotation strategies (size, time,
size-and-time, cron) without requiring custom XML configuration.

---

# Testing

JUnit 4 is removed. Use JUnit 5 exclusively.

Use `@SpringBootTest` for integration tests.

Use slice tests (`@WebMvcTest`, `@DataJpaTest`, etc.) to keep tests fast and focused.

Use `management.opentelemetry.enabled=false` in test profiles to disable telemetry export
while preserving context propagation.

Do not skip AOT processing for tests via `-DskipTests` — use the dedicated Maven goal.

---

# Deprecated / Removed in Spring Boot 4.x

Avoid these — they no longer exist.

- `ObjectMapper` as primary bean → use `JsonMapper`
- Spring's `@Nullable` / `@NonNull` → use JSpecify equivalents
- `RestTemplate` → use `RestClient` or `WebClient`
- `AntPathMatcher` for HTTP request mappings → use `PathPattern`
- `javax.annotation` / `javax.inject` → use `jakarta.annotation` / `jakarta.inject`
- JUnit 4 → use JUnit 5
- `@EnableRetry` / separate spring-retry dependency → use `@Retryable` from Spring core
- Layertools jar mode → use `tools` mode
- Derby embedded database → migrate to H2 or HSQL