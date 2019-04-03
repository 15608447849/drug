package redis.annation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheField {
	String type() default "string";
	String prefix() default "";
	String key() default "";
	String cachecolumn() default "";
	String reflectcolumn() default "";
}
