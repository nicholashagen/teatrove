package org.teatrove.tea.compiler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.teatrove.tea.annotations.AnnotationUtils;
import org.teatrove.tea.annotations.Dependencies;
import org.teatrove.tea.annotations.Parser;
import org.teatrove.tea.annotations.Priority;
import org.teatrove.trove.classfile.Modifiers;

public class Parsers {
    /** State of whether tokens have been loaded. */
    private boolean mLoaded;

    /** Annotation database and indexing. */
    private Annotations mAnnotations;

    /** List of parser instances. */
    private Map<Class<?>, Object> mInstances = new HashMap<Class<?>, Object>();

    /** List of supported parser annotations. */
    private Map<Class<? extends Annotation>, Set<Class<? extends Annotation>>> mParserAnnotations =
        new HashMap<Class<? extends Annotation>, Set<Class<? extends Annotation>>>();

    /** List of definitions. */
    private Map<Class<? extends Annotation>, Set<ParserDefinition>> mDefinitions =
        new HashMap<Class<? extends Annotation>, Set<ParserDefinition>>();

    /**
     * Default constructor.
     */
    public Parsers(Annotations annotations) {
        mAnnotations = annotations;
    }

    public ParserDefinition[] getParsers(Class<?> clazz) {
        return getParsers(clazz, true);
    }

    public ParserDefinition[] getParsers(Class<?> clazz,
                                         boolean includeSubclasses) {
        return getParsers(Parser.class, clazz, includeSubclasses);
    }

    public ParserDefinition[] getParsers(Class<? extends Annotation> annotation,
                                         Class<?> clazz) {
        return getParsers(annotation, true, clazz, true);
    }

    public ParserDefinition[] getParsers(Class<? extends Annotation> annotation,
                                         Class<?> clazz,
                                         boolean includeSubclasses) {
        return getParsers(annotation, true, clazz, includeSubclasses);
    }

    public ParserDefinition[] getParsers(Class<? extends Annotation> annotation,
                                         boolean includeChildren,
                                         Class<?> clazz,
                                         boolean includeSubclasses) {
        // lookup all definitions
        Set<ParserDefinition> definitions =
            getAnnotations(annotation, includeChildren);

        // search for matching types
        // TODO: cache this result to improve perforance on lookups
        Iterator<ParserDefinition> iterator = definitions.iterator();
        while (iterator.hasNext()) {
            ParserDefinition definition = iterator.next();
            Class<?> returnType = definition.getMethod().getReturnType();
            if ((includeSubclasses && !clazz.isAssignableFrom(returnType)) ||
                (!includeSubclasses && !clazz.equals(returnType))) {
                iterator.remove();
            }
        }

        // return result as array
        return definitions.toArray(new ParserDefinition[definitions.size()]);
    }

    protected
    Set<ParserDefinition> getAnnotations(Class<? extends Annotation> clazz,
                                         boolean includeChildren) {
        // lookup annotations
        Set<ParserDefinition> result = new HashSet<ParserDefinition>();
        getAnnotations(result, clazz, includeChildren);

        // return result
        return result;
    }

    protected void getAnnotations(Set<ParserDefinition> result,
                                  Class<? extends Annotation> clazz,
                                  boolean includeChildren) {
        // look for declared
        Set<ParserDefinition> definitions = mDefinitions.get(clazz);
        if (definitions != null) { result.addAll(definitions); }

        // look for children
        if (includeChildren) {
            Set<Class<? extends Annotation>> children =
                mParserAnnotations.get(clazz);

            if (children != null) {
                for (Class<? extends Annotation> child : children) {
                    getAnnotations(result, child, includeChildren);
                }
            }
        }
    }

    protected
    ParserDefinition[] getDefinitions(Class<?> clazz,
                                      boolean includeSubclasses) {
        // create result
        Set<ParserDefinition> result = new HashSet<ParserDefinition>();

        // lookup all definitions
        Set<ParserDefinition> definitions = new HashSet<ParserDefinition>();
        getAnnotations(definitions, Parser.class, true);

        // search for matching types
        // TODO: cache this result to improve perforance on lookups
        for (ParserDefinition definition : definitions) {
            Class<?> returnType = definition.getMethod().getReturnType();
            if ((includeSubclasses && clazz.isAssignableFrom(returnType)) ||
                (!includeSubclasses && clazz.equals(returnType))) {
                result.add(definition);
            }
        }

        // return result as array
        return result.toArray(new ParserDefinition[result.size()]);
    }

    protected void load() {
        // ignore if already loaded
        if (mLoaded) { return; }

        // load annotations processing errors
        mLoaded = true;
        try { loadAnnotation(org.teatrove.tea.annotations.Parser.class); }
        catch (Exception e) {
            throw new IllegalArgumentException("unable to load parsers", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void loadAnnotation(Class<? extends Annotation> annotationClass)
        throws Exception {

        // add collection of children annotations
        mParserAnnotations.put(annotationClass,
                               new HashSet<Class<? extends Annotation>>());

        // add collection of matching definitions
        mDefinitions.put(annotationClass, new HashSet<ParserDefinition>());

        // search for uses (annotations and methods/classes)
        Class<?>[] classes = mAnnotations.getAnnotations(annotationClass);
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotation()) {
                registerAnnotation(annotationClass,
                                   (Class<? extends Annotation>) clazz);
            }
            else {
                loadParser(annotationClass, clazz);
            }
        }
    }

    protected void registerAnnotation(Class<? extends Annotation> parentClass,
                                      Class<? extends Annotation> annotationClass)
        throws Exception {

        // save child annotation
        mParserAnnotations.get(parentClass).add(annotationClass);

        // load annotated references
        loadAnnotation(annotationClass);
    }

    protected void loadParser(Class<? extends Annotation> annotationClass,
                              Class<?> parserClass)
        throws Exception {

        // create instance
        Object instance = mInstances.get(parserClass);
        if (instance == null) {
            instance = createParser(parserClass);
            mInstances.put(parserClass, instance);
        }

        // register each method
        Class<?> current = parserClass;
        while (current != null) {
            loadParserMethods(annotationClass, instance, current);
            current = current.getSuperclass();
        }
    }


    protected void loadParserMethods(Class<? extends Annotation> annotationClass,
                                     Object instance, Class<?> current) {

        for (final Method method : current.getDeclaredMethods()) {
            Annotation annotation = method.getAnnotation(annotationClass);
            if (annotation == null) { continue; }

            // TODO: ignore if previously registered

            // TODO: support invoking callback w/ registered annotation

            // create handler to invoke underlying method
            ParserDefinition definition =
                createDefinition(instance, method);

            // register handler
            registerParser(annotationClass, definition);
        }
    }

    public static Object createProxy(final Class<?> clazz) {
        return Enhancer.create(clazz, new MethodInterceptor() {
            @Override
            public Object intercept(Object instance, Method method, Object[] params,
                                    MethodProxy proxied)
                throws Throwable {

                return proxied.invokeSuper(instance, params);
            }
        });
    }

    public static Object createParser(Class<?> clazz)
        throws Exception {

        // create instance around parser
        Object instance = null;
        if ((clazz.getModifiers() & Modifiers.ABSTRACT) == 0) {
            Constructor<?> ctor = clazz.getConstructor();
            if (ctor == null) {
                throw new ParserDefinitionException
                (
                    clazz, "missing default constructor"
                );
            }

            instance = clazz.newInstance();
        }
        else {
            instance = createProxy(clazz);
        }

        // return instance
        return instance;
    }

    protected ParserDefinition createDefinition(Object instance,
                                                Method method) {
        // verify method
        // Class<?>[] paramTypes = method.getParameterTypes();
        /* TODO: this does not work since @Around is @Parser
        if (paramTypes.length != 2 ||
            !Chain.class.isAssignableFrom(paramTypes[0]) ||
            !Parser.class.isAssignableFrom(paramTypes[1])) {
            throw new IllegalArgumentException
            (
                "method must have two params (Chain, Parser): "
                    .concat(method.getName())
            );
        }
        */

        // create definition
        ParserDefinition definition = new ParserDefinition(instance, method);

        // lookup priority
        Priority priority =
            AnnotationUtils.findAnnotation(Priority.class, method);
        if (priority != null) {
            definition.setPriority(priority.value());
        }

        // lookup dependencies
        Dependencies dependencies =
            AnnotationUtils.findAnnotation(Dependencies.class, method);
        if (dependencies != null) {
            definition.setDependencies(dependencies.value());
        }

        // return definition
        return definition;
    }

    protected void registerParser(Class<? extends Annotation> clazz,
                                  ParserDefinition definition) {
        mDefinitions.get(clazz).add(definition);
    }
}
