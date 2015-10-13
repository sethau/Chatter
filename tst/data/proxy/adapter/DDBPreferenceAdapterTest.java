package data.proxy.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.document.Item;

import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;

/**
 * Tests the functionality of the DDBPreferenceAdapter class.
 */
public class DDBPreferenceAdapterTest {
    
    private static final PreferenceCategory PREFERENCE_CATEGORY_TO_USE = PreferenceCategory.BOOKS;
    private static final String PREFERENCE_ID = "Lord of the Rings";
    private static final int PREFERENCE_POPULARITY = 20;
    private static final String CORRELATION_PREFERENCE_ID = "Harry Potter";
    private static final int CORRELATION_WEIGHT = 10;
    
    private Preference testPreference;
    private PreferenceCorrelation testCorrelation;
    private Item testModel;
    
    /**
     * Creates the Preference object and corresponding DynamoDB model to be used in the subsequent
     * tests.
     */
    @Before
    public void setup() {
        testCorrelation = new PreferenceCorrelation(
                DDBPreferenceAdapter.buildDBStringFromComponents(CORRELATION_PREFERENCE_ID,
                        PREFERENCE_CATEGORY_TO_USE), CORRELATION_WEIGHT);
        
        List<PreferenceCorrelation> correlations = new ArrayList<PreferenceCorrelation>();
        correlations.add(testCorrelation);
        
        Map<String, Integer> dbCorrelations = new HashMap<String, Integer>();
        dbCorrelations.put(testCorrelation.getToPreferenceID(), testCorrelation.getWeight());
        
        testPreference = new Preference(PREFERENCE_ID, PREFERENCE_CATEGORY_TO_USE,
                PREFERENCE_POPULARITY, correlations);
        testModel = new Item()
                .withPrimaryKey(
                        DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE,
                        DDBPreferenceAdapter.buildDBStringFromComponents(PREFERENCE_ID,
                                PREFERENCE_CATEGORY_TO_USE))
                .withMap(DDBPreferenceAdapter.CORRELATIONS_ATTRIBUTE, dbCorrelations)
                .withInt(DDBPreferenceAdapter.POPULARITY_ATTRIBUTE, PREFERENCE_POPULARITY);
    }
    
    /**
     * Tests that the toObject() method cannot be called without first setting the DynamoDB Item.
     */
    @Test
    public void testMissingModel() {
        DDBPreferenceAdapter adapter = new DDBPreferenceAdapter();
        
        boolean thrown = false;
        
        try {
            adapter.toObject();
        } catch (IllegalStateException e) {
            thrown = true;
        }
        
        assertTrue(
                "The adapter was asked to provide a Preference object without a DynamoDB Item, but no exception was thrown!",
                thrown);
    }
    
    /**
     * Tests that the toDBModel() method cannot be called without first setting the UserProfile
     * object.
     */
    @Test
    public void testMissingObject() {
        DDBPreferenceAdapter adapter = new DDBPreferenceAdapter();
        
        boolean thrown = false;
        
        try {
            adapter.toDBModel();
        } catch (IllegalStateException e) {
            thrown = true;
        }
        
        assertTrue(
                "The adapter was asked to provide a DynamoDB Item without a Preference object, but no exception was thrown!",
                thrown);
    }
    
    /**
     * Tests the conversion of a Preference object to a DynamoDB model.
     */
    @Test
    public void testToDBModel() {
        DDBPreferenceAdapter adapter = new DDBPreferenceAdapter().withObject(testPreference);
        Item result = adapter.toDBModel();
        
        assertEquals("The returned Item did not have the expected ID!",
                testModel.getString(DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE),
                result.getString(DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE));
        
        Map<String, Object> resultCorrelations = result
                .getMap(DDBPreferenceAdapter.CORRELATIONS_ATTRIBUTE);
        
        for (Entry<String, Object> entry : testModel.getMap(
                DDBPreferenceAdapter.CORRELATIONS_ATTRIBUTE).entrySet()) {
            assertTrue("The returned Item did not have the expected correlations!",
                    resultCorrelations.containsKey(entry.getKey()));
            
            int resultWeight = (int) resultCorrelations.get(entry.getKey());
            assertEquals("The returned correlation did not have the expected weight!",
                    entry.getValue(), resultWeight);
        }
    }
    
    /**
     * Tests the conversion of a DynamoDB model to a Preference object.
     */
    @Test
    public void testToObject() {
        DDBPreferenceAdapter adapter = new DDBPreferenceAdapter().withDBModel(testModel);
        Preference result = adapter.toObject();
        
        assertEquals("The returned Preference did not have the expected ID!",
                testPreference.getID(), result.getID());
        
        List<PreferenceCorrelation> expectedCorrelations = new ArrayList<PreferenceCorrelation>();
        expectedCorrelations.addAll(testPreference.getCorrelations());
        List<PreferenceCorrelation> actualCorrelations = new ArrayList<PreferenceCorrelation>();
        actualCorrelations.addAll(result.getCorrelations());
        
        assertEquals("The returned Preference did not have the expected number of correlations!",
                expectedCorrelations.size(), actualCorrelations.size());
        
        Comparator<PreferenceCorrelation> comparator = new Comparator<PreferenceCorrelation>() {
            @Override
            public int compare(PreferenceCorrelation a, PreferenceCorrelation b) {
                return a.getToPreferenceID().compareTo(b.getToPreferenceID());
            }
        };
        
        Collections.sort(expectedCorrelations, comparator);
        Collections.sort(actualCorrelations, comparator);
        
        for (int i = 0; i < expectedCorrelations.size(); i++) {
            PreferenceCorrelation expected = expectedCorrelations.get(i);
            PreferenceCorrelation actual = actualCorrelations.get(i);
            assertEquals("The returned Preference did not have the correct correlations!",
                    expected.getToPreferenceID(), actual.getToPreferenceID());
            assertEquals(
                    "The returned Preference's correlations did not have the correct weights!",
                    expected.getWeight(), actual.getWeight());
        }
    }
}
