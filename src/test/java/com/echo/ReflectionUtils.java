package com.echo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * Utility class providing consistent methods for accessing private fields and methods in tests, mainly using reflection
 * 
 * FUTURE - Separate reflection methods into a separate util class, add methods here
 */
public class ReflectionUtils {

    /**
     * Helper method to get a field value using reflection.
     * This method will search for the field in the class hierarchy if it's not found in the immediate class.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object object, String fieldName) {
        try {
            // Try to find the field in the class or its superclasses
            Field field = findField(object.getClass(), fieldName);
            if (field == null) {
                fail("Field not found: " + fieldName);
                return null;
            }

            // Set the field accessible and return its value
            field.setAccessible(true);
            return (T) field.get(object);

        } catch (Exception e) {
            fail("Failed to access field " + fieldName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to get a static field value using reflection.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getStaticFieldValue(Class<?> givenClass, String fieldName) {
        try {
            Field field = givenClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(null); // null because it's a static field
        } catch (Exception e) {
            fail("Failed to access static field " + fieldName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to set a field value using reflection.
     * This method will search for the field in the class hierarchy if it's not found in the immediate class.
     */
    public static void setFieldValue(Object object, String fieldName, Object value) {
        try {
            // Try to find the field in the class or its superclasses
            Field field = findField(object.getClass(), fieldName);
            if (field == null) {
                fail("Field not found: " + fieldName);
                return;
            }

            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            fail("Failed to set field " + fieldName + ": " + e.getMessage());
        }
    }

    /**
     * Helper method to find a field in a class or its superclasses.
     * @param givenClass The class to start searching from.
     * @param fieldName The name of the field to find.
     * @return The Field object if found, or null if not found.
     */
    private static Field findField(Class<?> givenClass, String fieldName) {
        // Try to find the field in the current class
        try {
            return givenClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // If not found, check the superclass
            Class<?> superClass = givenClass.getSuperclass();
            if (superClass != null) {
                return findField(superClass, fieldName);
            }
            return null;
        }
    }

     /**
     * Helper method to find a method in a class or its superclasses.
     */
    private static Method findMethod(Class<?> givenClass, String methodName) {
        // Try to find the method in the current class
        try {
            return givenClass.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            // If not found, check the superclass
            Class<?> superClass = givenClass.getSuperclass();
            if (superClass != null) {
                return findMethod(superClass, methodName);
            }
            return null;
        }
    }

    /**
     * Helper method to find a method with matching parameters in a class or its superclasses.
     */
    private static Method findMethodWithParams(Class<?> givenClass, String methodName, Object... args) {
        // Try to find the method in the current class
        Method[] methods = givenClass.getDeclaredMethods();

        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                boolean matches = true;
                Class<?>[] paramTypes = method.getParameterTypes();

                for (int i = 0; i < args.length; i++) {
                    if (args[i] != null && !paramTypes[i].isAssignableFrom(args[i].getClass())) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    return method;
                }
            }
        }

        // If not found, check the superclass
        Class<?> superClass = givenClass.getSuperclass();
        if (superClass != null) {
            return findMethodWithParams(superClass, methodName, args);
        }

        return null;
    }

    /**
     * Helper method to invoke a method with no parameters.
     * This method will search for the method in the class hierarchy if it's not found in the immediate class.\
     * @param target The object on which to invoke the method.
     * @param methodName The name of the method to invoke.
     */
    public static void invokeMethod(Object target, String methodName) throws Exception {
        // Try to find the method in the class or its superclasses
        Method method = findMethod(target.getClass(), methodName);
        if (method == null) {
            fail("Method not found: " + methodName);
            return;
        }

        method.setAccessible(true);
        method.invoke(target);
    }

    /**
     * Helper method to invoke a method with parameters.
     * This method will search for the method in the class hierarchy if it's not found in the immediate class.
     * 
     * @param target The object on which to invoke the method.
     * @param methodName The name of the method to invoke.
     * @param args The arguments to pass to the method.
     */
    public static void invokeMethod(Object target,String methodName,Object... args) throws Exception{
        // Find the method with matching parameter types in the class hierarchy
        Method methodToInvoke = findMethodWithParams(target.getClass(), methodName, args);

        if (methodToInvoke == null) {
            fail("Could not find method " + methodName + " with matching parameter types");
            return;
        }

        methodToInvoke.setAccessible(true);
        methodToInvoke.invoke(target, args);
    }

   
}
