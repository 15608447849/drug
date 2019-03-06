package properties.annotations;

import java.lang.annotation.*;

/**
 * 属性文件路径
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited  //可以被继承
public @interface PropertiesFilePath {
    String value();
}
