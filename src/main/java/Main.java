
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.rule_set.Rule;
import org.dmg.pmml.rule_set.RuleSet;
import org.dmg.pmml.rule_set.RuleSetModel;
import org.dmg.pmml.rule_set.SimpleRule;

public class Main {

	public static void main(String[] args) throws JAXBException, IOException {
		// Read the file from parameters
		InputStream inputStream = new FileInputStream(new File(args[0]));
		
		// Parse
		ANTLRInputStream input = new ANTLRInputStream(inputStream );
        RuleSetGrammarLexer lexer = new RuleSetGrammarLexer(input);
        TokenStream tokens = new CommonTokenStream(lexer);
        
        RuleSetGrammarParser parser = new RuleSetGrammarParser(tokens);
        //parser.removeErrorListeners();
        //parser.setErrorHandler(new ExceptionThrowingErrorHandler());
        

        ParserRuleContext ruleContext = parser.rule_set();
        //assertNull(ruleContext.exception);

        /*System.out.println("child : " + ruleContext.getChildCount());
        System.out.println("child : " + ruleContext.getChild(0).getText());
        System.out.println("child : " + ruleContext.getChild(1).getText());*/

        //show AST in console
        //System.out.println(parser.rule_set().toStringTree(parser));
        

        
        
        
        PMML pmml = new PMML();
		pmml.addModels(createModelFromRuleContext(ruleContext));
        
        // create JAXB context and instantiate marshaller
        JAXBContext context = JAXBContext.newInstance(PMML.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // Write to System.out
        m.marshal(pmml, System.out);

	}

	private static Model createModelFromRuleContext(ParserRuleContext ruleContext) {
		RuleSet ruleSet = new RuleSet();
		
		RuleSetModel model = new RuleSetModel();
		model.setRuleSet(ruleSet);
		
		for (int i = 0; i < ruleContext.getChildCount(); i++) 
		{
			
			// Build rule
			Rule rule = new SimpleRule();
			String id = ruleContext.getChild(i).getChild(0).getChild(0).getText();
			rule.setId(id);
			model.getRuleSet().addRules(rule);
		}
		
		return model;
	}

}
