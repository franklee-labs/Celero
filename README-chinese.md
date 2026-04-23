# Celero

<div align="center">
    <img src="./assets/celero_blue.svg" width="200" />
</div>

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Celero** 是一个轻量、易用的 Java 规则引擎，支持通过流式 API 或 JSON 配置定义并评估复杂业务规则。

## 功能特性

- **灵活的规则定义**：支持流式 API 编程式构建，也支持从 JSON 反序列化
- **丰富的条件类型**：等值、比较、正则、集合、CEL 表达式等
- **逻辑运算符**：AND / OR / NOT，可任意嵌套
- **三值逻辑**：支持 `TRUE` / `FALSE` / `INDETERMINATE` 三态结果（`AdvancedCeleroEngine`）
- **跨 Path 条件结果缓存**：同一条件在多路径中只执行一次（需显式开启）
- **条件优先级**：通过 `priority` 控制同一 Path 内条件的执行顺序
- **事件监听**：Rule 和 Condition 级别的回调
- **评估报告**：记录每条 Path 上已匹配、未匹配、缺失、已跳过的条件
- **CEL 表达式支持**：集成 [Google CEL](https://github.com/google/cel-spec) 进行高级表达式求值

---

## 安装

```bash
git clone https://github.com/franklee-labs/Celero.git
cd Celero
mvn clean install
```

---

## 核心设计

### 规则树 → Path 展开

Celero 的规则以**逻辑树**的形式描述：叶节点是 `ConditionNode`（条件），内节点是 `RelationNode`（AND / OR / NOT）。

在 `RuleBuilder.build()` 阶段，引擎将逻辑树**一次性展开**为一组扁平的 `Path`（称为 `PathGroup`）。每条 Path 是一个有序的条件列表，所有条件都满足则该 Path 通过。

展开规则：

| 表达式 | 展开结果 |
|---|---|
| `AND(A, B)` | 单条 Path：`[A, B]` |
| `OR(A, B)` | 两条 Path：`[A]` 和 `[B]` |
| `AND(A, OR(B, C))` | 两条 Path：`[A, B]` 和 `[A, C]` |
| `NOT(AND(A, B))` | 德摩根展开为 `OR(NOT(A), NOT(B))` |

引擎**按顺序**遍历所有 Path，只要有一条 Path 全部通过，规则即为真。这种设计将复杂逻辑的求值降低为对有序列表的简单扫描。

### EvalResult 三态

```java
public final class EvalResult {
    public static final EvalResult TRUE         = new EvalResult(State.TRUE);
    public static final EvalResult FALSE        = new EvalResult(State.FALSE);
    public static final EvalResult INDETERMINATE = new EvalResult(State.INDETERMINATE);
}
```

- **TRUE**：条件/规则明确为真
- **FALSE**：条件/规则明确为假
- **INDETERMINATE**：由于上下文中缺少所需参数，无法得出确定结论

`INDETERMINATE` 仅在 `AdvancedCeleroEngine` 中启用。`DefaultCeleroEngine` 将缺失参数当作 `FALSE` 处理，始终返回 `boolean`。

### 两种引擎

| | `DefaultCeleroEngine` | `AdvancedCeleroEngine` |
|---|---|---|
| 返回类型 | `boolean` | `EvalResult` |
| 缺失参数处理 | 视为 `FALSE` | 返回 `INDETERMINATE` |
| 事件类型 | `ConditionEvent` / `RuleEvent` | `AdvancedConditionEvent` / `AdvancedRuleEvent` |

**INDETERMINATE 传播逻辑（AdvancedCeleroEngine）**：

一条 Path 内，遇到 `FALSE` 立即短路返回 `FALSE`；若遍历完所有条件后没有 `FALSE`，但存在 `INDETERMINATE`，则该 Path 结果为 `INDETERMINATE`。

规则级别：若所有 Path 均不为 `TRUE`，且至少一条 Path 为 `INDETERMINATE`，则规则返回 `INDETERMINATE`，否则返回 `FALSE`。

```
path1: [A=INDETERMINATE, B=TRUE]  → INDETERMINATE
path2: [A=TRUE, B=FALSE]           → FALSE（短路）

rule → INDETERMINATE（有路径不确定，且无路径为 TRUE）
```

### 跨 Path 条件结果缓存

当规则展开后，同一个 `ConditionNode` 实例可能出现在多条 Path 中。例如 `AND(A, OR(B, C))` 展开为 `[A, B]` 和 `[A, C]`，条件 A 在两条 Path 中均出现。

若不开启缓存，A 会被执行两次（尤其是开销较大的复杂正则匹配）。

**开启缓存后**，第一次执行 A 的结果会写入 `Context`，后续 Path 直接读取缓存，跳过再次执行。

缓存需同时满足两个条件（双开关设计，互相独立）：

1. **规则级开关**：`RuleBuilder.cacheable(true)` — 是否允许该规则使用缓存
2. **条件级开关**：`ConditionNode.setCacheable(true)` — 是否允许该条件的结果被缓存

```
规则级 cacheable = false  →  无论条件如何，均不缓存
规则级 cacheable = true，条件级 cacheable = false  →  该条件不缓存
规则级 cacheable = true，条件级 cacheable = true   →  该条件结果被缓存并跨 Path 复用
```

缓存生命周期仅限于**单次规则评估**（存储于 `Context`），不会跨规则或跨请求泄漏。

---

## 快速上手

### 编程式构建规则

```java
import labs.franklee.celero.engine.*;
import labs.franklee.celero.rules.*;

// 定义条件节点
ConditionNode statusCondition = new ConditionNode();
statusCondition.setId("cond-status");
statusCondition.setSign("EQ");
statusCondition.setProperties(Map.of(
    "field", "status",
    "value", "active",
    "valueType", "String"
));

// 构建规则
Rule rule = RuleBuilder.create()
    .id("rule-001")
    .name("Active User Check")
    .root(statusCondition)
    .build();

// 评估规则
DefaultCeleroEngine engine = new DefaultCeleroEngine();
RuleContext context = RuleContext.of(Map.of("status", "active"));

boolean result = engine.evaluate(rule, context);  // true
```

### JSON 构建规则

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

### 组合逻辑

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

对应展开的两条 Path：`[A, B]` 和 `[A, C]`。

### 开启缓存（多 Path 场景）

```java
// 条件 A 标记为可缓存
ConditionNode condA = new ConditionNode();
condA.setId("cond-a").setSign("EQ").setCacheable(true);
condA.setProperties(Map.of("field", "role", "value", "admin", "valueType", "String"));

// 规则级也开启缓存
Rule rule = RuleBuilder.create()
    .id("rule")
    .name("rule")
    .cacheable(true)   // 规则级开关
    .root(andNode)
    .build();

// AND(A, OR(B, C)) → path1=[A,B], path2=[A,C]
// path1: 执行 A（结果缓存），执行 B
// path2: 直接读缓存中的 A 结果，执行 C
// A 仅执行一次
DefaultCeleroEngine engine = new DefaultCeleroEngine();
engine.evaluate(rule, RuleContext.of(Map.of("role", "admin")));
```

---

## 使用 AdvancedCeleroEngine（三态结果）

```java
AdvancedCeleroEngine engine = new AdvancedCeleroEngine();

// 参数完整 → TRUE
EvalResult r1 = engine.evaluate(rule, RuleContext.of(Map.of("status", "active")));
r1.isTrue();   // true

// 参数不满足 → FALSE
EvalResult r2 = engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive")));
r2.isFalse();  // true

// 参数缺失 → INDETERMINATE（无法判断）
EvalResult r3 = engine.evaluate(rule, RuleContext.of(Map.of()));
r3.isIndeterminate();  // true
```

`INDETERMINATE` 的典型应用场景：在渐进式规则匹配场景中，数据不完整时，区分「明确不满足条件」和「数据缺失，无法判断」，避免误判。
如用户填写表单时，根据输入的信息动态匹配当前已传入的字段，对于尚未输入的字段，返回INDETERMINATE，而不是判定为FALSE。
---

## 条件类型参考

| sign    | 说明             | valueType                                |
|---------|----------------|------------------------------------------|
| `EQ`    | 等于             | String / Number / Boolean / expression   |
| `NEQ`   | 不等于            | String / Number / Boolean  /  expression |
| `GT`    | 大于             | Number /  expression                     |
| `GTE`   | 大于等于           | Number /  expression                     |
| `LT`    | 小于             | Number /  expression                     |
| `LTE`   | 小于等于           | Number /  expression                     |
| `IN`    | 在集合中           | 支持 String / Number / Boolean 混合          |
| `NIN`   | 不在集合中          | 支持 String / Number / Boolean 混合          |
| `REGEX` | 正则匹配           | reg expression                           |
| `CEL`   | Google CEL 表达式 | expression                               |

### CEL 表达式示例

```java
ConditionNode cel = new ConditionNode();
cel.setId("cel-cond").setSign("CEL");
cel.setProperties(Map.of("expression", "age > 18 && status == 'active'"));
```

---

## 条件优先级

同一 Path 内，优先级数值越小，执行越早（类似 `ORDER BY priority ASC`）：

```java
// 内置常量
Priority.HIGHEST = Integer.MIN_VALUE  // 最先执行
Priority.DEFAULT = 0
Priority.LOWEST  = Integer.MAX_VALUE  // 最后执行
```

通过JSON配置规则时，ConditionNode的priority字段可指定优先级。  
通过ConditionFactory创建时，在properties中传入priority(int)可指定优先级。  

---

## 事件监听

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

使用 `AdvancedConditionListener` / `AdvancedRuleListener`，事件携带 `EvalResult`（三态）：

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

多个监听器按 `order()` 升序执行（值越小越先执行）。监听器内抛出异常会被静默吞掉，不影响规则评估。

---

## 评估报告

启用报告后，每次规则评估会记录每条 Path 上各条件的状态：

```java
RuleContext ctx = RuleContext.of(params).setEnableReports(true);
engine.evaluate(rule, ctx);

Map<Rule, Report> reports = ctx.getReports();
Report report = reports.get(rule);
for (Route route : report.getRoutes()) {
    route.getMatched();    // 已通过的条件
    route.getUnmatched();  // 未通过的条件
    route.getAbsent();     // 结果为 INDETERMINATE 的条件（仅 AdvancedCeleroEngine）
    route.getSkipped();    // 因短路未执行的条件
}
```

每条 `Route` 对应一次 Path 评估，包含 `Route.Item`（conditionId + conditionName）。

---

## RuleContext 属性

`RuleContext` 除了存放规则参数（`params`）外，还支持附加任意 key-value 属性，可在监听器中读取：

```java
RuleContext ctx = RuleContext.of(params)
    .setAttribute("requestId", "abc-123")
    .setAttribute("source", "api");

// 在监听器中读取
String requestId = (String) event.getContext().getAttribute("requestId");
```

> **注意**：`params` 的 key 不能以 `_` 开头，`_` 前缀保留给引擎内置参数。

---

## 批量评估

```java
List<Rule> rules = List.of(rule1, rule2, rule3);
RuleContext ctx = RuleContext.of(params);

// DefaultCeleroEngine — 每条规则评估完后触发 RuleListener
engine.evaluate(rules, ctx);

// AdvancedCeleroEngine — 同上，但 RuleListener 携带 EvalResult
advancedEngine.evaluate(rules, ctx);
```

---

## 架构概览

```
RuleBuilder
  └─ fromJson(String) / create()
       └─ build()
            ├─ RelationNode.transform()  → 逻辑树（AND / OR / NOT + Condition 节点）
            ├─ Node.validateAll()        → 结构校验
            └─ Relation.resolve()        → 展开为 PathGroup（Path 列表）
                                             每条 Path 按 priority 排序

评估阶段：
  Engine.evaluate(rule, ruleContext)
    └─ 遍历 PathGroup 中每条 Path
         └─ 顺序执行 Path 内每个 Condition
              ├─ 检查缓存（若开启）
              ├─ Condition.execute(context)
              │    ├─ evaluate() 正常 → TRUE / FALSE
              │    └─ MissingParameterException
              │         ├─ DefaultCeleroEngine → FALSE
              │         └─ AdvancedCeleroEngine → INDETERMINATE
              └─ 写入缓存（若开启）
```

### 包结构

```
src/main/java/labs/franklee/celero/
├── engine/      DefaultCeleroEngine, AdvancedCeleroEngine, RuleContext, Report, Route
├── rules/       RuleBuilder, Rule, ConditionNode, RelationNode, ConditionFactoryRegistry
├── logic/
│   ├── base/    Condition, Relation, EvalResult, Node, Priority, Validation
│   └── impl/    AND, OR, NOT, EqualCondition, CelCondition, RegexCondition ...
├── logic/path/  Path, PathGroup
├── listener/    RuleListener, ConditionListener, AdvancedXxx 变体
├── context/     Context（评估上下文，含缓存存储）
└── exceptions/  EvalException, InvalidConditionException, MissingParameterException ...
```

---

## 测试

```bash
mvn test
```

测试覆盖：
- 所有条件类型（等值、比较、正则、CEL）
- AND / OR / NOT 逻辑及任意嵌套
- 三态（INDETERMINATE）传播与边界
- 缓存开关的各种组合（规则级 × 条件级 × 是否有命中）
- 评估报告（matched / unmatched / absent / skipped）
- JSON 规则解析与编程式构建

---

## 贡献

欢迎 PR！

1. Fork 本仓库
2. 创建分支 (`git checkout -b feature/xxx`)
3. 提交更改 (`git commit -m 'Add xxx'`)
4. Push (`git push origin feature/xxx`)
5. 发起 Pull Request

---

## License

[MIT](LICENSE)

---

Made with ❤️ by [franklee-labs](https://github.com/franklee-labs)
