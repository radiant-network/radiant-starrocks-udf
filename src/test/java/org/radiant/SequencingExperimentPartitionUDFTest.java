package org.radiant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SequencingExperimentPartitionUDFTest {

    private SequencingExperimentPartitionUDF udf;

    @Test
    void test_UnsupportedSequencingType() {
        udf = new SequencingExperimentPartitionUDF();
        Integer result = udf.evaluate("unsupported", 0x00020001, 50);
        Assertions.assertNull(result);
    }

    @Test
    void test_wgs_PartitionIdDoesNotMatchSequencingTypeId() {
        udf = new SequencingExperimentPartitionUDF();
        Integer result = udf.evaluate("wgs", 0x00020001, 50);
        Assertions.assertNull(result);
    }

    @Test
    void test_wgs_ShouldIncrement() {
        udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate("wgs", 0x00010001, 100);
        Assertions.assertEquals(0x00010002, result);
    }

    @Test
    void test_wgs_ShouldNotIncrement() {
        udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate("wgs", 0x00010001, 99);
        Assertions.assertEquals(0x00010001, result);
    }

    @Test
    void test_wgs_UpperCase() {
        SequencingExperimentPartitionUDF udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate("WGS", 0x0001000F, 1500);
        Assertions.assertEquals(0x00010010, result);
    }

    @Test
    void test_wxs_PartitionIdDoesNotMatchSequencingTypeId() {
        udf = new SequencingExperimentPartitionUDF();
        Integer result = udf.evaluate("wxs", 0x00010001, 500);
        Assertions.assertNull(result);
    }

    @Test
    void test_wxs_ShouldIncrement() {
        udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate("wxs", 0x00020001, 1000);
        Assertions.assertEquals(0x00020002, result);
    }

    @Test
    void test_wxs_ShouldNotIncrement() {
        udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate("wxs", 0x00020001, 999);
        Assertions.assertEquals(0x00020001, result);
    }

    @Test
    void test_wxs_UpperCase() {
        SequencingExperimentPartitionUDF udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate("WXS", 0x0002000F, 1500);
        Assertions.assertEquals(0x00020010, result);
    }

}