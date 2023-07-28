package com.bitmovin.analytics.api;

import org.junit.Test;

public class CustomDataBuilderJavaTest {
    @Test
    public void test_createCustomDataWithBuilder() {
        CustomData customData = createCustomDataWithBuilder();

        assert(customData.getCustomData1().equals("customData1"));
        assert(customData.getCustomData2().equals("customData2"));
        assert(customData.getCustomData3().equals("customData3"));
        assert(customData.getCustomData4().equals("customData4"));
        assert(customData.getCustomData5().equals("customData5"));
        assert(customData.getCustomData6().equals("customData6"));
        assert(customData.getCustomData7().equals("customData7"));
        assert(customData.getCustomData8().equals("customData8"));
        assert(customData.getCustomData9().equals("customData9"));
        assert(customData.getCustomData10().equals("customData10"));
        assert(customData.getCustomData11().equals("customData11"));
        assert(customData.getCustomData12().equals("customData12"));
        assert(customData.getCustomData13().equals("customData13"));
        assert(customData.getCustomData14().equals("customData14"));
        assert(customData.getCustomData15().equals("customData15"));
        assert(customData.getCustomData16().equals("customData16"));
        assert(customData.getCustomData17().equals("customData17"));
        assert(customData.getCustomData18().equals("customData18"));
        assert(customData.getCustomData19().equals("customData19"));
        assert(customData.getCustomData20().equals("customData20"));
        assert(customData.getCustomData21().equals("customData21"));
        assert(customData.getCustomData22().equals("customData22"));
        assert(customData.getCustomData23().equals("customData23"));
        assert(customData.getCustomData24().equals("customData24"));
        assert(customData.getCustomData25().equals("customData25"));
        assert(customData.getCustomData26().equals("customData26"));
        assert(customData.getCustomData27().equals("customData27"));
        assert(customData.getCustomData28().equals("customData28"));
        assert(customData.getCustomData29().equals("customData29"));
        assert(customData.getCustomData30().equals("customData30"));
        assert(customData.getExperimentName().equals("experimentName"));
    }

    @Test
    public void test_copyCustomDataWithBuilder() {
        CustomData customDataToCopy = createCustomDataWithBuilder();

        CustomData customData = new CustomData.Builder(customDataToCopy)
                .build();

        assert(customData.getCustomData1().equals("customData1"));
        assert(customData.getCustomData2().equals("customData2"));
        assert(customData.getCustomData3().equals("customData3"));
        assert(customData.getCustomData4().equals("customData4"));
        assert(customData.getCustomData5().equals("customData5"));
        assert(customData.getCustomData6().equals("customData6"));
        assert(customData.getCustomData7().equals("customData7"));
        assert(customData.getCustomData8().equals("customData8"));
        assert(customData.getCustomData9().equals("customData9"));
        assert(customData.getCustomData10().equals("customData10"));
        assert(customData.getCustomData11().equals("customData11"));
        assert(customData.getCustomData12().equals("customData12"));
        assert(customData.getCustomData13().equals("customData13"));
        assert(customData.getCustomData14().equals("customData14"));
        assert(customData.getCustomData15().equals("customData15"));
        assert(customData.getCustomData16().equals("customData16"));
        assert(customData.getCustomData17().equals("customData17"));
        assert(customData.getCustomData18().equals("customData18"));
        assert(customData.getCustomData19().equals("customData19"));
        assert(customData.getCustomData20().equals("customData20"));
        assert(customData.getCustomData21().equals("customData21"));
        assert(customData.getCustomData22().equals("customData22"));
        assert(customData.getCustomData23().equals("customData23"));
        assert(customData.getCustomData24().equals("customData24"));
        assert(customData.getCustomData25().equals("customData25"));
        assert(customData.getCustomData26().equals("customData26"));
        assert(customData.getCustomData27().equals("customData27"));
        assert(customData.getCustomData28().equals("customData28"));
        assert(customData.getCustomData29().equals("customData29"));
        assert(customData.getCustomData30().equals("customData30"));
        assert(customData.getExperimentName().equals("experimentName"));
    }

    @Test
    public void test_copyAndSetCustomDataWithBuilder() {
        CustomData customDataToCopy = createCustomDataWithBuilder();

        CustomData customData = new CustomData.Builder(customDataToCopy)
                .setCustomData1("customData1Changed")
                .setCustomData30("customData30Changed")
                .setExperimentName("experimentNameChanged")
                .build();

        assert(customData.getCustomData1().equals("customData1Changed"));
        assert(customData.getCustomData2().equals("customData2"));
        assert(customData.getCustomData3().equals("customData3"));
        assert(customData.getCustomData4().equals("customData4"));
        assert(customData.getCustomData5().equals("customData5"));
        assert(customData.getCustomData6().equals("customData6"));
        assert(customData.getCustomData7().equals("customData7"));
        assert(customData.getCustomData8().equals("customData8"));
        assert(customData.getCustomData9().equals("customData9"));
        assert(customData.getCustomData10().equals("customData10"));
        assert(customData.getCustomData11().equals("customData11"));
        assert(customData.getCustomData12().equals("customData12"));
        assert(customData.getCustomData13().equals("customData13"));
        assert(customData.getCustomData14().equals("customData14"));
        assert(customData.getCustomData15().equals("customData15"));
        assert(customData.getCustomData16().equals("customData16"));
        assert(customData.getCustomData17().equals("customData17"));
        assert(customData.getCustomData18().equals("customData18"));
        assert(customData.getCustomData19().equals("customData19"));
        assert(customData.getCustomData20().equals("customData20"));
        assert(customData.getCustomData21().equals("customData21"));
        assert(customData.getCustomData22().equals("customData22"));
        assert(customData.getCustomData23().equals("customData23"));
        assert(customData.getCustomData24().equals("customData24"));
        assert(customData.getCustomData25().equals("customData25"));
        assert(customData.getCustomData26().equals("customData26"));
        assert(customData.getCustomData27().equals("customData27"));
        assert(customData.getCustomData28().equals("customData28"));
        assert(customData.getCustomData29().equals("customData29"));
        assert(customData.getCustomData30().equals("customData30Changed"));
        assert(customData.getExperimentName().equals("experimentNameChanged"));
    }

    @Test
    public void test_buildUponCustomDataWithBuilder() {
        CustomData customDataToBuildUpon = createCustomDataWithBuilder();

        CustomData customData = customDataToBuildUpon.buildUpon()
                .setCustomData1("customData1Changed")
                .setCustomData30("customData30Changed")
                .setExperimentName("experimentNameChanged")
                .build();

        assert(customData.getCustomData1().equals("customData1Changed"));
        assert(customData.getCustomData2().equals("customData2"));
        assert(customData.getCustomData3().equals("customData3"));
        assert(customData.getCustomData4().equals("customData4"));
        assert(customData.getCustomData5().equals("customData5"));
        assert(customData.getCustomData6().equals("customData6"));
        assert(customData.getCustomData7().equals("customData7"));
        assert(customData.getCustomData8().equals("customData8"));
        assert(customData.getCustomData9().equals("customData9"));
        assert(customData.getCustomData10().equals("customData10"));
        assert(customData.getCustomData11().equals("customData11"));
        assert(customData.getCustomData12().equals("customData12"));
        assert(customData.getCustomData13().equals("customData13"));
        assert(customData.getCustomData14().equals("customData14"));
        assert(customData.getCustomData15().equals("customData15"));
        assert(customData.getCustomData16().equals("customData16"));
        assert(customData.getCustomData17().equals("customData17"));
        assert(customData.getCustomData18().equals("customData18"));
        assert(customData.getCustomData19().equals("customData19"));
        assert(customData.getCustomData20().equals("customData20"));
        assert(customData.getCustomData21().equals("customData21"));
        assert(customData.getCustomData22().equals("customData22"));
        assert(customData.getCustomData23().equals("customData23"));
        assert(customData.getCustomData24().equals("customData24"));
        assert(customData.getCustomData25().equals("customData25"));
        assert(customData.getCustomData26().equals("customData26"));
        assert(customData.getCustomData27().equals("customData27"));
        assert(customData.getCustomData28().equals("customData28"));
        assert(customData.getCustomData29().equals("customData29"));
        assert(customData.getCustomData30().equals("customData30Changed"));
        assert(customData.getExperimentName().equals("experimentNameChanged"));
    }

    private CustomData createCustomDataWithBuilder() {
        CustomData customData = new CustomData.Builder()
                .setCustomData1("customData1")
                .setCustomData2("customData2")
                .setCustomData3("customData3")
                .setCustomData4("customData4")
                .setCustomData5("customData5")
                .setCustomData6("customData6")
                .setCustomData7("customData7")
                .setCustomData8("customData8")
                .setCustomData9("customData9")
                .setCustomData10("customData10")
                .setCustomData11("customData11")
                .setCustomData12("customData12")
                .setCustomData13("customData13")
                .setCustomData14("customData14")
                .setCustomData15("customData15")
                .setCustomData16("customData16")
                .setCustomData17("customData17")
                .setCustomData18("customData18")
                .setCustomData19("customData19")
                .setCustomData20("customData20")
                .setCustomData21("customData21")
                .setCustomData22("customData22")
                .setCustomData23("customData23")
                .setCustomData24("customData24")
                .setCustomData25("customData25")
                .setCustomData26("customData26")
                .setCustomData27("customData27")
                .setCustomData28("customData28")
                .setCustomData29("customData29")
                .setCustomData30("customData30")
                .setExperimentName("experimentName")
                .build();
        return customData;
    }
}
