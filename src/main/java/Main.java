
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
import org.antlr.v4.runtime.tree.ParseTree;
import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.CompoundPredicate.BooleanOperator;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningField.UsageType;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimplePredicate.Operator;
import org.dmg.pmml.rule_set.Rule;
import org.dmg.pmml.rule_set.RuleSet;
import org.dmg.pmml.rule_set.RuleSetModel;
import org.dmg.pmml.rule_set.SimpleRule;

public class Main {

	public static void main(String[] args) throws JAXBException, IOException, ConvertToPredicateException, ConvertToOperatorException {
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
        

        
        
        
        PMML pmml = createModelFromRuleContext(ruleContext);
        
        // create JAXB context and instantiate marshaller
        JAXBContext context = JAXBContext.newInstance(PMML.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // Write to System.out
        m.marshal(pmml, System.out);

	}

	private static PMML createModelFromRuleContext(ParserRuleContext ruleContext) throws ConvertToPredicateException, ConvertToOperatorException {
		  PMML pmml = new PMML();
		
		ConvertContext context = new ConvertContext();
		
		RuleSet ruleSet = new RuleSet();
		
		RuleSetModel model = new RuleSetModel();
		model.setMiningFunction(MiningFunction.CLASSIFICATION);
		model.setRuleSet(ruleSet);
		// For each rules
		for (int i = 0; i < ruleContext.getChildCount(); i++) 
		{
			
			// Build rule
			Rule rule = new SimpleRule();
			String id = ruleContext.getChild(i).getChild(0).getChild(0).getText();
			rule.setId(id);
			model.getRuleSet().addRules(rule);
			
			// Add predicate
			ParseTree pred = ruleContext.getChild(i).getChild(1);

			// Convert
			Predicate predicate = convertToPredicate(context, (RuleSetGrammarParser.Logical_exprContext)pred.getChild(2));
			rule.setPredicate(predicate);
		}
		
		
		
		
		addMiningField(context, model);
		
		addDataDictionnary(context, pmml);
		

		pmml.addModels(model);
		
		return pmml;
	}
	
	private static void addDataDictionnary(ConvertContext context, PMML pmml) {
		DataDictionary dataDictionary = new DataDictionary();
		for (FieldName name : context.getFields().values()) {
			dataDictionary.addDataFields(new DataField(name, OpType.CONTINUOUS, DataType.DOUBLE));
		}
		//MiningField target = new MiningField(context.getField("target"));
		//target.setUsageType(UsageType.TARGET);
		//miningSchema.addMiningFields(target); // add special var
		
		// Add field afterward
		pmml.setDataDictionary(dataDictionary);
	}

	private static void addMiningField(ConvertContext context, RuleSetModel model) {
		MiningSchema miningSchema = new MiningSchema();
		for (FieldName name : context.getFields().values()) {
			miningSchema.addMiningFields(new MiningField(name));
		}
		MiningField target = new MiningField(context.getField("target"));
		target.setUsageType(UsageType.TARGET);
		miningSchema.addMiningFields(target); // add special var
		// Add field afterward
		model.setMiningSchema(miningSchema );
	}

	private static Predicate convertToPredicate(ConvertContext context, ParseTree parseTree) throws ConvertToPredicateException, ConvertToOperatorException {
		if (parseTree instanceof RuleSetGrammarParser.LogicalExpressionAndContext) {
			RuleSetGrammarParser.LogicalExpressionAndContext andExpre = (RuleSetGrammarParser.LogicalExpressionAndContext)parseTree;
			CompoundPredicate predicate = new CompoundPredicate(BooleanOperator.AND);
			// add left and right
			predicate.addPredicates(convertToPredicate(context, andExpre.getChild(0)));
			predicate.addPredicates(convertToPredicate(context, andExpre.getChild(2)));
			return predicate;
		}
		
		// ComparisonExpressionContext
		else if (parseTree instanceof RuleSetGrammarParser.ComparisonExpressionContext) {
			// There are only two cases that we can convert
			return convertToPredicate(context, parseTree.getChild(0));
		}
		
		// ComparisonExpressionWithOperatorContext
		else if (parseTree instanceof RuleSetGrammarParser.ComparisonExpressionWithOperatorContext) {
			SimplePredicate predicate = new SimplePredicate();
			predicate.setField(context.getField(parseTree.getChild(0).getText()));
			predicate.setOperator(convertToOperator(parseTree.getChild(1)));
			predicate.setValue(parseTree.getChild(2).getText());
			return predicate;
		}
		
		throw new ConvertToPredicateException();
	}

	private static Operator convertToOperator(ParseTree child) throws ConvertToOperatorException {
		if (child instanceof RuleSetGrammarParser.OperatorEqualContext) {
			return Operator.EQUAL;
		} else if (child instanceof RuleSetGrammarParser.OperatorNotEqualContext) {
			return Operator.NOT_EQUAL;
		} else if (child instanceof RuleSetGrammarParser.OperatorLessThanContext) {
			return Operator.LESS_THAN;
		} else if (child instanceof RuleSetGrammarParser.OperatorLessOrEqualContext) {
			return Operator.LESS_OR_EQUAL;
		} else if (child instanceof RuleSetGrammarParser.OperatorGreaterThanContext) {
			return Operator.GREATER_THAN;
		} else if (child instanceof RuleSetGrammarParser.OperatorGreaterOrEqualContext) {
			return Operator.GREATER_OR_EQUAL;
		} else {
			throw new ConvertToOperatorException();
		}
	}
}
