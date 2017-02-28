
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class PredicateTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            /* Valid rules. */
        	{ true, "PREDICATE : BP>3 " },
        	{ true, "PREDICATE : BP>3 AND var2 < 3 " },
        	{ true, "PREDICATE : BP=\"HIGH\" AND var2 < 3  " },
        	{ true, "PREDICATE: BP=\"HIGH\" AND K > 0.045804001 AND Age <= 50 AND Na <= 0.77240998" },
            
        	/* Invalid rules. */
            { false, "crap crap crap" }, // no operator but 3 operands
            { false, "PREDICATE: junk < <" }, // 2 operators

        });
    }

    private final boolean testValid;
    private final String testString;

    public PredicateTest(boolean testValid, String testString) {
        this.testValid = testValid;
        this.testString = testString;
    }

    @Test
    public void testRule() {
        ANTLRInputStream input = new ANTLRInputStream(this.testString);
        RuleSetGrammarLexer lexer = new RuleSetGrammarLexer(input);
        TokenStream tokens = new CommonTokenStream(lexer);

        RuleSetGrammarParser parser = new RuleSetGrammarParser(tokens);

        parser.removeErrorListeners();
        parser.setErrorHandler(new ExceptionThrowingErrorHandler());

        if (this.testValid) {
            ParserRuleContext ruleContext = parser.rule_predicate();
            assertNull(ruleContext.exception);
        } else {
            try {
                ParserRuleContext ruleContext = parser.rule_predicate();
                fail("Failed on \"" + this.testString + "\"");
            } catch (RuntimeException e) {
                // deliberately do nothing
            }
        }
    }
}