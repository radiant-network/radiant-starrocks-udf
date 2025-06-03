package org.radiant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GetSequencingExperimentPartitionUDFTest {

    private GetSequencingExperimentPartitionUDF udf;

    @Test
    void testNullValues() {
        udf = new GetSequencingExperimentPartitionUDF();
        Integer result = udf.evaluate(null, 500);
        Assertions.assertNull(result);
    }

    @Test
    void testUnknownType() {
        udf = new GetSequencingExperimentPartitionUDF();
        Integer result = udf.evaluate(0x00030001, 1);
        Assertions.assertNull(result);
    }

    @Test
    void testMaxRange() {
        udf = new GetSequencingExperimentPartitionUDF();
        Integer result = udf.evaluate(0x0000FFFF, 100);
        Assertions.assertNull(result);
    }

    @Test
    void test_wgs_ShouldIncrement() {
        udf = new GetSequencingExperimentPartitionUDF();
        int result = udf.evaluate(0x0000FFFE, 100);
        Assertions.assertEquals(0x0000FFFF, result);
    }

    @Test
    void test_wgs_ShouldNotIncrement() {
        udf = new GetSequencingExperimentPartitionUDF();
        int result = udf.evaluate(0x00000001, 99);
        Assertions.assertEquals(0x00000001, result);
    }

    @Test
    void test_wxs_ShouldIncrement() {
        udf = new GetSequencingExperimentPartitionUDF();
        int result = udf.evaluate(0x00010001, 1000);
        Assertions.assertEquals(0x00010002, result);
    }

    @Test
    void test_wxs_ShouldNotIncrement() {
        udf = new GetSequencingExperimentPartitionUDF();
        int result = udf.evaluate(0x00010001, 999);
        Assertions.assertEquals(0x00010001, result);
    }

}