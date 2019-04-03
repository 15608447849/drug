package redis.annation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DictCacheField {
	GetDictWay dictWay() default GetDictWay.ID;
	String type() default "";
	String reflectcolumn() default "";
}
