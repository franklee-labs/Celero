# Celero

<div align="center">
    <img src="./assets/celero_blue.svg" width="200" />
</div>

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Celero** is a lightweight, easy-to-use Java rule engine for defining and evaluating complex business rules via a fluent API or JSON configuration.

## Features

- **Flexible rule definition**: build rules programmatically with a fluent API or deserialize from JSON
- **Rich condition types**: equality, comparison, regex, collection membership, CEL expressions, and more
- **Logical operators**: AND / OR / NOT, arbitrarily nestable
- **Three-valued logic**: `TRUE` / `FALSE` / `INDETERMINATE` result states (`AdvancedCeleroEngine`)
- **Cross-path condition result cache**: a shared condition across multiple paths is evaluated only once (opt-in)
- **Condition priority**: control the execution order of conditions within a path via `priority`
- **Event listeners**: rule-level and condition-level callbacks
- **Evaluation reports**: per-path record of matched, unmatched, absent, and skipped conditions
- **CEL expression support**: integrates [Google CEL](https://github.com/google/cel-spec) for advanced expression evaluation

---

## Installation

```bash
git clone https://github.com/franklee-labs/Celero.git
cd Celero
mvn clean install
```

---

## Core Design

### Rule Tree → Path Expansion

A Celero rule is described as a **logic tree**: leaf nodes are `ConditionNode` instances (conditions), and internal nodes are `RelationNode` instances (AND / OR / NOT).

During `RuleBuilder.build()`, the engine **expands the logic tree once** into a flat set of `Path` objects (called a `PathGroup`). Each path is an ordered list of conditions; a path passes when every condition in it is satisfied.

Expansion rules:

| Expression | Expanded paths |
|---|---|
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
|---|---|---|
| Return type | `boolean` | `EvalResult` |
| Missing parameter handling | Treated as `FALSE` | Returns `INDETERMINATE` |
| Event types | `ConditionEvent` / `RuleEvent` | `AdvancedConditionEvent` / `AdvancedRuleEvent` |

**INDETERMINATE propagation (`AdvancedCeleroEngine`)**:

Within a path, a `FALSE` result short-circuits immediately. If all conditions are evaluated without any `FALSE` but at least one is `INDETERMINATE`, the path result is `INDETERMINATE`.

At the rule level: if no path returns `TRUE` but at least one path returned `INDETERMINATE`, the rule returns `INDETERMINATE`; otherwise it returns `FALSE`.

```
path1: [A=INDETERMINATE, B=TRUE]  → INDETERMINATE
path2: [A=TRUE, B=FALSE]           → FALSE (short-circuit)

rule → INDETERMINATE (no path is TRUE, but one path is uncertain)
```

### Cross-Path Condition Result Cache

After expansion, the same `ConditionNode` instance may appear in multiple paths. For example, `AND(A, OR(B, C))` expands to `[A, B]` and `[A, C]` — condition A appears in both paths.

Without caching, A is evaluated twice (especially costly for complex reg expression conditions).

**With caching enabled**, the result of the first execution of A is written into the `Context`; subsequent paths read from the cache and skip re-evaluation.

Caching requires **both** switches to be enabled (independent, dual-gate design):

1. **Rule-level switch**: `RuleBuilder.cacheable(true)` — allows the rule to use caching at all
2. **Condition-level switch**: `ConditionNode.setCacheable(true)` — allows this specific condition's result to be cached

```
rule cacheable = false                                    →  no caching, regardless of condition setting
rule cacheable = true, condition cacheable = false        →  this condition is not cached
rule cacheable = true, condition cacheable = true         →  result is cached and reused across paths
```

Cache lifetime is **scoped to a single rule evaluation** (stored in `Context`); it never leaks across rules or requests.

---

## Quick Start

### Building a Rule Programmatically

```java
import labs.franklee.celero.engine.*;
import labs.franklee.celero.rules.ConditionNode;
import labs.franklee.celero.rules.Rule;
import labs.franklee.celero.rules.RuleBuilder;

// Define a condition node
ConditionNode statusCondition = new ConditionNode();
statusCondition.

        setId("cond-status");
statusCondition.

        setSign("EQ");
statusCondition.

        setProperties(Map.of(
                "field", "status",
                              "value","active",
                              "valueType","String"
        ));

        // Build the rule
        Rule rule = RuleBuilder.create()
                .id("rule-001")
                .name("Active User Check")
                .root(statusCondition)
                .build();

        // Evaluate
        DefaultCeleroEngine engine = new DefaultCeleroEngine();
        RuleContext context = RuleContext.of(Map.of("status", "active"));

        boolean result = engine.evaluate(rule, context);  // true
```

### Building a Rule from JSON

```java
String ruleJson = """
{
  "id": "age-check",
  "name": "Adult Verification",
  "root": {
    "type": "relation",
    "sign": "AND",
    "children": [
      {
        "id": "age-cond",
        "type": "condition",
        "sign": "GT",
        "field": "age",
        "value": "18",
        "valueType": "Number"
      }
    ]
  }
}
""";

Rule rule = RuleBuilder.fromJson(ruleJson).build();
boolean result = engine.evaluate(rule, RuleContext.of(Map.of("age", 25)));  // true
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

Rule rule = RuleBuilder.create().id("rule").name("rule").root(andNode).build();
```

This expands to two paths: `[A, B]` and `[A, C]`.

### Enabling the Cache (Multi-Path Scenario)

```java
// Mark condition A as cacheable
ConditionNode condA = new ConditionNode();
condA.setId("cond-a").setSign("EQ").setCacheable(true);
condA.setProperties(Map.of("field", "role", "value", "admin", "valueType", "String"));

// Also enable the rule-level cache switch
Rule rule = RuleBuilder.create()
    .id("rule")
    .name("rule")
    .cacheable(true)   // rule-level switch
    .root(andNode)
    .build();

// AND(A, OR(B, C)) → path1=[A,B], path2=[A,C]
// path1: evaluate A (result cached), evaluate B
// path2: read A's cached result, evaluate C
// A is executed only once
DefaultCeleroEngine engine = new DefaultCeleroEngine();
engine.evaluate(rule, RuleContext.of(Map.of("role", "admin")));
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

| Sign    | Description                        | valueType                               |
|---------|------------------------------------|-----------------------------------------|
| `EQ`    | Equal to                           | String / Number / Boolean /  expression |
| `NEQ`   | Not equal to                       | String / Number / Boolean /  expression |
| `GT`    | Greater than                       | Number /  expression                    |
| `GTE`   | Greater than or equal              | Number /  expression                    |
| `LT`    | Less than                          | Number /  expression                    |
| `LTE`   | Less than or equal                 | Number /  expression                    |
| `IN`    | Value exists in collection         | Mixed String / Number / Boolean         |
| `NIN`   | Value does not exist in collection | Mixed String / Number / Boolean         |
| `REGEX` | Regular expression match           | regex expression                        |
| `CEL`   | Google CEL expression              | expression                              |

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

When configuring rules via JSON, set the `priority` field on a `ConditionNode`.  
When creating conditions via `ConditionFactory`, pass `priority` (int) in the properties map.

---

## Event Listeners

### DefaultCeleroEngine

```java
engine.addConditionListener(new ConditionListener() {
    @Override
    public void onResult(ConditionEvent event) {
        System.out.println("Condition: " + event.getConditionId()
            + " Result: " + event.isResult());
    }

    @Override
    public int order() { return 0; }
});

engine.addRuleListener(new RuleListener() {
    @Override
    public void onRuleResult(RuleEvent event) {
        System.out.println("Rule: " + event.getRuleName()
            + " Result: " + event.isResult());
    }

    @Override
    public int order() { return 0; }
});
```

### AdvancedCeleroEngine

Use `AdvancedConditionListener` / `AdvancedRuleListener`; events carry an `EvalResult` (three-valued):

```java
engine.addConditionListener(new AdvancedConditionListener() {
    @Override
    public void onResult(AdvancedConditionEvent event) {
        EvalResult result = event.getResult();  // TRUE / FALSE / INDETERMINATE
    }

    @Override
    public int order() { return 0; }
});
```

Multiple listeners execute in ascending `order()` (lower value = earlier). Exceptions thrown inside a listener are silently swallowed and do not affect rule evaluation.

---

## Evaluation Reports

When reports are enabled, each rule evaluation records the status of every condition on every path:

```java
RuleContext ctx = RuleContext.of(params).setEnableReports(true);
engine.evaluate(rule, ctx);

Map<Rule, Report> reports = ctx.getReports();
Report report = reports.get(rule);
for (Route route : report.getRoutes()) {
    route.getMatched();    // conditions that passed
    route.getUnmatched();  // conditions that failed
    route.getAbsent();     // conditions that returned INDETERMINATE (AdvancedCeleroEngine only)
    route.getSkipped();    // conditions skipped due to short-circuit
}
```

Each `Route` corresponds to one path evaluation and contains `Route.Item` objects (conditionId + conditionName).

---

## RuleContext Attributes

In addition to rule parameters (`params`), `RuleContext` supports arbitrary key-value attributes that can be read inside listeners:

```java
RuleContext ctx = RuleContext.of(params)
    .setAttribute("requestId", "abc-123")
    .setAttribute("source", "api");

// Read inside a listener
String requestId = (String) event.getContext().getAttribute("requestId");
```

> **Note**: keys in `params` must not start with `_`; the `_` prefix is reserved for engine-internal parameters.

---

## Batch Evaluation

```java
List<Rule> rules = List.of(rule1, rule2, rule3);
RuleContext ctx = RuleContext.of(params);

// DefaultCeleroEngine — fires RuleListener after each rule
engine.evaluate(rules, ctx);

// AdvancedCeleroEngine — same, but RuleListener receives EvalResult
advancedEngine.evaluate(rules, ctx);
```

---

## Architecture Overview

```
RuleBuilder
  └─ fromJson(String) / create()
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
              │         ├─ DefaultCeleroEngine  → FALSE
              │         └─ AdvancedCeleroEngine → INDETERMINATE
              └─ write to cache (if enabled)
```

### Package Structure

```
src/main/java/labs/franklee/celero/
├── engine/      DefaultCeleroEngine, AdvancedCeleroEngine, RuleContext, Report, Route
├── rules/       RuleBuilder, Rule, ConditionNode, RelationNode, ConditionFactoryRegistry
├── logic/
│   ├── base/    Condition, Relation, EvalResult, Node, Priority, Validation
│   └── impl/    AND, OR, NOT, EqualCondition, CelCondition, RegexCondition ...
├── logic/path/  Path, PathGroup
├── listener/    RuleListener, ConditionListener, and Advanced variants
├── context/     Context (evaluation context, holds the condition result cache)
└── exceptions/  EvalException, InvalidConditionException, MissingParameterException ...
```

---

## Testing

```bash
mvn test
```

Test coverage includes:
- All condition types (equality, comparison, regex, CEL)
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
