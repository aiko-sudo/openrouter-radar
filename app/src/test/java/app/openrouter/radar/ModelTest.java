package app.openrouter.radar;
import static org.junit.Assert.*;
import org.junit.Test;
import java.util.List;
public class ModelTest {
    @Test public void parseFree() throws Exception {
        String json = "{\"data\":[{\"id\":\"a/b\",\"name\":\"A\",\"created\":1700000000,\"context_length\":128000,\"pricing\":{\"prompt\":\"0\",\"completion\":\"0\"}},{\"id\":\"c/d\",\"name\":\"C\",\"created\":1700000001,\"pricing\":{\"prompt\":\"0.0000002\",\"completion\":\"0.0000006\"}}]}";
        List<Model> ms = Model.parse(json);
        assertEquals(2, ms.size());
        assertTrue(ms.get(0).isFree());
        assertFalse(ms.get(1).isFree());
    }
    @Test public void empty() throws Exception {
        assertEquals(0, Model.parse("{\"data\":[]}").size());
    }
}
