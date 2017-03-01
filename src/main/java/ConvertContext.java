import java.util.LinkedHashMap;
import java.util.Map;

import org.dmg.pmml.FieldName;

public class ConvertContext {
	private Map<String, FieldName> fields = new LinkedHashMap<>();

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
}
