# Celero

<img src="./assets/celero_blue.svg" alt="Celero logo" width="250">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Celero** 是一个轻量、易用的 Java 规则引擎，支持通过流式 API 或 JSON 配置定义并评估复杂业务规则。

## 功能特性

- **灵活的规则定义**：支持 API 编程式构建，也支持从 JSON 反序列化
- **丰富的条件类型**：等值、比较、正则、集合、列表交集/不相交、字段存在性、CEL 表达式等
- **逻辑运算符**：AND / OR / NOT，可任意嵌套
- **三值逻辑**：支持 `TRUE` / `FALSE` / `INDETERMINATE` 三态结果（`AdvancedCeleroEngine`）
- **跨 Path 条件结果缓存**：同一条件在多路径中只执行一次（需显式开启）
- **条件优先级**：通过 `priority` 控制同一 Path 内条件的执行顺序
- **事件监听**：Rule 和 Condition 级别的回调，支持优先级排序
- **评估报告**：记录每条 Path 上已匹配、未匹配、缺失、已跳过的条件
- **CEL 表达式支持**：集成 [Google CEL](https://github.com/google/cel-spec) 进行高级表达式求值

---

## 安装

### 从 Maven 引入

```xml
<dependency>
    <groupId>io.github.franklee-labs</groupId>
    <artifactId>celero</artifactId>
    <version>0.0.2-RELEASE</version>
</dependency>
```

### 从源码构建

```bash
git clone https://github.com/franklee-labs/celero.git
cd celero
mvn clean install
```

---

## 核心设计

### 规则树 → Path 展开

Celero 的规则以**逻辑树**的形式描述：叶节点是 `ConditionNode`（条件），内节点是 `RelationNode`（AND / OR / NOT）。

在 `RuleBuilder.build()` 阶段，引擎将逻辑树**一次性展开**为一组扁平的 `Path`（称为 `PathGroup`）。每条 Path 是一个有序的条件列表，所有条件都满足则该 Path 通过。

展开规则：

| 表达式 | 展开结果 |
| --- | --- |
| `AND(A, B)` | 单条 Path：`[A, B]` |
| `OR(A, B)` | 两条 Path：`[A]` 和 `[B]` |
| `AND(A, OR(B, C))` | 两条 Path：`[A, B]` 和 `[A, C]` |
| `NOT(AND(A, B))` | 德摩根展开为 `OR(NOT(A), NOT(B))` |

引擎**按顺序**遍历所有 Path，只要有一条 Path 全部通过，规则即为真。这种设计将复杂逻辑的求值降低为对有序列表的简单扫描。

### EvalResult 三态

```java
public final class EvalResult {
    public static final EvalResult TRUE          = new EvalResult(State.TRUE);
    public static final EvalResult FALSE         = new EvalResult(State.FALSE);
    public static final EvalResult INDETERMINATE = new EvalResult(State.INDETERMINATE);
}
```

- **TRUE**：条件/规则明确为真
- **FALSE**：条件/规则明确为假
- **INDETERMINATE**：由于上下文中缺少所需参数，无法得出确定结论

`INDETERMINATE` 仅在 `AdvancedCeleroEngine` 中启用。`DefaultCeleroEngine` 将缺失参数当作 `FALSE` 处理，始终返回 `boolean`。

### 两种引擎

| | `DefaultCeleroEngine` | `AdvancedCeleroEngine` |
| --- | --- | --- |
| 返回类型 | `boolean` | `EvalResult` |
| 缺失参数处理 | 视为 `FALSE` | 返回 `INDETERMINATE` |
| 事件类型 | `ConditionEvent` / `RuleEvent` | `AdvancedConditionEvent` / `AdvancedRuleEvent` |

**INDETERMINATE 传播逻辑（AdvancedCeleroEngine）**：

一条 Path 内，遇到 `FALSE` 立即短路返回 `FALSE`；若遍历完所有条件后没有 `FALSE`，但存在 `INDETERMINATE`，则该 Path 结果为 `INDETERMINATE`。

规则级别：若所有 Path 均不为 `TRUE`，且至少一条 Path 为 `INDETERMINATE`，则规则返回 `INDETERMINATE`，否则返回 `FALSE`。

```text
path1: [A=INDETERMINATE, B=TRUE]  → INDETERMINATE
path2: [A=TRUE, B=FALSE]           → FALSE（短路）

rule → INDETERMINATE（有路径不确定，且无路径为 TRUE）
```

### 跨 Path 条件结果缓存

当规则展开后，同一个 `ConditionNode` 实例可能出现在多条 Path 中。例如 `AND(A, OR(B, C))` 展开为 `[A, B]` 和 `[A, C]`，条件 A 在两条 Path 中均出现。

若不开启缓存，A 会被执行两次（尤其是开销较大的正则匹配等复杂条件）。

**开启缓存后**，第一次执行 A 的结果会写入 `Context`，后续 Path 直接读取缓存，跳过再次执行。

缓存需同时满足两个条件（双开关设计，互相独立）：

1. **规则级开关**：`RuleBuilder.cacheable(true)` — 是否允许该规则使用缓存
2. **条件级开关**：条件节点上设置 `"cacheable": true` — 是否允许该条件的结果被缓存

```text
规则级 cacheable = false  →  无论条件如何，均不缓存
规则级 cacheable = true，条件级 cacheable = false  →  该条件不缓存
规则级 cacheable = true，条件级 cacheable = true   →  该条件结果被缓存并跨 Path 复用
```

缓存生命周期仅限于**单次规则评估**（存储于 `Context`），不会跨规则或跨请求泄漏。

> **注意**：`ConditionListener` 只在条件被真正执行时触发，缓存命中不会触发监听器。

---

## 快速上手

### 编程式构建规则

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

### JSON 构建规则

`fromJson` 分别接收规则 id 和逻辑树根节点的 JSON。条件属性嵌套在 `"properties"` 对象中。`cacheable`、`ignoreAbsence` 等是条件节点的**顶级字段**，不在 `"properties"` 内。

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

### 组合逻辑

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

对应展开的两条 Path：`[A, B]` 和 `[A, C]`。

### 开启缓存（多 Path 场景）

```java
// 条件 A 标记为可缓存（节点级字段）
ConditionNode condA = new ConditionNode();
condA.setId("cond-a").setSign("REGEXP").setCacheable(true);
condA.setProperties(Map.of("field", "email", "value", "^[\\w.+-]+@[\\w-]+\\.[a-z]{2,}$"));

// 规则级也开启缓存
CeleroRule rule = RuleBuilder.create()
    .id("rule")
    .cacheable(true)   // 规则级开关
    .root(andNode)
    .build();

// AND(A, OR(B, C)) → path1=[A,B], path2=[A,C]
// path1: 执行 A（结果缓存），执行 B → false
// path2: 直接读缓存中的 A 结果（不再执行），执行 C → true
DefaultCeleroEngine engine = new DefaultCeleroEngine();
engine.evaluate(rule, RuleContext.of(Map.of("email", "alice@example.com", "level", "high")));
```

JSON 中 `cacheable` 为条件节点的顶级字段：

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

## 使用 AdvancedCeleroEngine（三态结果）

```java
AdvancedCeleroEngine engine = new AdvancedCeleroEngine();

// 参数完整且匹配 → TRUE
EvalResult r1 = engine.evaluate(rule, RuleContext.of(Map.of("status", "active")));
r1.isTrue();   // true

// 参数存在但不匹配 → FALSE
EvalResult r2 = engine.evaluate(rule, RuleContext.of(Map.of("status", "inactive")));
r2.isFalse();  // true

// 参数缺失 → INDETERMINATE（无法判断）
EvalResult r3 = engine.evaluate(rule, RuleContext.of(Map.of()));
r3.isIndeterminate();  // true
```

`INDETERMINATE` 的典型应用场景：在渐进式规则匹配中，数据不完整时，区分「明确不满足条件」和「数据缺失，无法判断」，避免误判。例如用户填写表单时，引擎只对已填写的字段求值，尚未输入的字段返回 `INDETERMINATE` 而非 `FALSE`。

---

## 条件类型参考

| Sign | 说明 | Properties |
| --- | --- | --- |
| `EQ` | 等于 | `field`、`value`、`valueType`：String / Number / Boolean / Expression |
| `NEQ` | 不等于 | `field`、`value`、`valueType`：String / Number / Boolean / Expression |
| `GT` | 大于 | `field`、`value`、`valueType`：Number / Expression |
| `GTE` | 大于等于 | `field`、`value`、`valueType`：Number / Expression |
| `LT` | 小于 | `field`、`value`、`valueType`：Number / Expression |
| `LTE` | 小于等于 | `field`、`value`、`valueType`：Number / Expression |
| `IN` | 在集合中 | `field`、`value`（JSON 数组字符串）、`valueType`：List |
| `NIN` | 不在集合中 | `field`、`value`（JSON 数组字符串）、`valueType`：List |
| `REGEXP` | 正则匹配 | `field`、`value`（正则表达式） |
| `CEL` | Google CEL 表达式 | `expression`（CEL 表达式字符串） |
| `INTERSECT` | 两个列表有公共元素 | `field1`、`valueType1`、`field2`、`valueType2`：List / Expression |
| `DISJOINT` | 两个列表无公共元素 | `field1`、`valueType1`、`field2`、`valueType2`：List / Expression |
| `EXISTS` | 字段存在于上下文中 | `field`（字段名） |
| `ABSENT` | 字段不存在于上下文中 | `field`（字段名） |

### 特定条件说明

**`INTERSECT` / `DISJOINT`**：`valueType` 为 `List` 时，字段值为 JSON 数组字面量（如 `"[\"a\",\"b\"]"`）；`valueType` 为 `Expression` 时，字段为上下文中持有列表的变量名。

**`EXISTS` / `ABSENT`**：无论上下文模式如何，始终返回确定的 `TRUE` 或 `FALSE`。`ABSENT` 是 `EXISTS` 的逻辑取反，`ignoreAbsence` 对这两个条件不适用。

**数值类型**：`value` 字符串不含小数部分（如 `"18"`）时，引擎编译为 `int64`；含小数部分（如 `"18.5"`）时编译为 `double`。`RuleContext` 传入的参数需对应——整数规则传 `long`，小数规则传 `double`。

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

在 `ConditionNode` 的 `properties` 中设置 `priority` 字段：

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

## 忽略缺失（`ignoreAbsence`）

默认情况下，当条件所需参数在上下文中不存在时：

- `DefaultCeleroEngine` — 返回 `FALSE`
- `AdvancedCeleroEngine` — 返回 `INDETERMINATE`

对条件设置 `ignoreAbsence = true` 后，参数缺失将**始终**返回 `FALSE`，即使在 `AdvancedCeleroEngine` 下也不会产生 `INDETERMINATE`。适用于可选字段——缺失时直接判为不满足，而不让整条规则变为不确定。

`ignoreAbsence` 是条件节点的**顶级字段**，不在 `properties` 内。

**编程式：**

```java
ConditionNode cond = new ConditionNode();
cond.setId("opt-cond").setSign("EQ");
cond.setIgnoreAbsence(true);   // 顶级节点字段，不在 properties 中
cond.setProperties(Map.of(
    "field", "optionalTag",
    "value", "vip",
    "valueType", "String"
));
```

**JSON：**

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

`ignoreAbsence` 支持以下条件类型：`EQ`、`NEQ`、`GT`、`GTE`、`LT`、`LTE`、`IN`、`NIN`、`REGEXP`、`CEL`、`INTERSECT`、`DISJOINT`。
`EXISTS` / `ABSENT` **不支持**该属性，因为它们本身就是用于判断字段是否存在的。

---

## 事件监听

监听器在评估前注册，按 `order()` 升序调用（值越小越先执行）。监听器内抛出异常会被静默吞掉，不影响规则评估。

### DefaultCeleroEngine

```java
// Lambda 写法（ConditionListener 和 RuleListener 均为 @FunctionalInterface）
engine.addConditionListener(event ->
    System.out.println("条件 " + event.getConditionName() + " 匹配：" + event.isMatched())
);

engine.addRuleListener(event ->
    System.out.println("规则 " + event.getRuleName() + " 匹配：" + event.isMatched())
);

// 指定 order 的写法
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

使用 `AdvancedConditionListener` / `AdvancedRuleListener`，事件携带 `EvalResult`（三态）：

```java
engine.addConditionListener(event -> {
    EvalResult result = event.getResult();  // TRUE / FALSE / INDETERMINATE
    if (result.isIndeterminate()) {
        System.out.println("字段缺失：" + event.getConditionName());
    }
});

engine.addRuleListener(event -> {
    EvalResult result = event.isMatched();  // TRUE / FALSE / INDETERMINATE
    System.out.println("规则 " + event.getRuleName() + " → " + result);
});
```

### 通过 RuleContext 属性在监听器间传递状态

监听器可通过 `RuleContext` 的 key-value 属性在同一次评估中共享数据：

```java
// 低 order 的监听器写入
engine.addConditionListener(new ConditionListener() {
    public void onResult(ConditionEvent event) {
        int count = (int) Optional.ofNullable(event.getContext().getAttribute("count")).orElse(0);
        event.getContext().setAttribute("count", count + 1);
    }
    public int order() { return 1; }
});

// 高 order 的监听器读取
engine.addConditionListener(new ConditionListener() {
    public void onResult(ConditionEvent event) {
        System.out.println("已执行条件数：" + event.getContext().getAttribute("count"));
    }
    public int order() { return 10; }
});

// 评估结束后，调用方也可读取属性
RuleContext ctx = RuleContext.of(params);
engine.evaluate(rules, ctx);
System.out.println("合计：" + ctx.getAttribute("count"));
```

> **注意**：`params` 的 key 不能以 `_` 开头，`_` 前缀保留给引擎内置参数。

---

## 评估报告

启用报告后，每次规则评估会记录每条 Path 上各条件的状态：

```java
RuleContext ctx = RuleContext.of(params).setEnableReports(true);
engine.evaluate(rule, ctx);

Report report = ctx.getReports().get(rule);
for (Route route : report.getRoutes()) {
    route.getMatched();    // 已通过的条件
    route.getUnmatched();  // 未通过的条件（导致该 Path 失败）
    route.getAbsent();     // 结果为 INDETERMINATE 的条件（仅 AdvancedCeleroEngine）
    route.getSkipped();    // 因短路未执行的条件
}
```

每条 `Route` 对应一次 Path 评估，包含 `Route.Item`（conditionId + conditionName）。

---

## 批量评估

```java
List<CeleroRule> rules = List.of(rule1, rule2, rule3);
RuleContext ctx = RuleContext.of(params);

// DefaultCeleroEngine — 每条规则评估完后触发 RuleListener
engine.evaluate(rules, ctx);

// AdvancedCeleroEngine — 同上，但 RuleListener 携带 EvalResult
advancedEngine.evaluate(rules, ctx);
```

> **注意**：单规则版 `engine.evaluate(rule, ctx)` **不触发** `RuleListener`。需要规则级回调时请使用 List 版。

---

## 自定义条件类型

通过 `ConditionFactoryRegistry` 注册自定义工厂，可全局注册（所有规则可用）或按规则 id 注册：

```java
// 实现 ConditionFactory
public class StartsWithConditionFactory implements ConditionFactory {
    @Override
    public Condition create(ConditionNode node) {
        String field  = (String) node.getProperties().get("field");
        String prefix = (String) node.getProperties().get("value");
        return new StartsWithCondition(field, prefix);
    }
}

// 启动时注册一次 — sign 不能与内置 sign 冲突
ConditionFactoryRegistry.registerGlobalFactory("STARTS_WITH", new StartsWithConditionFactory());

// 或只对某条规则注册
ConditionFactoryRegistry.registerFactory("my-rule-id", "STARTS_WITH", new StartsWithConditionFactory());
```

注册后即可在规则定义中像内置 sign 一样使用。

---

## 架构概览

```text
RuleBuilder
  └─ fromJson(String id, String json) / create()
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
              │         ├─ ignoreAbsence=true          → FALSE
              │         ├─ DefaultCeleroEngine          → FALSE
              │         └─ AdvancedCeleroEngine         → INDETERMINATE
              └─ 写入缓存（若该条件开启了缓存）
```

### 包结构

```text
src/main/java/labs/franklee/celero/
├── engine/      DefaultCeleroEngine, AdvancedCeleroEngine, CeleroRule, RuleContext, Report, Route
├── rules/       RuleBuilder, Rule, ConditionNode, RelationNode, ConditionFactoryRegistry
├── logic/
│   ├── base/    Condition, Relation, EvalResult, Node, Priority, Validation
│   └── impl/    AND, OR, NOT, EqualCondition, CompareCondition, CelCondition, RegexCondition,
│                IntersectCondition, DisjointCondition, ExistsCondition, AbsentCondition ...
├── logic/path/  Path, PathGroup
├── listener/    RuleListener, ConditionListener, AdvancedXxx 变体
├── context/     Context（评估上下文，含缓存存储）
└── exceptions/  EvalException, InvalidConditionException, MissingParameterException ...
```

---

## 示例

`src/main/java/labs/franklee/celero/examples/` 目录下提供了可直接运行的示例，每个类均有 `main` 方法。

| 类名                                       | 演示内容 |
|------------------------------------------| --- |
| `SimpleRuleEngineExample`                | 从 JSON 加载规则，单规则与批量评估 |
| `SimpleRuleEngineListenersExample`       | `ConditionListener` / `RuleListener` 优先级排序及 context 属性共享 |
| `AdvancedRuleEngineExample`              | `AdvancedCeleroEngine` 三态结果（TRUE / FALSE / INDETERMINATE） |
| `AdvancedRuleEngineListenersExample`     | `AdvancedConditionListener` / `AdvancedRuleListener` 及 context 属性 |
| `AdvancedRuleEngineIgnoreAbsenceExample` | 同一规则内混用 `ignoreAbsence` 设置 |
| `SimpleRuleEngineCachedConditionExample` | 条件结果缓存——`A AND (B OR C)` 中 A 只执行一次 |

---

## 测试

```bash
mvn test
```

测试覆盖：

- 所有条件类型（等值、比较、正则、CEL、列表交集/不相交、字段存在性）
- AND / OR / NOT 逻辑及任意嵌套
- 三态（INDETERMINATE）传播与边界
- 缓存开关的各种组合（规则级 × 条件级 × 是否有命中）
- 评估报告（matched / unmatched / absent / skipped）
- JSON 规则解析与编程式构建

---

## 贡献

欢迎 PR！

1. Fork 本仓库
2. 创建分支（`git checkout -b feature/xxx`）
3. 提交更改（`git commit -m 'Add xxx'`）
4. Push（`git push origin feature/xxx`）
5. 发起 Pull Request

---

## License

[MIT](LICENSE)

---

Made with ❤️ by [franklee-labs](https://github.com/franklee-labs)
