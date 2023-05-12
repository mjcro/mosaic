package io.github.mjcro.mosaic.handlers.sql;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        Assert.assertEquals(insert.size(), 3);
        Assert.assertNotNull(insert.get("id"));
        Assert.assertEquals(insert.get("id").size(), 1);
        Assert.assertEquals(insert.get("id").get(0), 10L);
        Assert.assertNotNull(insert.get("type"));
        Assert.assertEquals(insert.get("type").size(), 2);
        Assert.assertEquals(insert.get("type").get(0), "one");
        Assert.assertEquals(insert.get("type").get(1), "hundred");
        Assert.assertNotNull(insert.get("relationId"));
        Assert.assertEquals(insert.get("relationId").size(), 1);
        Assert.assertEquals(insert.get("relationId").get(0), 999L);

        Set<Integer> delete = detector.calculateIdToDelete();
        Assert.assertEquals(delete.size(), 2);
        Assert.assertTrue(delete.contains(4));
        Assert.assertTrue(delete.contains(11));

        Set<Integer> intact = detector.calculateIdIntact();
        Assert.assertEquals(intact.size(), 3);
        Assert.assertTrue(intact.contains(3));
        Assert.assertTrue(intact.contains(9));
        Assert.assertTrue(intact.contains(10));
    }

    @Test
    public void testCalculateNoDatabase() {
        LinkedHashMap<String, List<Object>> valuesToStore = new LinkedHashMap<>();
        valuesToStore.put("id", Collections.singletonList(10L));
        valuesToStore.put("parentId", Collections.singletonList(2261L));

        ChangesDetector<Integer, String> detector = new ChangesDetector<>(valuesToStore, Collections.emptyList());

        Map<String, List<Object>> insert = detector.calculateValuesToInsert();
        Assert.assertEquals(insert.size(), 2);
        Assert.assertNotNull(insert.get("id"));
        Assert.assertEquals(insert.get("id").size(), 1);
        Assert.assertEquals(insert.get("id").get(0), 10L);
        Assert.assertNotNull(insert.get("parentId"));
        Assert.assertEquals(insert.get("parentId").size(), 1);
        Assert.assertEquals(insert.get("parentId").get(0), 2261L);

        Set<Integer> delete = detector.calculateIdToDelete();
        Assert.assertTrue(delete.isEmpty());

        Set<Integer> intact = detector.calculateIdIntact();
        Assert.assertTrue(intact.isEmpty());
    }
}