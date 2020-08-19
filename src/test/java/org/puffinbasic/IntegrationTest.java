package org.puffinbasic;

import org.junit.Before;
import org.puffinbasic.PuffinBasicInterpreterMain.UserOptions;
import org.puffinbasic.error.PuffinBasicRuntimeError;
import org.junit.Test;
import org.puffinbasic.runtime.Environment;
import org.puffinbasic.runtime.Environment.SystemEnv;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.puffinbasic.PuffinBasicInterpreterMain.interpretAndRun;
import static org.puffinbasic.error.PuffinBasicRuntimeError.ErrorCode.IO_ERROR;
import static org.junit.Assert.assertEquals;

public class IntegrationTest {

    private Environment env;

    @Before
    public void setup() {
        env = new SystemEnv();
    }

    @Test
    public void testForLoop() {
        runTest("forloop.bas", "forloop.bas.output");
    }

    @Test
    public void testNestedForLoop() {
        runTest("nested_forloop.bas", "nested_forloop.bas.output");
    }

    @Test
    public void testScalarVariable() {
        runTest("scalar_var.bas", "scalar_var.bas.output");
    }

    @Test
    public void testArrayVariable() {
        runTest("array_var.bas", "array_var.bas.output");
    }

    @Test
    public void testWhile() {
        runTest("while.bas", "while.bas.output");
    }

    @Test
    public void testExpr() {
        runTest("expr.bas", "expr.bas.output");
    }

    @Test
    public void testFunc() {
        runTest("func.bas", "func.bas.output");
    }

    @Test
    public void testIf() {
        runTest("if.bas", "if.bas.output");
    }

    @Test
    public void testReadData() {
        runTest("readdata.bas", "readdata.bas.output");
    }

    @Test
    public void testGosub() {
        runTest("gosub.bas", "gosub.bas.output");
    }

    @Test
    public void testDef() {
        runTest("def.bas", "def.bas.output");
    }

    @Test
    public void testUdf() {
        runTest("udf.bas", "udf.bas.output");
    }

    @Test
    public void testStrStmt() {
        runTest("strstmt.bas", "strstmt.bas.output");
    }

    @Test
    public void testPrintUsing() {
        runTest("printusing.bas", "printusing.bas.output");
    }

    @Test
    public void testWrite() {
        runTest("write.bas", "write.bas.output");
    }

    @Test
    public void testSwap() {
        runTest("swap.bas", "swap.bas.output");
    }

    @Test
    public void testRandomAccessFile() throws IOException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        String filename = "puffin_basic_test_random_access_file_"
                + Instant.now().getEpochSecond() + ".data";
        env.set("TEST_TMP_DIR", tmpdir);
        env.set("TEST_FILENAME", filename);
        runTest("randomaccessfile.bas", "randomaccessfile.bas.output");
        Files.delete(Path.of(tmpdir, filename));
    }

    @Test
    public void testSequentialAccessFile() throws IOException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        String filename = "puffin_basic_test_sequential_access_file_"
                + Instant.now().getEpochSecond() + ".data";
        env.set("TEST_TMP_DIR", tmpdir);
        env.set("TEST_SEQ_FILENAME", filename);
        runTest("sequentialaccessfile.bas", "sequentialaccessfile.bas.output");
        Files.delete(Path.of(tmpdir, filename));
    }

    private void runTest(String source, String output) {
        var bos = new ByteArrayOutputStream();
        var out = new PrintStream(bos);
        interpretAndRun(
                UserOptions.ofTest(),
                loadSourceCodeFromResource(source),
                out,
                env);
        out.close();

        assertEquals(
                loadOutputFromResource(output),
                new String(bos.toByteArray())
        );
    }

    private String loadSourceCodeFromResource(String filename) {
        return PuffinBasicInterpreterMain.loadSource(
                getClass().getClassLoader().getResource(filename).getFile());
    }

    private String loadOutputFromResource(String filename) {
        return loadOutput(
                getClass().getClassLoader().getResource(filename).getFile());
    }

    private static String loadOutput(String filename) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(filename))) {
            return new String(in.readAllBytes());
        } catch (IOException e) {
            throw new PuffinBasicRuntimeError(
                    IO_ERROR,
                    "Failed to read source code: " + filename + ", error: " + e.getMessage()
            );
        }
    }
}