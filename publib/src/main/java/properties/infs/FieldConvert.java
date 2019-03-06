package properties.infs;

import java.lang.reflect.Field;

public interface FieldConvert {
	void setValue(Object holder, Field f, Object v) throws IllegalArgumentException, IllegalAccessException;
}
