
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class FileTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
        	{ true, "rules.txt" },
        });
    }

    private final boolean testValid;
    private final String testString;

    public FileTest(boolean testValid, String testString) {
        this.testValid = testValid;
        this.testString = testString;
    }

    @Test
    public void testRule() throws IOException {
    	InputStream inputFile = getClass().getResourceAsStream(this.testString);
    	
        ANTLRInputStream input = new ANTLRInputStream(inputFile);
        RuleSetGrammarLexer lexer = new RuleSetGrammarLexer(input);
        TokenStream tokens = new CommonTokenStream(lexer);

        RuleSetGrammarParser parser = new RuleSetGrammarParser(tokens);

        parser.removeErrorListeners();
        //parser.setErrorHandler(new ExceptionThrowingErrorHandler());

        if (this.testValid) {
            ParserRuleContext ruleContext = parser.rule_set();
            assertNull(ruleContext.exception);
        } else {
            try {
                ParserRuleContext ruleContext = parser.rule_set();
                fail("Failed on \"" + this.testString + "\"");
                assertNotNull(ruleContext);
            } catch (RuntimeException e) {
                // deliberately do nothing
            }
        }
    }
}