
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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dmg.pmml.PMML;
import org.dmg.pmml.RuleSelectionMethod.Criterion;

public class Main {

	private static final String OPT_SELECTION_METHOD = "selectionmethod";
	private static final String OPT_DEFAULT_SCORE = "defaultscore";

	public static void main(String[] args) throws JAXBException, IOException, ConvertToPredicateException,
			ConvertToOperatorException, DataTypeConsistencyException, ParseException {
		// create Options object
		Options options = new Options();
		// add t option
		options.addOption("t", false, "display current time");

		options.addOption(OPT_DEFAULT_SCORE, true, "display current time");
		options.addOption(OPT_SELECTION_METHOD, true, "display current time");
		
		CommandLineParser parser = new GnuParser();
		CommandLine cmd = parser.parse( options, args);
		// convert
		convert(cmd.getArgs()[0], 
				Criterion.fromValue(cmd.getOptionValue(OPT_SELECTION_METHOD, "firstHit")),
				cmd.getOptionValue(OPT_DEFAULT_SCORE, "0.0"));
	}

	public static void convert(final String filePath, Criterion ruleSelectionMethod, String defaultScore) throws JAXBException, IOException, ConvertToPredicateException,
			ConvertToOperatorException, DataTypeConsistencyException {
		// Read the file from parameters
		InputStream inputStream = new FileInputStream(new File(filePath));

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

		PMML pmml = Converter.createModelFromRuleContext(ruleContext, filePath, ruleSelectionMethod, defaultScore);

		// create JAXB context and instantiate Marshaller
		JAXBContext context = JAXBContext.newInstance(PMML.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		// Write to System.out
		m.marshal(pmml, System.out);
	}
}
