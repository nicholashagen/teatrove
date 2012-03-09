package org.teatrove.tea;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.teatrove.tea.compiler.ErrorEvent;
import org.teatrove.tea.compiler.ErrorListener;
import org.teatrove.tea.engine.ContextSource;
import org.teatrove.tea.engine.MergedContextSource;
import org.teatrove.tea.runtime.Context;
import org.teatrove.tea.util.FileCompiler;
import org.teatrove.tea.util.TestCompiler;
import org.teatrove.trove.util.ClassInjector;

public abstract class AbstractTemplateTest {

    protected static final String DEST = "target/templates";
    protected static final String PKG = "org.teatrove.tea.templates";

    protected static ClassInjector INJECTOR;
    protected static List<ContextSource> CONTEXTS;
    protected static Object CONTEXT;
    
    static {
        CONTEXTS = new ArrayList<ContextSource>();
        addContext(new TestCompiler.Context(System.out));
    }
    
    public static void addContext(final Object context) {
        CONTEXTS.add(new ContextSource() {
            
            @Override
            public Class<?> getContextType() throws Exception {
                return context.getClass();
            }
            
            @Override
            public Object createContext(Object param) throws Exception {
                return context;
            }
        });
    }
    
    public static Object getContext() {
        if (CONTEXT == null) {
            try { CONTEXT = createContext(); }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        
        return CONTEXT;
    }

    protected static Object createContext() throws Exception {
        // setup merged context
        MergedContextSource source = new MergedContextSource();
        source.init
        (
            Thread.currentThread().getContextClassLoader(), 
            CONTEXTS.toArray(new ContextSource[CONTEXTS.size()]),
            false
        );
        
        return source.createContext(null);
    }
    
    protected static ClassInjector getInjector() {
        if (INJECTOR == null) {
            try { INJECTOR = createInjector(); }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        
        return INJECTOR;
    }
    
    protected static ClassInjector createInjector() throws Exception {
        return ClassInjector.getInstance(getContext().getClass().getClassLoader());
        /*
        (
            new URLClassLoader
            (
                new URL[] { new File(DEST).toURI().toURL() },
                getContext().getClass().getClassLoader()
            )
        );
        */
    }
    
    public static void compile(String... templates) throws Exception {
        for (String template : templates) {
            compile(template);
        }
    }
    
    public static void compile(String template) throws Exception {
        // create target directory
        File target = new File(DEST + '/' + PKG.replace('.', '/'));
        target.mkdirs();
        
        // clean template class
        new File(target, template.replace('.', '/').concat(".class")).delete();
        
        // create compiler
        FileCompiler compiler = new FileCompiler
        (
            new File("src/tests/templates"), PKG, target, getInjector()
        );
        
        // setup context
        compiler.setRuntimeContext(getContext().getClass());
        
        // setup error handler
        compiler.addErrorListener(new ErrorListener() {
            @Override
            public void compileError(ErrorEvent e) {
                System.err.println(e.getDetailedErrorMessage());
            }
        });
        
        // compile templates
        String[] results = compiler.compile(template);
        if (results == null || results.length < 1) {
            throw new IllegalStateException("unable to compile");
        }
    }
    
    public static void execute(String template, Object... params)
        throws Exception {
        
        // get class loader
        // ClassLoader loader = getInjector();
        ClassLoader loader = new URLClassLoader
        (
            new URL[] { new File(DEST).toURI().toURL() },
            getContext().getClass().getClassLoader()
        );
        
        // compile
        compile(template);
        
        // load template class
        Class<?> clazz = loader.loadClass(PKG + '.' + template);
        if (clazz == null) {
            throw new IllegalStateException("unable to load class");
        }
        
        // lookup execute methd
        Method execute = null;
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals("execute")) {
                execute = method;
                break;
            }
        }
        
        // verify execute method
        if (execute == null) {
            throw new IllegalStateException("unable to find execute method");
        }
        
        // setup params
        Object[] args = new Object[params.length + 1];
        args[0] = getContext();
        for (int i = 0; i < params.length; i++) {
            args[i + 1] = params[i];
        }
        
        // execute template
        Object result = execute.invoke(null, args);
        if (!void.class.equals(execute.getReturnType())) {
            ((Context) getContext()).print(result);
        }
    }
}
