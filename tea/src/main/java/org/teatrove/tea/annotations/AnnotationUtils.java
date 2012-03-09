package org.teatrove.tea.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationUtils {
    private AnnotationUtils() {
        super();
    }

    public static <T extends Annotation>
    T findAnnotation(Class<T> clazz, Method method) {
        return findAnnotation(clazz, method.getDeclaringClass(),
                              method.getName(), method.getParameterTypes());
    }

    public static <T extends Annotation>
    T findAnnotation(Class<T> annotationClass, Class<?> clazz,
                     String methodName, Class<?>[] paramTypes) {
        T annotation = null;

        try {
            // check method itself
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            annotation = method.getAnnotation(annotationClass);
            if (annotation != null) { return annotation; }
        }
        catch (NoSuchMethodException nsme) {
            // ignore
        }

        // check class itself
        annotation = clazz.getAnnotation(annotationClass);
        if (annotation != null) { return annotation; }

        // check parent class
        Class<?> parent = clazz.getSuperclass();
        if (parent != null && Object.class != parent) {
            annotation =
                findAnnotation(annotationClass, parent, methodName, paramTypes);
            if (annotation != null) { return annotation; }
        }

        // check interfaces
        Class<?>[] ifaces = clazz.getInterfaces();
        for (Class<?> iface : ifaces) {
            annotation =
                findAnnotation(annotationClass, iface, methodName, paramTypes);
            if (annotation != null) { return annotation; }
        }

        // none found
        return null;
    }
}
