package properties.infs.baseImp;

import properties.infs.FieldConvert;

import java.lang.reflect.Field;


public class FloatConvertImp implements FieldConvert {

	@Override
	public void setValue(Object holder, Field f, Object v)
			throws IllegalArgumentException, IllegalAccessException {
		f.set(holder, Float.valueOf(v.toString()));
	}

}
