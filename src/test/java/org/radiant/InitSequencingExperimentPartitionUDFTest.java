package org.radiant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InitSequencingExperimentPartitionUDFTest {

    @Test
    void testNullAnalysisType() {
        InitSequencingExperimentPartitionUDF udf = new InitSequencingExperimentPartitionUDF();
        Integer result = udf.evaluate(null);
        Assertions.assertNull(result);
    }

    @Test
    void testEmptyAnalysisType() {
        InitSequencingExperimentPartitionUDF udf = new InitSequencingExperimentPartitionUDF();
        Integer result = udf.evaluate("");
        Assertions.assertNull(result);
    }

    @Test
    void testUnknownType() {
        InitSequencingExperimentPartitionUDF udf = new InitSequencingExperimentPartitionUDF();
        Integer result = udf.evaluate("unsupported");
        Assertions.assertNull(result);
    }

    @Test
    void testWgsInit() {
        InitSequencingExperimentPartitionUDF udf = new InitSequencingExperimentPartitionUDF();
        Integer result = udf.evaluate("wgs");
        Assertions.assertEquals(0x00000000, result);
    }

    @Test
    void testWgsInitCaseInsensitive() {
        InitSequencingExperimentPartitionUDF udf = new InitSequencingExperimentPartitionUDF();
        Integer result = udf.evaluate("WGS");
        Assertions.assertEquals(0x00000000, result);
    }

    @Test
    void testWxsInit() {
        InitSequencingExperimentPartitionUDF udf = new InitSequencingExperimentPartitionUDF();
        Integer result = udf.evaluate("wxs");
        Assertions.assertEquals(0x00010000, result);
    }

    @Test
    void testWxsInitCaseInsensitive() {
        InitSequencingExperimentPartitionUDF udf = new InitSequencingExperimentPartitionUDF();
        Integer result = udf.evaluate("WXS");
        Assertions.assertEquals(0x00010000, result);
    }
}