package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.specs.util.SpecsIo;

public class AnalysisTest {
    private static void noErrors(String code) {
        var result = TestUtils.analyse(code);
        TestUtils.noErrors(result);
        // System.out.println(result.getSymbolTable().print());
    }

    private static void mustFail(String code) {
        var result = TestUtils.analyse(code);
        TestUtils.mustFail(result);
        for (var report : result.getReports())
            System.out.println(report);

        // System.out.println(result.getSymbolTable().print());
    }

    /*
     * Code that must be successfully analysed
     */

    @Test
    public void helloWorld() {
        noErrors(SpecsIo.getResource("fixtures/public/general/HelloWorld.jmm"));
    }

    @Test
    public void findMaximum() {
        noErrors(SpecsIo.getResource("fixtures/public/general/FindMaximum.jmm"));
    }

    @Test
    public void lazysort() {
        noErrors(SpecsIo.getResource("fixtures/public/general/Lazysort.jmm"));
    }

    @Test
    public void life() {
        noErrors(SpecsIo.getResource("fixtures/public/general/Life.jmm"));
    }

    // @Test // TODO Method overloading is not to be implemented
    public void quickSort() {
        noErrors(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
    }

    @Test
    public void simple() {
        noErrors(SpecsIo.getResource("fixtures/public/general/Simple.jmm"));
    }

    @Test
    public void ticTacToe() {
        noErrors(SpecsIo.getResource("fixtures/public/general/TicTacToe.jmm"));
    }

    @Test
    public void whileAndIf() {
        noErrors(SpecsIo.getResource("fixtures/public/general/WhileAndIf.jmm"));
    }



    /*
     * Code that must fail
     */

    @Test
    public void arr_index_not_int() {
        mustFail(SpecsIo.getResource("fixtures/public/fail/semantic/arr_index_not_int.jmm"));
    }

    @Test
    public void arr_size_not_int() {
        mustFail(SpecsIo.getResource("fixtures/public/fail/semantic/arr_size_not_int.jmm"));
    }

    @Test
    public void badArguments() {
        mustFail(SpecsIo.getResource("fixtures/public/fail/semantic/badArguments.jmm"));
    }

    @Test
    public void binop_incomp() {
        mustFail(SpecsIo.getResource("fixtures/public/fail/semantic/binop_incomp.jmm"));
    }

    @Test
    public void funcNotFound() {
        mustFail(SpecsIo.getResource("fixtures/public/fail/semantic/funcNotFound.jmm"));
    }

    @Test
    public void simple_length() {
        mustFail(SpecsIo.getResource("fixtures/public/fail/semantic/simple_length.jmm"));
    }

    @Test
    public void var_exp_incomp() {
        mustFail(SpecsIo.getResource("fixtures/public/fail/semantic/var_exp_incomp.jmm"));
    }

    @Test
    public void var_lit_incomp() {
        mustFail(SpecsIo.getResource("fixtures/public/fail/semantic/var_lit_incomp.jmm"));
    }

    @Test
    public void var_undef() {
        mustFail(SpecsIo.getResource("fixtures/public/fail/semantic/var_undef.jmm"));
    }

    @Test
    public void varNotInit() {
        mustFail(SpecsIo.getResource("fixtures/public/fail/semantic/varNotInit.jmm"));
    }
}
