package labs.franklee.celero.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConditionNodeTest {

    @Test
    public void jsonSetterGetter() throws JsonProcessingException {
        String json = """
                {
                    "id": "id-1",
                    "name": "name-1",
                    "description": "desc-1",
                    "sign": "+",
                    "type": "condition",
                    "cacheable": true,
                    "k1": "v1",
                    "k2": 2
                }
                """;
        JsonMapper mapper = new JsonMapper();
        RuleNode conditionNode = mapper.readValue(json, RuleNode.class);
        assertInstanceOf(ConditionNode.class, conditionNode);
        assertEquals("id-1", conditionNode.getId());
        assertEquals("name-1", conditionNode.getName());
        assertEquals("desc-1", conditionNode.getDescription());
        assertEquals("+", conditionNode.getSign());
        assertEquals("condition", conditionNode.getType());
        assertTrue(((ConditionNode)conditionNode).isCacheable());
        assertEquals(2, ((ConditionNode)conditionNode).getProperties().size());
        assertEquals("v1", ((ConditionNode)conditionNode).getProperties().get("k1"));
        assertEquals(2, ((ConditionNode)conditionNode).getProperties().get("k2"));
        assertFalse(conditionNode.isRelationNode());
        assertTrue(conditionNode.isConditionNode());
    }
}
