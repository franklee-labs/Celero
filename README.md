# Celero

<div align="center">
    <img src="./assets/celero_blue.svg" width="200" />
</div>

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-25-blue)](https://www.oracle.com/java/)
[![Maven Central](https://img.shields.io/badge/Maven-1.0.0-blue)](https://mvnrepository.com/artifact/labs.franklee/celero)

**Celero** is a lightweight, easy-to-use rule engine for Java. It allows you to define, build, and evaluate complex business rules using a fluent API or JSON configuration, with support for logical operators (AND, OR, NOT), multiple condition types, and comprehensive event listeners.

## 🎯 Features

- **Flexible Rule Definition**: Build rules programmatically or from JSON
- **Rich Condition Types**: Support for equality, comparison, regex, range checks, and CEL expressions
- **Logical Operators**: Combine conditions with AND, OR, NOT operators with configurable precedence
- **Expression Language Support**: Integrate [Google CEL](https://github.com/google/cel-spec) for advanced expression evaluation
- **Event Listeners**: Track rule and condition evaluation with customizable listeners and reporting
- **Performance Optimization**: Built-in caching and short-circuit evaluation
- **Path-Based Evaluation**: Evaluate multiple logical paths through conditions
- **Comprehensive Exception Handling**: Detailed error reporting with custom exception types
- **Full Test Coverage**: Extensive test suite with 20+ unit tests

## 📦 Installation

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>labs.franklee</groupId>
    <artifactId>celero</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Build from Source

```bash
# Clone the repository
git clone https://github.com/franklee-labs/Celero.git
cd Celero

# Build with Maven
mvn clean install

# Run tests
mvn test
```

## 🚀 Quick Start

### Programmatic Rule Building

```java
import labs.franklee.celero.rules.*;
import labs.franklee.celero.engine.*;

// Create condition nodes
ConditionNode statusCondition = new ConditionNode();
statusCondition.setId("cond-status");
statusCondition.setSign("EQ");
statusCondition.setProperties(Map.of(
    "key", "status",
    "value", "active",
    "valueType", "String"
));

// Build the rule
Rule rule = RuleBuilder.create()
    .id("rule-001")
    .name("Active User Check")
    .description("Check if user status is active")
    .root(statusCondition)
    .build();

// Evaluate the rule
DefaultCeleroEngine engine = new DefaultCeleroEngine();
RuleContext context = new RuleContext();
context.setRuleContext(Map.of("status", "active"));

boolean result = engine.evaluate(rule, context);  // returns true
```

### JSON-Based Rule Definition

```java
String ruleJson = """
{
  "id": "age-check",
  "name": "Adult Verification",
  "description": "Check if age is greater than 18",
  "root": {
    "type": "relation",
    "sign": "AND",
    "children": [
      {
        "id": "age-cond",
        "type": "condition",
        "sign": "GT",
        "key": "age",
        "value": "18",
        "valueType": "Number"
      }
    ]
  }
}
""";

Rule rule = RuleBuilder.fromJson(ruleJson).build();

// Evaluate with context
RuleContext context = new RuleContext();
context.setRuleContext(Map.of("age", 25));
boolean result = engine.evaluate(rule, context);  // returns true
```

## 📋 Supported Conditions

| Sign    | Description | Example |
|---------|-------------|---------|
| `EQ`    | Equal to | `status == "active"` |
| `NEQ`   | Not equal to | `status != "inactive"` |
| `GT`    | Greater than | `age > 18` |
| `GTE`   | Greater than or equal | `score >= 80` |
| `LT`    | Less than | `age < 65` |
| `LTE`   | Less than or equal | `score <= 100` |
| `IN`    | In collection | `status in ["active", "pending"]` |
| `NIN`   | Not in collection | `status not in ["deleted", "banned"]` |
| `REGEX` | Regular expression match | `email =~ "^[a-z]+@example\.com$"` |
| `CEL`   | CEL expression | `age > 18 && status == "active"` |

## 🔌 Condition Types

### Basic Conditions
- **EqualCondition**: Checks equality
- **NotEqualCondition**: Checks inequality
- **GreaterThanCondition**: Numeric comparison (>)
- **GreaterThanOrEqualCondition**: Numeric comparison (>=)
- **LessThanCondition**: Numeric comparison (<)
- **LessThanOrEqualCondition**: Numeric comparison (<=)

### Collection Conditions
- **InCondition**: Check if value exists in a collection
- **NotInCondition**: Check if value does not exist in a collection

### Pattern Matching
- **RegexCondition**: Match string against regex pattern
- **NegateRegexCondition**: Negated regex matching

### Advanced
- **CelCondition**: Google CEL expression evaluation
- **NegateCelCondition**: Negated CEL expression

## 🎨 Logical Operators

Combine multiple conditions with logical operators:

```java
// AND operator (all conditions must be true)
RelationNode andNode = new RelationNode();
andNode.setSign("AND");
andNode.setChildren(List.of(condition1, condition2));

// OR operator (at least one condition must be true)
RelationNode orNode = new RelationNode();
orNode.setSign("OR");
orNode.setChildren(List.of(condition1, condition2));

// NOT operator (negates the condition result)
RelationNode notNode = new RelationNode();
notNode.setSign("NOT");
notNode.setChildren(List.of(condition1));
```

## 📊 Event Listeners

### Rule Listeners

```java
engine.addRuleListener(new RuleListener() {
    @Override
    public void onRuleResult(RuleEvent event) {
        System.out.println("Rule: " + event.getRuleName() + 
                          " Result: " + event.isResult());
    }
    
    @Override
    public int order() {
        return 0;  // execution order
    }
});
```

### Condition Listeners

```java
engine.addConditionListener(new ConditionListener() {
    @Override
    public void onResult(ConditionEvent event) {
        System.out.println("Condition: " + event.getConditionName() + 
                          " Result: " + event.isResult());
    }
    
    @Override
    public int order() {
        return 0;  // execution order
    }
});
```

## 🔧 Advanced Features

### Rule Context Configuration

```java
RuleContext context = new RuleContext();

// Set rule parameters
context.setRuleContext(Map.of(
    "age", 25,
    "status", "active",
    "email", "user@example.com"
));

// Enable caching for better performance
rule.setECacheable(true);

// Configure evaluation behavior
engine.evaluate(rule, context);
```

### Batch Rule Evaluation

```java
List<Rule> rules = List.of(rule1, rule2, rule3);
RuleContext context = new RuleContext();
context.setRuleContext(evaluationData);

// Evaluate all rules in sequence
engine.evaluate(rules, context);
```

### Evaluation Reports

```java
context.setEnableReport(true);
boolean result = engine.evaluate(rule, context);

// Access detailed evaluation report
Report report = context.getReport(rule.getId());
if (report != null) {
    Set<Route> routes = report.getRoutes();
    // Analyze matched, unmatched, and skipped conditions
}
```

## 📖 Architecture

### Core Components

- **Rule Engine**: `DefaultCeleroEngine` and `AdvancedCeleroEngine` for rule evaluation
- **Rule Builder**: Fluent API for constructing rules programmatically or from JSON
- **Conditions**: Base classes and implementations for different condition types
- **Relations**: Logical operators (AND, OR, NOT) for combining conditions
- **Context**: Manages rule parameters and evaluation state
- **Listeners**: Event-driven architecture for tracking evaluation

### Module Structure

```
src/main/java/labs/franklee/celero/
├── engine/          # Rule evaluation engines
├── rules/           # Rule building and factories
├── logic/           # Conditions, relations, and operators
│   ├── base/        # Base classes and interfaces
│   └── impl/        # Concrete implementations
├── listener/        # Event listeners and events
├── context/         # Evaluation context management
└── exceptions/      # Custom exception types
```

## 🧪 Testing

Run the test suite:

```bash
mvn test
```

Test coverage includes:
- Condition evaluation (equality, comparison, regex, CEL)
- Logical operators (AND, OR, NOT)
- Rule building (programmatic and JSON)
- Path evaluation and route tracking
- Event listeners and reporting
- Edge cases and error handling

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📧 Contact

For questions, issues, or suggestions, please:
- Open an [Issue](https://github.com/franklee-labs/Celero/issues)
- Create a [Discussion](https://github.com/franklee-labs/Celero/discussions)
- Visit the repository: [franklee-labs/Celero](https://github.com/franklee-labs/Celero)

---

Made with ❤️ by [franklee-labs](https://github.com/franklee-labs)
