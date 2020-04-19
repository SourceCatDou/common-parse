package org.osource.scd.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chengdu
 *
 */
public class ReflectUtil {

    private static final  Map<Class<?>, Field[]> DECLARED_FIELDS_CACHE = new ConcurrentHashMap<>(1024);

    private static final  Map<Class<?>, Method[]> DECLARED_METHODS_CACHE = new ConcurrentHashMap<>(1024);

    private static final  Map<Class<?>, List<Method>> BEAN_METHOD_CACHE = new ConcurrentHashMap<>(1024);

    private static final  Map<Class<?>, Map<String, Method>> BEAN_FIELD_SETTER_CACHE = new ConcurrentHashMap<>(1024);

    private static final String SET_METHOD_PREFIX = "set";

    public static Field[] getClassField(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("input param is null");
        }
        Field[] fields = DECLARED_FIELDS_CACHE.get(clazz);
        if (fields == null) {
            fields = clazz.getDeclaredFields();
            DECLARED_FIELDS_CACHE.put(clazz, fields);
        }
        return fields;
    }

    /**
     * exclude Object class
     * @param clazz clazz
     * @return this class field list
     */
    public static List<Field[]> getAllFields(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("input param is null");
        }
        List<Field[]> allFields = new ArrayList<>();
        while(clazz != Object.class) {
            Field[] fields = getClassField(clazz);
            if (fields != null && fields.length > 0) {
                allFields.add(fields);
            }
            clazz = clazz.getSuperclass();
        }
        return allFields;
    }

    public static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) ||
                !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
                Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    public static Field findField(Class<?> clazz, String name, Class<?> type) {
        if (clazz == null || name == null || type == null) {
            return null;
        }
        List<Field[]> fieldList = getAllFields(clazz);
        for (Field[] fieldArr : fieldList) {
            for (Field field : fieldArr) {
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                if (name.equals(fieldName) && type.equals(fieldType)) {
                    return field;
                }
            }
        }
        return null;
    }

    public static Method[] getClassMethod(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("input param class is null");
        }
        Method[] declareMethods = DECLARED_METHODS_CACHE.get(clazz);
        if (declareMethods == null) {
            declareMethods = clazz.getDeclaredMethods();
            DECLARED_METHODS_CACHE.put(clazz, declareMethods);
        }
        return declareMethods;
    }

    /**
     * get all methods, but excluding
     * Object class
     * @param clazz clazz
     * @return clazz method
     */
    public static List<Method[]> getAllMethods(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("input param class is null");
        }
        List<Method[]> allMethods = new ArrayList<>();
        while (clazz != Object.class) {
            Method[] methods = getClassMethod(clazz);
            if (methods != null && methods.length > 0) {
                allMethods.add(methods);
            }
            clazz = clazz.getSuperclass();
        }
        return allMethods;
    }

    public static Method findMethod(Class<?> clazz, String name, Class<?>[] paramTypes) {
        if (clazz == null || name == null || name.length() == 0) {
            throw new IllegalArgumentException("input param error");
        }
        List<Method[]> allMethods = getAllMethods(clazz);
        for (Method[] methodArr : allMethods) {
            for (Method method : methodArr) {
                String methodName = method.getName();
                Class<?>[] methodParamTypes = method.getParameterTypes();
                boolean nameEquals = name.equals(methodName);
                boolean isEqualTypes = isEqualTypes(paramTypes, methodParamTypes);
                if (nameEquals && isEqualTypes) {
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean isEqualTypes(Class<?>[] paramTypes, Class<?>[] methodParamTypes) {
        int paramLen = paramTypes.length;
        int methodParamLen = methodParamTypes.length;
        if (paramLen != methodParamLen) {
            return false;
        }
        for(int i = 0; i < paramLen; i++) {
            if (!paramTypes[i].equals(methodParamTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * model method, setXX
     * @param clazz clazz
     * @return method list
     */
    public static List<Method> getBeanMethods(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("input param class is null");
        }
        List<Method> allBeanMethods = BEAN_METHOD_CACHE.get(clazz);
        if (allBeanMethods == null) {
            allBeanMethods = new ArrayList<>();
            Class<?> tempClazz = clazz;
            while (tempClazz != Object.class) {
                Method[] methods = getClassMethod(tempClazz);
                if (methods != null && methods.length > 0) {
                    List<Method> beanMethods = findBeanMethod(methods);
                    allBeanMethods.addAll(beanMethods);
                }
                tempClazz = tempClazz.getSuperclass();
            }
            BEAN_METHOD_CACHE.put(clazz, allBeanMethods);
        }
        return allBeanMethods;
    }

    /**
     * find setXX method
     * @param methods methods
     * @return method list
     */
    private static List<Method> findBeanMethod(Method[] methods) {
        List<Method> beanMethods = new ArrayList<>(methods.length);
        for (Method method : methods) {
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (methodName.startsWith(SET_METHOD_PREFIX) && parameterTypes.length == 1) {
                beanMethods.add(method);
            }
        }
        return beanMethods;
    }

    /**
     * field name to field setter
     * @param clazz clazz
     * @return field name to filed setter method
     */
    public static Map<String, Method> getBeanSetterMap(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("input param class is null");
        }
        Map<String, Method> beanSetterMap = BEAN_FIELD_SETTER_CACHE.get(clazz);
        if (beanSetterMap == null) {
            beanSetterMap = new HashMap<>(16);
            Class<?> tempClazz = clazz;
            while (tempClazz != Object.class) {
                Method[] methods = getClassMethod(tempClazz);
                if (methods != null && methods.length > 0) {
                    mapFieldSetter(methods, beanSetterMap);
                }
                tempClazz = tempClazz.getSuperclass();
            }
            BEAN_FIELD_SETTER_CACHE.put(clazz, beanSetterMap);
        }
        return beanSetterMap;
    }

    private static void mapFieldSetter(Method[] methods, Map<String, Method> beanSetterMap) {
        String fieldName;
        for (Method method : methods) {
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (methodName.startsWith(SET_METHOD_PREFIX) && parameterTypes.length == 1) {
                fieldName = methodName.substring(SET_METHOD_PREFIX.length()).toLowerCase();
                beanSetterMap.put(fieldName, method);
            }
        }
    }
}
