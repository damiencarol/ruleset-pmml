
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
import org.dmg.pmml.PMML;

public class Main {

	public static void main(String[] args) throws JAXBException, IOException, ConvertToPredicateException,
			ConvertToOperatorException, DataTypeConsistencyException {
		// Read the file from parameters
		InputStream inputStream = new FileInputStream(new File(args[0]));

		// Parse
		ANTLRInputStream input = new ANTLRInputStream(inputStream);
		RuleSetGrammarLexer lexer = new RuleSetGrammarLexer(input);
		TokenStream tokens = new CommonTokenStream(lexer);

		RuleSetGrammarParser parser = new RuleSetGrammarParser(tokens);
		// parser.removeErrorListeners();
		// parser.setErrorHandler(new ExceptionThrowingErrorHandler());
		ParserRuleContext ruleContext = parser.ruleSet();
		// show AST in console
		// System.out.println(parser.rule_set().toStringTree(parser));
		
		

		PMML pmml = Converter.createModelFromRuleContext(ruleContext, args[0]);

		// create JAXB context and instantiate marshaller
		JAXBContext context = JAXBContext.newInstance(PMML.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		// Write to System.out
		m.marshal(pmml, System.out);
	}
}
