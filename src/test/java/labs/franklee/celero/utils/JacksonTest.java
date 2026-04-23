package labs.franklee.celero.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JacksonTest {

    @Test
    public void dynamicList_fromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                [1, "2", true, 3.0090000000001, 1.01, 1.00, 1.23E10]
                """;
        List<Object> list = mapper.readValue(json, new TypeReference<>() {});
        assertNotNull(list);
        for (Object o : list) {
            System.out.println("class:" + o.getClass() + " value:" + o);
        }
        assertEquals(7, list.size());
        assertTrue(list.get(0).equals(1) && list.get(0) instanceof Integer);
        assertTrue("2".equals(list.get(1)) && list.get(1) instanceof String);
        assertTrue(list.get(2) instanceof Boolean b && b);
        assertTrue(list.get(3).equals(3.0090000000001) && list.get(3) instanceof Double);
        assertTrue(list.get(4).equals(1.01) && list.get(4) instanceof Double);
        assertTrue(list.get(5).equals(1.00) && list.get(5) instanceof Double);
        assertTrue(list.get(6).equals(1.23E10) && list.get(6) instanceof Double);
    }
}
