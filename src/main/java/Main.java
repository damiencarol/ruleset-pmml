
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;

public class Main {

	public static void main(String[] args) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("RULE1 : PREDICATE: BP>3 PREDICTION: a ");
		sb.append("\n");
		sb.append("RULE2: PREDICATE: CPT < 1 PREDICTION: 1.0  ");
		String testString = sb.toString();
		System.out.println(testString);
		
		ANTLRInputStream input = new ANTLRInputStream(testString);
        RuleSetGrammarLexer lexer = new RuleSetGrammarLexer(input);
        TokenStream tokens = new CommonTokenStream(lexer);

        RuleSetGrammarParser parser = new RuleSetGrammarParser(tokens);

        parser.removeErrorListeners();
        parser.setErrorHandler(new ExceptionThrowingErrorHandler());

        

        ParserRuleContext ruleContext = parser.rule_set();
        //assertNull(ruleContext.exception);

        System.out.println("child : " + ruleContext.getChildCount());
        System.out.println("child : " + ruleContext.getChild(0).getText());
        System.out.println("child : " + ruleContext.getChild(1).getText());

        //show AST in console
        System.out.println(parser.rule_set().toStringTree(parser));
        
        
        /*
        ParseTree tree = parser.rule_set(); 
      //show AST in GUI
        JFrame frame = new JFrame("Antlr AST");
        JPanel panel = new JPanel();
        TreeViewer viewr = new  org.antlr.v4.runtime.tree.gui.TreeViewer (Arrays.asList(
                parser.getRuleNames()),tree);
        viewr.setScale(1.5);//scale a little
        panel.add(viewr);
        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200,200);
        frame.setVisible(true);*/
	}

}
