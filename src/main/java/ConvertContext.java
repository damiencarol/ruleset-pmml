import java.util.LinkedHashMap;
import java.util.Map;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;

public class ConvertContext {
	private static final DataType DEFAULT_DATATYPE = DataType.DOUBLE;

	private String targetVarName = "target";
	private OpType targetOpType = OpType.CATEGORICAL;
	private DataType targetDataType = DataType.STRING;
	
	private Map<String, FieldName> fields = new LinkedHashMap<>();
	private Map<String, DataType> fieldTypes = new LinkedHashMap<>();

	public Map<String, FieldName> getFields() {
		return fields;
	}
	
	public synchronized FieldName getField(String key) {
		FieldName field = fields.get(key);
		if (field==null) {
			field = new FieldName(key);
			fields.put(key, field);
		}
		return field;
	}
	
	public DataType getFieldType(String key) {
		if (fieldTypes.containsKey(key)) {
			return fieldTypes.get(key);
		}
		return DEFAULT_DATATYPE;
	}

	public void setFieldType(String key, DataType type) throws DataTypeConsistencyException {
		if (!fieldTypes.containsKey(key)) {
			fieldTypes.put(key, type);
		} else {
			DataType datatype = fieldTypes.get(key);
			if (datatype!=type) {
				throw new DataTypeConsistencyException();
			}
		}
	}

	public String getTargetVarName() {
		return targetVarName;
	}

	public void setTargetVarName(String targetVarName) {
		this.targetVarName = targetVarName;
	}

	public OpType getTargetOpType() {
		return targetOpType;
	}

	public void setTargetOpType(OpType targetOpType) {
		this.targetOpType = targetOpType;
	}

	public DataType getTargetDataType() {
		return targetDataType;
	}

	public void setTargetDataType(DataType targetDataType) {
		this.targetDataType = targetDataType;
	}
}
