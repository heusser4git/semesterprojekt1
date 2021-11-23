package Database;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface DBField {
    String name() default "";
    Class< ?> type() default Integer.class;
    String datatype() default "";
    String datatypesize() default "";
    boolean useQuotes() default false;
    boolean isAutoincrement() default false;
    boolean isPrimaryKey() default false;
    boolean isForeignKey() default false;
    String foreignTable() default "";
    String foreignTableColumn() default "";
    boolean isNotInDb() default false;
}
