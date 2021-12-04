package Database;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObjectToDb {
    public <T> void testing(T object) {
        System.out.println(this.getObjectName(object));
        System.out.println("---");
        ArrayList attributes = this.getObjectFieldNames(object);
        attributes.forEach(fieldname -> {
                    try {
                        System.out.println(fieldname + " :: " + this.getTypeOfObjectField(object, fieldname.toString()));
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
        );
        ArrayList methods = this.getObjectsMethods(object);
        methods.forEach(methodName -> {
            System.out.println(methodName);
        });
    }

    /**
     * Returns the Name of the Object
     * @param object
     * @param <T>
     * @return
     */
    public <T> String getObjectName(T object) {
        return object.getClass().getSimpleName();
    }

    /**
     * Returns an ArrayList with the Attribute-Names of the Object
     * @param object
     * @param <T>
     * @return
     */
    public <T> ArrayList<String> getObjectFieldNames(T object) {
        ArrayList<String> arrayList = new ArrayList<>();
        for (Field field: object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            arrayList.add(field.getName());
        }
        return arrayList;
    }

    public <T> ArrayList<Field> getObjectFields(T object) {
        ArrayList<Field> arrayList = new ArrayList<>();
        for (Field field: object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            arrayList.add(field);
        }
        return arrayList;
    }

    /**
     * Returns the Type of the Attribute of a Object
     * @param object
     * @param fieldname
     * @param <T>
     * @return
     * @throws NoSuchFieldException
     */
    public <T> Object getTypeOfObjectField(T object, String fieldname) throws NoSuchFieldException {
        for (Field field: object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if(field.getName()==fieldname) {
                return field.getType();
            }
        }
        return "";
    }

    public <T> Field getFieldOfObjectByName(T object, String fieldName) {
        for (Field field : this.getObjectFields(object)) {
            field.setAccessible(true);
            if(field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public <T> ArrayList<String> getObjectsMethods(T object) {
        ArrayList<String> methods = new ArrayList<>();
        for (Method method : object.getClass().getMethods()) {
            method.setAccessible(true);
            methods.add(method.getName());
        }
        return methods;
    }

    public <T> Method getObjectMethod(T object, String methodName) throws NoSuchMethodException {
        for (Method method : object.getClass().getMethods()) {
            method.setAccessible(true);
            if(method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
//        return object.getClass().getMethod(methodName);
    }

    /**
     * Gets the Annotation-Info from Field
     * @param field
     * @return
     */
    public DBField getDbFieldFromField(Field field) {
        return field.getAnnotation(DBField.class);
    }
}
