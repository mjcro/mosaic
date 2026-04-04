package io.github.mjcro.mosaic.handlers.sql;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;

public class ChangesDetectorTest {
    @Test
    public void testCalculate() {
        LinkedHashMap<String, List<Object>> valuesToStore = new LinkedHashMap<>();
        valuesToStore.put("id", Collections.singletonList(10L));
        valuesToStore.put("parentId", Collections.singletonList(2261L));
        valuesToStore.put("relationId", Collections.singletonList(999L));
        valuesToStore.put("type", Arrays.asList("one", "two", "three", "hundred"));

        ArrayList<ChangesDetector.StoredValue<Integer, String>> dataFromDatabase = new ArrayList<>();
        dataFromDatabase.add(new ChangesDetector.StoredValue<>(3, "parentId", 2261L));
        dataFromDatabase.add(new ChangesDetector.StoredValue<>(4, "relationId", 1000L));
        dataFromDatabase.add(new ChangesDetector.StoredValue<>(9, "type", "two"));
        dataFromDatabase.add(new ChangesDetector.StoredValue<>(10, "type", "three"));
        dataFromDatabase.add(new ChangesDetector.StoredValue<>(11, "type", "four"));

        ChangesDetector<Integer, String> detector = new ChangesDetector<>(valuesToStore, dataFromDatabase);

        Map<String, List<Object>> insert = detector.calculateValuesToInsert();
        Assertions.assertEquals(3, insert.size());
        Assertions.assertNotNull(insert.get("id"));
        Assertions.assertEquals(1, insert.get("id").size());
        Assertions.assertEquals(10L, insert.get("id").get(0));
        Assertions.assertNotNull(insert.get("type"));
        Assertions.assertEquals(2, insert.get("type").size());
        Assertions.assertEquals("one", insert.get("type").get(0));
        Assertions.assertEquals("hundred", insert.get("type").get(1));
        Assertions.assertNotNull(insert.get("relationId"));
        Assertions.assertEquals(1, insert.get("relationId").size());
        Assertions.assertEquals(999L, insert.get("relationId").get(0));

        Set<Integer> delete = detector.calculateIdToDelete();
        Assertions.assertEquals(2, delete.size());
        Assertions.assertTrue(delete.contains(4));
        Assertions.assertTrue(delete.contains(11));

        Set<Integer> intact = detector.calculateIdIntact();
        Assertions.assertEquals(3, intact.size());
        Assertions.assertTrue(intact.contains(3));
        Assertions.assertTrue(intact.contains(9));
        Assertions.assertTrue(intact.contains(10));
    }

    @Test
    public void testCalculateNoDatabase() {
        LinkedHashMap<String, List<Object>> valuesToStore = new LinkedHashMap<>();
        valuesToStore.put("id", Collections.singletonList(10L));
        valuesToStore.put("parentId", Collections.singletonList(2261L));

        ChangesDetector<Integer, String> detector = new ChangesDetector<>(valuesToStore, Collections.emptyList());

        Map<String, List<Object>> insert = detector.calculateValuesToInsert();
        Assertions.assertEquals(2, insert.size());
        Assertions.assertNotNull(insert.get("id"));
        Assertions.assertEquals(1, insert.get("id").size());
        Assertions.assertEquals(10L, insert.get("id").get(0));
        Assertions.assertNotNull(insert.get("parentId"));
        Assertions.assertEquals(1, insert.get("parentId").size());
        Assertions.assertEquals(2261L, insert.get("parentId").get(0));

        Set<Integer> delete = detector.calculateIdToDelete();
        Assertions.assertTrue(delete.isEmpty());

        Set<Integer> intact = detector.calculateIdIntact();
        Assertions.assertTrue(intact.isEmpty());
    }
}
