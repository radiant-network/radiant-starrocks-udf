package org.radiant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SequencingExperimentPartitionUDFTest {

    private SequencingExperimentPartitionUDF udf;

    @Test
    void testNullValues() {
        udf = new SequencingExperimentPartitionUDF();
        Integer result = udf.evaluate(null, 500);
        Assertions.assertNull(result);
    }

    @Test
    void test_wgs_ShouldIncrement() {
        udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate(0x00010001, 100);
        Assertions.assertEquals(0x00010002, result);
    }

    @Test
    void test_wgs_ShouldNotIncrement() {
        udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate(0x00010001, 99);
        Assertions.assertEquals(0x00010001, result);
    }

    @Test
    void test_wgs_UpperCase() {
        udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate(0x0001000F, 1500);
        Assertions.assertEquals(0x00010010, result);
    }

    @Test
    void test_wxs_ShouldIncrement() {
        udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate(0x00020001, 1000);
        Assertions.assertEquals(0x00020002, result);
    }

    @Test
    void test_wxs_ShouldNotIncrement() {
        udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate(0x00020001, 999);
        Assertions.assertEquals(0x00020001, result);
    }

    @Test
    void test_wxs_UpperCase() {
        SequencingExperimentPartitionUDF udf = new SequencingExperimentPartitionUDF();
        int result = udf.evaluate(0x0002000F, 1500);
        Assertions.assertEquals(0x00020010, result);
    }

}