package org.teatrove.tea.compiler;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

public class Annotations {

    /** The state of whether annotations have been loaded. */
    private boolean mLoaded;

    /** List of URLs to search for annotations. */
    private Set<URL> mClasspath = new HashSet<URL>();

    /** Annotation database and indexing. */
    private AnnotationDB mDatabase = new AnnotationDB();

    /** History of loaded annotations. */
    private Map<Class<? extends Annotation>, Class<?>[]> mAnnotations =
        new HashMap<Class<? extends Annotation>, Class<?>[]>();

    /**
     * Default constructor.
     */
    public Annotations() {
        super();
    }

    /**
     * Add the specified URL to the searchable classpath for annotations.  Note
     * that invoking this method will override the default classpath based on
     * the java.class.path environment setting.
     *
     * @param url  the url to add to the searchable classpath
     */
    public void addScannedClasspath(URL url) {
        mClasspath.add(url);
    }

    /**
     * Add the specified package name to the list of class packages to ignore
     * when searching the classpath for annotations within classes.
     *
     * @param pkg  the package to ignore
     */
    public void addIgnoredPackage(String pkg) {
        mDatabase.addIgnoredPackages(pkg);
    }

    /**
     * Get the list of classes containing the specified annotation at any place
     * within the class.
     *
     * @param annotation  The associated annotation class
     *
     * @return  The list of annotated classes
     */
    public Class<?>[] getAnnotations(Class<? extends Annotation> annotation) {
        // verify loaded
        load();

        // check if previously loaded
        if (mAnnotations.containsKey(annotation)) {
            return mAnnotations.get(annotation);
        }

        // load annotation
        List<Class<?>> list = new ArrayList<Class<?>>();
        Map<String, Set<String>> index = mDatabase.getAnnotationIndex();
        for (String className : index.get(annotation.getName())) {
            try { list.add(Class.forName(className)); }
            catch (Exception exception) {
                // TODO: error(exception);
            }
        }

        // save instance
        Class<?>[] annotations = list.toArray(new Class<?>[list.size()]);
        mAnnotations.put(annotation, annotations);
        return annotations;
    }

    protected void load() {
        // ignore if already loaded
        if (mLoaded) { return; }

        // load annotations processing errors
        mLoaded = true;
        try { loadAnnotations(); }
        catch (Exception e) {
            throw new IllegalArgumentException("unable to load tokens", e);
        }
    }

    protected void loadAnnotations() throws Exception {
        // setup annotation database
        mDatabase.setScanClassAnnotations(true);
        mDatabase.setScanFieldAnnotations(true);
        mDatabase.setScanMethodAnnotations(true);

        // define classpath
        URL[] classpath = null;
        if (mClasspath.isEmpty()) {
            classpath = ClasspathUrlFinder.findClassPaths();
        }
        else {
            classpath = mClasspath.toArray(new URL[mClasspath.size()]);
        }

        // scan archives in classpath
        mDatabase.scanArchives(classpath);
    }
}
