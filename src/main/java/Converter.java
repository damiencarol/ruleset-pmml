
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.dmg.pmml.Application;
import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.CompoundPredicate.BooleanOperator;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Extension;
import org.dmg.pmml.False;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldUsageType;
import org.dmg.pmml.Header;
import org.dmg.pmml.MiningBuildTask;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.RuleSelectionMethod;
import org.dmg.pmml.RuleSelectionMethod.Criterion;
import org.dmg.pmml.RuleSet;
import org.dmg.pmml.RuleSetModel;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimplePredicate.Operator;
import org.dmg.pmml.SimpleRule;
import org.dmg.pmml.Timestamp;
import org.dmg.pmml.True;

public class Converter {

	private static final String PMML_VERSION = "4.2";
	private static final String APPLICATION_VERSION = "1.0";
	private static final String APPLICATION_NAME = "RulesetPmml";

	public static PMML createModelFromRuleContext(ParserRuleContext ruleContext, Criterion ruleSelectionMethodCriterion, String defaultscore)
			throws ConvertToPredicateException, ConvertToOperatorException, DataTypeConsistencyException, IOException {
		return createModelFromRuleContext(ruleContext, null, ruleSelectionMethodCriterion, defaultscore);
	}

	public static PMML createModelFromRuleContext(ParserRuleContext ruleContext, String path, Criterion ruleSelectionMethodCriterion, String defaultScore)
			throws ConvertToPredicateException, ConvertToOperatorException, DataTypeConsistencyException, IOException {
		PMML pmml = new PMML();
		pmml.setVersion(PMML_VERSION);

		// Add header
		Header header = new Header();
		addTimestampNow(header);
		addApplication(header);
		pmml.setHeader(header);

		// MiningBuildTask
		if (path != null) {
			addMiningBuildTask(pmml, path);
		}

		ConvertContext context = new ConvertContext();

		RuleSet ruleSet = new RuleSet();
		RuleSelectionMethod ruleSelectionMethod = new RuleSelectionMethod();
		ruleSelectionMethod.setCriterion(ruleSelectionMethodCriterion);
		ruleSet.addRuleSelectionMethods(ruleSelectionMethod);

		RuleSetModel model = new RuleSetModel();
		// FIXME change for 4.2/4.3
		// model.setMiningFunction(MiningFunction.CLASSIFICATION);
		model.setFunctionName(MiningFunctionType.CLASSIFICATION);
		model.setRuleSet(ruleSet);
		// For each rules
		for (int i = 0; i < ruleContext.getChildCount(); i++) {

			// Build rule
			SimpleRule rule = new SimpleRule();
			String id = ruleContext.getChild(i).getChild(0).getChild(0).getText();
			rule.setId(id);
			model.getRuleSet().addRules(rule);

			// declare result
			rule.setScore(ruleContext.getChild(i).getChild(2).getChild(2).getText());

			// Add predicate
			ParseTree pred = ruleContext.getChild(i).getChild(1);

			// Convert
			Predicate predicate = convertToPredicate(context,
					(RuleSetGrammarParser.Logical_exprContext) pred.getChild(2));
			rule.setPredicate(predicate);
		}
		// Set the default score
		ruleSet.setDefaultScore(defaultScore);

		addMiningField(context, model);

		addDataDictionnary(context, pmml);

		pmml.addModels(model);

		return pmml;
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	private static void addMiningBuildTask(PMML pmml, String path) throws IOException {
		MiningBuildTask miningBuildTask = new MiningBuildTask();

		Extension extension = new Extension();

		extension.addContent(readFile(path, StandardCharsets.UTF_8));

		miningBuildTask.addExtensions(extension);

		pmml.setMiningBuildTask(miningBuildTask);
	}

	private static void addApplication(Header header) {
		Application application = new Application();
		application.setName(APPLICATION_NAME);
		application.setVersion(APPLICATION_VERSION);
		header.setApplication(application);
	}

	private static void addTimestampNow(Header header) {
		Timestamp timestamp = new Timestamp();
		SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
		Date now = new Date(System.currentTimeMillis());
		timestamp.addContent(ISO8601DATEFORMAT.format(now));
		header.setTimestamp(timestamp);
	}

	private static void addDataDictionnary(ConvertContext context, PMML pmml) {
		DataDictionary dataDictionary = new DataDictionary();
		for (FieldName name : context.getFields().values()) {
			DataField dataField = new DataField(name, OpType.CONTINUOUS, DataType.DOUBLE);
			dataField.setDataType(context.getFieldType(name.getValue()));
			dataDictionary.addDataFields(dataField);
		}

		// DataField dataFieldTarget = new DataField("target",
		// OpType.CONTINUOUS, DataType.STRING);

		// Add target
		DataField targetDataField = new DataField(new FieldName(context.getTargetVarName()), context.getTargetOpType(),
				context.getTargetDataType());
		dataDictionary.addDataFields(targetDataField);

		// Add field afterward
		pmml.setDataDictionary(dataDictionary);
	}

	private static void addMiningField(ConvertContext context, RuleSetModel model) {
		MiningSchema miningSchema = new MiningSchema();
		for (FieldName name : context.getFields().values()) {
			MiningField miningField = new MiningField(name);
			miningSchema.addMiningFields(miningField);
		}

		// Add target
		MiningField target = new MiningField(new FieldName(context.getTargetVarName()));
		target.setUsageType(FieldUsageType.TARGET);
		miningSchema.addMiningFields(target); // add special var

		// Add field afterward
		model.setMiningSchema(miningSchema);
	}

	private static Predicate convertToPredicate(ConvertContext context, ParseTree parseTree)
			throws ConvertToPredicateException, ConvertToOperatorException, DataTypeConsistencyException {
		if (parseTree instanceof RuleSetGrammarParser.LogicalExpressionAndContext) {
			RuleSetGrammarParser.LogicalExpressionAndContext andExpre = (RuleSetGrammarParser.LogicalExpressionAndContext) parseTree;
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
			String key = parseTree.getChild(0).getText();

			predicate.setField(context.getField(key));
			predicate.setOperator(convertToOperator(parseTree.getChild(1)));

			// Check if it's a string
			// System.out.println(parseTree.getChild(2).getClass().getName());
			if (parseTree.getChild(2) instanceof RuleSetGrammarParser.ComparisonOperandStringContext) {
				context.setFieldType(key, DataType.STRING);
				String val = parseTree.getChild(2).getText().substring(1);
				predicate.setValue(val.substring(0, val.length() - 1));
			} else if (parseTree.getChild(2) instanceof RuleSetGrammarParser.ComparisonOperandExprContext) {
				context.setFieldType(key, DataType.DOUBLE);
				predicate.setValue(parseTree.getChild(2).getText());
			}

			return predicate;
		}

		else if (parseTree instanceof RuleSetGrammarParser.LogicalEntityContext) {
			ParseTree child = parseTree.getChild(0);
			if (child instanceof RuleSetGrammarParser.LogicalTrueConstContext) {
				return new True();
			} else if (child instanceof RuleSetGrammarParser.LogicalFalseConstContext) {
				return new False();
			}
			throw new ConvertToPredicateException();
		}

		// OR
		else if (parseTree instanceof RuleSetGrammarParser.LogicalExpressionOrContext) {
			ParseTree childLeft = parseTree.getChild(0);
			ParseTree childRight = parseTree.getChild(2);

			CompoundPredicate predicate = new CompoundPredicate();
			predicate.setBooleanOperator(BooleanOperator.OR);
			predicate.addPredicates(convertToPredicate(context, childLeft));
			predicate.addPredicates(convertToPredicate(context, childRight));

			return predicate;
		}

		// paren
		else if (parseTree instanceof RuleSetGrammarParser.LogicalExpressionInParenContext) {
			return convertToPredicate(context, parseTree.getChild(1));
		}
		// paren 2
		else if (parseTree instanceof RuleSetGrammarParser.ComparisonExpressionParensContext) {
			return convertToPredicate(context, parseTree.getChild(1));
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
