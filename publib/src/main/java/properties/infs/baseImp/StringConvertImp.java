package properties.infs.baseImp;

import properties.infs.FieldConvert;

import java.lang.reflect.Field;

public class StringConvertImp implements FieldConvert {
    @Override
    public void setValue(Object holder, Field f, Object v) throws IllegalArgumentException, IllegalAccessException {
        f.set(holder, String.valueOf(v.toString()));
    }
}
