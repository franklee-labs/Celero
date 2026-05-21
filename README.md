# Celero

<img src="./assets/celero_blue.svg" alt="Celero logo" width="250">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Celero** is a lightweight, easy-to-use Java rule engine for defining and evaluating complex business rules via a fluent API or JSON configuration.

## Features

- **Flexible rule definition**: build rules programmatically or deserialize from JSON
- **Rich condition types**: equality, comparison, regex, collection membership, list intersection/disjointness, field existence, CEL expressions, and more
- **Logical operators**: AND / OR / NOT, arbitrarily nestable
- **Three-valued logic**: `TRUE` / `FALSE` / `INDETERMINATE` result states (`AdvancedCeleroEngine`)
- **Cross-path condition result cache**: a shared condition across multiple paths is evaluated only once (opt-in)
- **Condition priority**: control the execution order of conditions within a path via `priority`
- **Event listeners**: rule-level and condition-level callbacks with ordering support
- **Evaluation reports**: per-path record of matched, unmatched, absent, and skipped conditions
- **CEL expression support**: integrates [Google CEL](https://github.com/google/cel-spec) for advanced expression evaluation

---

## Installation

### Import from Maven

```xml
<dependency>
    <groupId>io.github.franklee-labs</groupId>
    <artifactId>celero</artifactId>
    <version>0.0.1-RELEASE</version>
</dependency>
```

### Build from source

```bash
git clone https://github.com/franklee-labs/celero.git
cd celero
mvn clean install
```

---

## Core Design

### Rule Tree → Path Expansion

A Celero rule is described as a **logic tree**: leaf nodes are `ConditionNode` instances (conditions), and internal nodes are `RelationNode` instances (AND / OR / NOT).

During `RuleBuilder.build()`, the engine **expands the logic tree once** into a flat set of `Path` objects (called a `PathGroup`). Each path is an ordered list of conditions; a path passes when every condition in it is satisfied.

Expansion rules:

| Expression | Expanded paths |
| --- | --- |
| `AND(A, B)` | Single path: `[A, B]` |
| `OR(A, B)` | Two paths: `[A]` and `[B]` |
| `AND(A, OR(B, C))` | Two paths: `[A, B]` and `[A, C]` |
| `NOT(AND(A, B))` | De Morgan expansion → `OR(NOT(A), NOT(B))` |

The engine iterates paths **in order**; the rule is true as soon as any path passes entirely. This design reduces the evaluation of complex logic to a simple sequential scan over flat lists.

### Three-Valued EvalResult

```java
public final class EvalResult {
    public static final EvalResult TRUE          = new EvalResult(State.TRUE);
    public static final EvalResult FALSE         = new EvalResult(State.FALSE);
    public static final EvalResult INDETERMINATE = new EvalResult(State.INDETERMINATE);
}
```

- **TRUE** — the condition/rule is definitively true
- **FALSE** — the condition/rule is definitively false
- **INDETERMINATE** — a required parameter is absent from the context; the outcome cannot be determined

`INDETERMINATE` is only available in `AdvancedCeleroEngine`. `DefaultCeleroEngine` treats missing parameters as `FALSE` and always returns a plain `boolean`.

### Two Engines

| | `DefaultCeleroEngine` | `AdvancedCeleroEngine` |
| --- | --- | --- |
| Return type | `boolean` | `EvalResult` |
| Missing parameter handling | Treated as `FALSE` | Returns `INDETERMINATE` |
| Event types | `ConditionEvent` / `RuleEvent` | `AdvancedConditionEvent` / `AdvancedRuleEvent` |

**INDETERMINATE propagation (`AdvancedCeleroEngine`)**:

Within a path, a `FALSE` result short-circuits immediately. If all conditions are evaluated without any `FALSE` but at least one is `INDETERMINATE`, the path result is `INDETERMINATE`.

At the rule level: if no path returns `TRUE` but at least one path returned `INDETERMINATE`, the rule returns `INDETERMINATE`; otherwise it returns `FALSE`.

```text
path1: [A=INDETERMINATE, B=TRUE]  → INDETERMINATE
path2: [A=TRUE, B=FALSE]           → FALSE (short-circuit)

rule → INDETERMINATE (no path is TRUE, but one path is uncertain)
```

### Cross-Path Condition Result Cache

After expansion, the same `ConditionNode` instance may appear in multiple paths. For example, `AND(A, OR(B, C))` expands to `[A, B]` and `[A, C]` — condition A appears in both paths.

Without caching, A is evaluated twice (especially costly for complex conditions such as regular expressions).

**With caching enabled**, the result of the first execution of A is written into the `Context`; subsequent paths read from the cache and skip re-evaluation.

Caching requires **both** switches to be enabled (independent, dual-gate design):

1. **Rule-level switch**: `RuleBuilder.cacheable(true)` — allows the rule to use caching at all
2. **Condition-level switch**: `"cacheable": true` on the condition node — allows this specific condition's result to be cached

```text
rule cacheable = false                                    →  no caching, regardless of condition setting
rule cacheable = true, condition cacheable = false        →  this condition is not cached
rule cacheable = true, condition cacheable = true         →  result is cached and reused across paths
```

Cache lifetime is **scoped to a single rule evaluation** (stored in `Context`); it never leaks across rules or requests.

> **Note**: `ConditionListener` is only fired when a condition is actually executed. A cache hit does not trigger the listener.

---

## Quick Start

### Building a Rule Programmatically

```java
import labs.franklee.celero.engine.*;
import labs.franklee.celero.rules.ConditionNode;
import labs.franklee.celero.rules.RuleBuilder;

ConditionNode statusCondition = new ConditionNode();
statusCondition.setId("cond-status");
statusCondition.setSign("EQ");
statusCondition.setProperties(Map.of(
    "field", "status",
    "value", "active",
    "valueType", "String"
));

CeleroRule rule = RuleBuilder.create()
    .id("rule-001")
    .name("Active User Check")
    .root(statusCondition)
    .build();

DefaultCeleroEngine engine = new DefaultCeleroEngine();
RuleContext context = RuleContext.of(Map.of("status", "active"));

boolean result = engine.evaluate(rule, context);  // true
```

### Building a Rule from JSON

`fromJson` accepts the rule id and the JSON of the logic tree root node. Condition properties are nested under a `"properties"` object. Fields such as `cacheable` and `ignoreAbsence` are **top-level** condition node fields, not inside `"properties"`.

```java
String ruleJson = """
{
  "type": "relation",
  "sign": "AND",
  "children": [
    {
      "id": "age-cond",
      "type": "condition",
      "sign": "GT",
      "properties": {
        "field": "age",
        "value": "18",
        "valueType": "Number"
      }
    }
  ]
}
""";

CeleroRule rule = RuleBuilder.fromJson("age-check", ruleJson)
    .name("Adult Verification")
    .build();
boolean result = engine.evaluate(rule, RuleContext.of(Map.of("age", 25L)));  // true
```

### Composing Logic

```java
// AND(A, OR(B, C))
RelationNode orNode = new RelationNode();
orNode.setSign("OR");
orNode.setChildren(List.of(condB, condC));

RelationNode andNode = new RelationNode();
andNode.setSign("AND");
andNode.setChildren(List.of(condA, orNode));

CeleroRule rule = RuleBuilder.create().id("rule").name("rule").root(andNode).build();
```

This expands to two paths: `[A, B]` and `[A, C]`.

### Enabling the Cache (Multi-Path Scenario)

```java
// Mark condition A as cacheable at the node level
ConditionNode condA = new ConditionNode();
condA.setId("cond-a").setSign("REGEXP").setCacheable(true);
condA.setProperties(Map.of("field", "email", "value", "^[\\w.+-]+@[\\w-]+\\.[a-z]{2,}$"));

// Also enable the rule-level cache switch
CeleroRule rule = RuleBuilder.create()
    .id("rule")
    .cacheable(true)   // rule-level switch
    .root(andNode)
    .build();

// AND(A, OR(B, C)) → path1=[A,B], path2=[A,C]
// path1: evaluate A (result cached), evaluate B → false
// path2: read A's cached result (not re-executed), evaluate C → true
DefaultCeleroEngine engine = new DefaultCeleroEngine();
engine.evaluate(rule, RuleContext.of(Map.of("email", "alice@example.com", "level", "high")));
```

In JSON, set `cacheable` at the condition node level:

```json
{
  "id": "cond-a",
  "type": "condition",
  "sign": "REGEXP",
  "cacheable": true,
  "properties": {
    "field": "email",
    "value": "^[\\w.+-]+@[\\w-]+\\.[a-z]{2,}$"
  }
}
```

---

## Using AdvancedCeleroEngine (Three-Valued Results)

```java
AdvancedCeleroEngine engine = new AdvancedCeleroEngine();

// All parameters present and matching → TRUE
EvalResult r1 = engine.evaluate(rule, RuleContext.of(Map.of("status", "active")));
r1.isTrue();          // true

// Parameters present but not matching → FALSE
EvalResult r2 = engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive")));
r2.isFalse();         // true

// Required parameter missing → INDETERMINATE (cannot determine)
EvalResult r3 = engine.evaluate(rule, RuleContext.of(Map.of()));
r3.isIndeterminate(); // true
```

A typical use case for `INDETERMINATE`: in progressive rule-matching scenarios where data arrives incrementally, it lets you distinguish "definitively does not match" from "data is missing, cannot yet decide" — preventing false negatives. For example, when a user is filling out a form, the engine can match only the fields already provided; fields not yet entered return `INDETERMINATE` rather than `FALSE`.

---

## Condition Reference

| Sign | Description | Properties |
| --- | --- | --- |
| `EQ` | Equal to | `field`, `value`, `valueType`: String / Number / Boolean / Expression |
| `NEQ` | Not equal to | `field`, `value`, `valueType`: String / Number / Boolean / Expression |
| `GT` | Greater than | `field`, `value`, `valueType`: Number / Expression |
| `GTE` | Greater than or equal | `field`, `value`, `valueType`: Number / Expression |
| `LT` | Less than | `field`, `value`, `valueType`: Number / Expression |
| `LTE` | Less than or equal | `field`, `value`, `valueType`: Number / Expression |
| `IN` | Value exists in collection | `field`, `value` (JSON array string), `valueType`: List |
| `NIN` | Value does not exist in collection | `field`, `value` (JSON array string), `valueType`: List |
| `REGEXP` | Regular expression match | `field`, `value` (regex pattern) |
| `CEL` | Google CEL expression | `expression` (CEL expression string) |
| `INTERSECT` | Two lists share at least one common element | `field1`, `valueType1`, `field2`, `valueType2`: List / Expression |
| `DISJOINT` | Two lists share no common elements | `field1`, `valueType1`, `field2`, `valueType2`: List / Expression |
| `EXISTS` | Field is present in the evaluation context | `field` (field name) |
| `ABSENT` | Field is absent from the evaluation context | `field` (field name) |

### Notes on specific conditions

**`INTERSECT` / `DISJOINT`**: when `valueType` is `List`, the field value is a JSON array literal (e.g., `"[\"a\",\"b\"]"`); when `valueType` is `Expression`, the field is a context variable name holding a list.

**`EXISTS` / `ABSENT`**: always return a definite `TRUE` or `FALSE` regardless of context mode. `ABSENT` is the logical negation of `EXISTS`. `ignoreAbsence` does not apply to these two conditions.

**Numeric types**: when the `value` string has no decimal part (e.g., `"18"`), the engine compiles it as `int64`; when it has a decimal part (e.g., `"18.5"`), it is compiled as `double`. The corresponding parameter in `RuleContext` must match — pass `long` for integer rules, `double` for decimal rules.

### CEL Expression Example

```java
ConditionNode cel = new ConditionNode();
cel.setId("cel-cond").setSign("CEL");
cel.setProperties(Map.of("expression", "age > 18 && status == 'active'"));
```

---

## Condition Priority

Within a path, a lower priority value means earlier execution (analogous to `ORDER BY priority ASC`):

```java
// Built-in constants
Priority.HIGHEST = Integer.MIN_VALUE  // executed first
Priority.DEFAULT = 0
Priority.LOWEST  = Integer.MAX_VALUE  // executed last
```

Set `priority` inside the `properties` object of a condition node:

```json
{
  "id": "cond-1",
  "type": "condition",
  "sign": "REGEXP",
  "properties": {
    "field": "email",
    "value": "^.+@.+$",
    "priority": 1
  }
}
```

---

## Ignoring Absence (`ignoreAbsence`)

By default, when a condition's required parameter is missing from the context:

- `DefaultCeleroEngine` — returns `FALSE`
- `AdvancedCeleroEngine` — returns `INDETERMINATE`

Setting `ignoreAbsence = true` on a condition overrides this: a missing parameter **always** returns `FALSE`, even in `AdvancedCeleroEngine`. This is useful for optional fields that should simply fail the condition rather than make the entire rule indeterminate.

`ignoreAbsence` is a **top-level field** on the condition node, not a property.

**Programmatic:**

```java
ConditionNode cond = new ConditionNode();
cond.setId("opt-cond").setSign("EQ");
cond.setIgnoreAbsence(true);   // top-level node field, not in properties
cond.setProperties(Map.of(
    "field", "optionalTag",
    "value", "vip",
    "valueType", "String"
));
```

**JSON:**

```json
{
  "id": "opt-cond",
  "type": "condition",
  "sign": "EQ",
  "ignoreAbsence": true,
  "properties": {
    "field": "optionalTag",
    "value": "vip",
    "valueType": "String"
  }
}
```

`ignoreAbsence` is supported by: `EQ`, `NEQ`, `GT`, `GTE`, `LT`, `LTE`, `IN`, `NIN`, `REGEXP`, `CEL`, `INTERSECT`, `DISJOINT`.
It is **not** applicable to `EXISTS` / `ABSENT`, since those conditions are specifically about field presence.

---

## Event Listeners

Listeners are registered before evaluation and called in ascending `order()` value (lower = earlier). Exceptions thrown inside a listener are silently swallowed and do not affect rule evaluation.

### DefaultCeleroEngine

```java
// Lambda form (ConditionListener and RuleListener are @FunctionalInterface)
engine.addConditionListener(event ->
    System.out.println("Condition " + event.getConditionName() + " matched: " + event.isMatched())
);

engine.addRuleListener(event ->
    System.out.println("Rule " + event.getRuleName() + " matched: " + event.isMatched())
);

// With explicit order
engine.addConditionListener(new ConditionListener() {
    @Override
    public void onResult(ConditionEvent event) {
        System.out.println("[order=10] " + event.getConditionName() + " → " + event.isMatched());
    }
    @Override
    public int order() { return 10; }
});
```

### AdvancedCeleroEngine

Use `AdvancedConditionListener` / `AdvancedRuleListener`; events carry an `EvalResult` (three-valued):

```java
engine.addConditionListener(event -> {
    EvalResult result = event.getResult();  // TRUE / FALSE / INDETERMINATE
    if (result.isIndeterminate()) {
        System.out.println("Missing field for: " + event.getConditionName());
    }
});

engine.addRuleListener(event -> {
    EvalResult result = event.isMatched();  // TRUE / FALSE / INDETERMINATE
    System.out.println("Rule " + event.getRuleName() + " → " + result);
});
```

### RuleContext Attributes — sharing state between listeners

Listeners can read and write arbitrary key-value attributes on the `RuleContext` to share state within a single evaluation:

```java
// A lower-order listener writes a value
engine.addConditionListener(new ConditionListener() {
    public void onResult(ConditionEvent event) {
        int count = (int) Optional.ofNullable(event.getContext().getAttribute("count")).orElse(0);
        event.getContext().setAttribute("count", count + 1);
    }
    public int order() { return 1; }
});

// A higher-order listener reads it
engine.addConditionListener(new ConditionListener() {
    public void onResult(ConditionEvent event) {
        System.out.println("Conditions evaluated so far: " + event.getContext().getAttribute("count"));
    }
    public int order() { return 10; }
});

// After evaluation, the caller can read attributes too
RuleContext ctx = RuleContext.of(params);
engine.evaluate(rules, ctx);
System.out.println("Total: " + ctx.getAttribute("count"));
```

> **Note**: keys in `params` must not start with `_`; the `_` prefix is reserved for engine-internal parameters.

---

## Evaluation Reports

When reports are enabled, each rule evaluation records the status of every condition on every path:

```java
RuleContext ctx = RuleContext.of(params).setEnableReports(true);
engine.evaluate(rule, ctx);

Report report = ctx.getReports().get(rule);
for (Route route : report.getRoutes()) {
    route.getMatched();    // conditions that evaluated to true
    route.getUnmatched();  // conditions that evaluated to false (caused path to fail)
    route.getAbsent();     // conditions with INDETERMINATE result (AdvancedCeleroEngine only)
    route.getSkipped();    // conditions not evaluated due to short-circuit
}
```

Each `Route` corresponds to one path evaluation attempt and contains `Route.Item` objects (conditionId + conditionName).

---

## Batch Evaluation

```java
List<CeleroRule> rules = List.of(rule1, rule2, rule3);
RuleContext ctx = RuleContext.of(params);

// DefaultCeleroEngine — fires RuleListener after each rule
engine.evaluate(rules, ctx);

// AdvancedCeleroEngine — same, but RuleListener receives EvalResult
advancedEngine.evaluate(rules, ctx);
```

> **Note**: the single-rule overload `engine.evaluate(rule, ctx)` does **not** fire `RuleListener`. Use the list overload when you need rule-level callbacks.

---

## Custom Condition Types

Register a custom factory globally (available to all rules) or scoped to a specific rule:

```java
// Implement ConditionFactory
public class StartsWithConditionFactory implements ConditionFactory {
    @Override
    public Condition create(ConditionNode node) {
        String field  = (String) node.getProperties().get("field");
        String prefix = (String) node.getProperties().get("value");
        return new StartsWithCondition(field, prefix);
    }
}

// Register once at startup — sign must not conflict with built-in signs
ConditionFactoryRegistry.registerGlobalFactory("STARTS_WITH", new StartsWithConditionFactory());

// Or scope it to a specific rule
ConditionFactoryRegistry.registerFactory("my-rule-id", "STARTS_WITH", new StartsWithConditionFactory());
```

Then use the sign in a rule definition like any built-in sign.

---

## Architecture Overview

```text
RuleBuilder
  └─ fromJson(String id, String json) / create()
       └─ build()
            ├─ RelationNode.transform()  → logic tree (AND / OR / NOT + Condition nodes)
            ├─ Node.validateAll()        → structural validation
            └─ Relation.resolve()        → expand into PathGroup (list of Paths)
                                             each Path sorted by condition priority

Evaluation:
  Engine.evaluate(rule, ruleContext)
    └─ iterate each Path in PathGroup
         └─ execute each Condition in order
              ├─ check cache (if enabled)
              ├─ Condition.execute(context)
              │    ├─ evaluate() succeeds → TRUE / FALSE
              │    └─ MissingParameterException
              │         ├─ ignoreAbsence=true          → FALSE
              │         ├─ DefaultCeleroEngine          → FALSE
              │         └─ AdvancedCeleroEngine         → INDETERMINATE
              └─ write to cache (if cacheable)
```

### Package Structure

```text
src/main/java/labs/franklee/celero/
├── engine/      DefaultCeleroEngine, AdvancedCeleroEngine, CeleroRule, RuleContext, Report, Route
├── rules/       RuleBuilder, Rule, ConditionNode, RelationNode, ConditionFactoryRegistry
├── logic/
│   ├── base/    Condition, Relation, EvalResult, Node, Priority, Validation
│   └── impl/    AND, OR, NOT, EqualCondition, CompareCondition, CelCondition, RegexCondition,
│                IntersectCondition, DisjointCondition, ExistsCondition, AbsentCondition ...
├── logic/path/  Path, PathGroup
├── listener/    RuleListener, ConditionListener, and Advanced variants
├── context/     Context (evaluation context, holds the condition result cache)
└── exceptions/  EvalException, InvalidConditionException, MissingParameterException ...
```

---

## Examples

Runnable examples are provided under `src/main/java/labs/franklee/celero/examples/`. Each class has a `main` method.

| Class                                    | Demonstrates |
|------------------------------------------| --- |
| `SimpleRuleEngineExample`                | Load rules from JSON, single-rule and batch evaluation |
| `SimpleRuleEngineListenersExample`       | `ConditionListener` / `RuleListener` with priority ordering and context attributes |
| `AdvancedRuleEngineExample`              | `AdvancedCeleroEngine` with TRUE / FALSE / INDETERMINATE results |
| `AdvancedRuleEngineListenersExample`            | `AdvancedConditionListener` / `AdvancedRuleListener` with context attributes |
| `AdvancedRuleEngineIgnoreAbsenceExample`        | Mixed `ignoreAbsence` settings within the same rule |
| `SimpleRuleEngineCachedConditionExample` | Condition result caching — A only evaluated once in `A AND (B OR C)` |

---

## Testing

```bash
mvn test
```

Test coverage includes:

- All condition types (equality, comparison, regex, CEL, list intersection/disjointness, field existence)
- AND / OR / NOT logic with arbitrary nesting
- Three-valued (INDETERMINATE) propagation and edge cases
- All cache switch combinations (rule-level × condition-level × cache hit/miss)
- Evaluation reports (matched / unmatched / absent / skipped)
- JSON rule parsing and programmatic rule building

---

## Contributing

Contributions are welcome!

1. Fork the repository
2. Create a branch (`git checkout -b feature/xxx`)
3. Commit your changes (`git commit -m 'Add xxx'`)
4. Push (`git push origin feature/xxx`)
5. Open a Pull Request

---

## License

[MIT](LICENSE)

---

Made with ❤️ by [franklee-labs](https://github.com/franklee-labs)
