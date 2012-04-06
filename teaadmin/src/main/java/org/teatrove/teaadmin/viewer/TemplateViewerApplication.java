package org.teatrove.teaadmin.viewer;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.teatrove.tea.compiler.Parser;
import org.teatrove.tea.compiler.Scanner;
import org.teatrove.tea.compiler.TemplateRepository;
import org.teatrove.tea.compiler.TemplateRepository.TemplateInfo;
import org.teatrove.teaservlet.AdminApp;
import org.teatrove.teaservlet.AppAdminLinks;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;
import org.teatrove.teaservlet.TeaServlet;
import org.teatrove.teaservlet.TeaServletAdmin;
import org.teatrove.teaservlet.TeaServletAdmin.TemplateWrapper;
import org.teatrove.teaservlet.TeaServletEngine;
import org.teatrove.trove.io.SourceReader;
import org.teatrove.trove.log.Log;

/*

fix issue w/ /relative vs absolute 'call xyz' and 'app.method'
cache results and reset if mod after last updated
size and last mod info?
add close button and links in nav to show hiearchies
right click to view hiearc
quote states must interpret escape chars
add support for recents
general search (search entire source of all templates and provides matches)
on right click method (open return type, open declaration)
on right click call (open return type, open declaration, open info)
on click of dot property (open type)

*/

public class TemplateViewerApplication implements AdminApp
{
    private Log log;
    private int maxCache;
    private String[] paths;
    private boolean initialized;
    private TeaServletAdmin admin;
    private ApplicationConfig config;

    private Map<String, TemplateView> cache =
        new HashMap<String, TemplateView>();
    private Map<String, TemplateSource> sourceMap =
        new HashMap<String, TemplateSource>();
    private SortedSet<TemplateSource> sourceList =
        new TreeSet<TemplateSource>();

    public TemplateViewerApplication()
    {
        super();
    }

    @Override
    public void init(ApplicationConfig conf)
        throws ServletException
    {
        this.config = conf;
        this.log = conf.getLog();
        this.maxCache = conf.getProperties().getInt("maxCache", 100);

        String tmp = conf.getProperties().getString("template.path");
        if (tmp != null) { 
            this.paths = tmp.split("[;,]");
            for (int i = 0; i < this.paths.length; i++) {
                this.paths[i] = this.paths[i].trim();
                while (this.paths[i].endsWith("/")) {
                    this.paths[i] = 
                        this.paths[i].substring(0, this.paths[i].length() - 1);
                }
            }
        }
    }

    @Override
    public void destroy()
    {
        // nothing to do
    }

    @Override
    public AppAdminLinks getAdminLinks()
    {
        AppAdminLinks links = new AppAdminLinks("Template Viewer");
        links.addAdminLink("Viewer", "system.viewer.index");
        return links;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class getContextType()
    {
        return TemplateViewerContext.class;
    }

    @Override
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response)
    {
        if (!initialized) { initialize(); }
        return new TemplateViewerContextImpl(request, response);
    }

    protected void initialize()
    {
        try
        {
            // get the associated tea servlet
            ServletContext context = config.getServletContext();
            TeaServlet servlet =
                (TeaServlet) context.getAttribute(TeaServlet.class.getName());

            // error if no servlet found
            if (servlet == null)
            {
                throw new IllegalArgumentException("no tea servlet defined");
            }

            // find the TeaServlet.getEngine protected method
            Method method = null;
            Class<?> clazz = servlet.getClass();
            while (clazz != null && method == null) {
                try { method = clazz.getDeclaredMethod("getEngine"); }
                catch (Exception e) { clazz = clazz.getSuperclass(); }
            }

            // error if no method found
            if (method == null)
            {
                throw new IllegalArgumentException("no getEngine method found");
            }

            // get tea servlet engine
            method.setAccessible(true);
            TeaServletEngine engine = (TeaServletEngine) method.invoke(servlet);

            // create a new administration and paths
            this.admin = new TeaServletAdmin(engine);
            if (this.paths == null || this.paths.length == 0)
            {
                this.paths = this.admin.getTemplatePaths();
                System.out.println("TEMPLATE PATHS: " + Arrays.toString(this.paths));
            }

            // mark initialized
            this.initialized = true;
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("unable to initialize app", e);
        }
    }

    public static class TemplateSource implements Comparable<TemplateSource>
    {
        private long accessTime;
        private long accessCount;
        private String templateName;
        private String sourceCode;

        public TemplateSource(String templateName, String sourceCode)
        {
            this.templateName = templateName;
            this.sourceCode = sourceCode;
        }

        public String getTemplateName() { return this.templateName; }
        public String getSourceCode() { return this.sourceCode; }

        public void markAccessed()
        {
            this.accessCount++;
            this.accessTime = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object object)
        {
            if (object == this) { return true; }
            else if (!(object instanceof TemplateSource)) { return false; }

            TemplateSource other = (TemplateSource) object;
            return this.templateName.equals(other.templateName);
        }

        @Override
        public int hashCode()
        {
            return this.templateName.hashCode();
        }

        @Override
        public int compareTo(TemplateSource other)
        {
            if (this.accessTime < other.accessTime) { return -1; }
            else if (this.accessTime > other.accessTime) { return 1; }
            else { return this.templateName.compareTo(other.templateName); }
        }
    }

    public static class Name {
        private String name;
        public Name(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public class TemplateViewerContextImpl implements TemplateViewerContext
    {
        @SuppressWarnings("unused")
        private ApplicationRequest request;

        @SuppressWarnings("unused")
        private ApplicationResponse response;

        public TemplateViewerContextImpl(ApplicationRequest request,
                                         ApplicationResponse response)
        {
            super();
            this.request = request;
            this.response = response;
        }

        public List<Name> getNames()
        {
            return Arrays.asList(new Name("Nick"), new Name("John"));
        }

        public Integer[] getNumbersAsArray()
        {
            return new Integer[] { Integer.valueOf(1), Integer.valueOf(2) };
        }



        public String[] findTemplates(String term)
        {
            Set<String> matches = new TreeSet<String>();
            TemplateWrapper[] templates = admin.getKnownTemplates();
            for (TemplateWrapper template : templates)
            {
                String name = template.getName();
                if (name.contains(term)) { matches.add(name); }
            }

            return matches.toArray(new String[matches.size()]);
        }

        public Parent[] getParents(String parent)
        {
            Set<Parent> parents = new TreeSet<Parent>();
            Map<String, Boolean> dirs = new HashMap<String, Boolean>();
            TemplateWrapper[] templates = admin.getKnownTemplates();

            for (TemplateWrapper template : templates)
            {
                String name = template.getName();
                if (parent.length() == 0)
                {
                    int idx = name.indexOf('.');
                    if (idx < 0) { parents.add(new Parent(parent, name, false)); }
                    else
                    {
                        String dir = name.substring(0, idx);
                        if (!dirs.containsKey(dir))
                        {
                            dirs.put(dir, Boolean.TRUE);
                            parents.add(new Parent(parent, dir, true));
                        }
                    }
                }
                else if (name.startsWith(parent))
                {
                    int idx = name.indexOf('.', parent.length() + 1);
                    if (idx < 0)
                    {
                        parents.add(new Parent(parent, name.substring(parent.length() + 1), false));
                    }
                    else
                    {
                        String dir = name.substring(parent.length() + 1, idx);
                        if (!dirs.containsKey(dir))
                        {
                            dirs.put(dir, Boolean.TRUE);
                            parents.add(new Parent(parent, dir, true));
                        }
                    }
                }
            }

            return parents.toArray(new Parent[parents.size()]);
        }

        public TemplateView[] getTemplateViews()
        {
            TemplateRepository repo = TemplateRepository.getInstance();

            Set<TemplateView> matches = new TreeSet<TemplateView>();
            TemplateWrapper[] templates = admin.getKnownTemplates();
            for (TemplateWrapper template : templates)
            {
                String name = template.getName();
                TemplateView view = cache.get(name);
                if (view == null)
                {
                    TemplateInfo info = repo.getTemplateInfo(name);
                    if (info != null) { view = new TemplateView(name, info); }
                }

                if (view != null) { matches.add(view); }
            }

            return matches.toArray(new TemplateView[matches.size()]);
        }

        public void resetTemplateViews()
        {
            cache.clear();
            sourceList.clear();
            sourceMap.clear();
        }

        public void resetTemplateView(String name)
        {
            cache.remove(name);
            TemplateSource source = sourceMap.remove(name);
            if (source != null) { sourceList.remove(source); }
        }

        public TemplateView getTemplateView(String parent, String name)
            throws Exception
        {
            // verify template
            if (name == null) { throw new IllegalArgumentException("name"); }

            // find actual path based on parent and template
            String path = null;
            TemplateInfo template = null;
            TemplateRepository repo = TemplateRepository.getInstance();
            if (parent != null)
            {
                path = parent + '.' + name;
                template = repo.getTemplateInfo(path);
            }

            if (template == null)
            {
                path = name;
                template = repo.getTemplateInfo(name);
            }

            if (template == null)
            {
                throw new IllegalArgumentException("template not found");
            }

            // check cache
            TemplateView view = cache.get(path);
            if (view == null)
            {
                // build view and cache
                view = new TemplateView(path, template);
                cache.put(path, view);
            }

            // build source if necessary
            if (view.getSourceCode() == null)
            {
                this.parseTemplate(view);
            }

            // get source code
            TemplateSource source = sourceMap.get(path);
            if (source == null)
            {
                source = new TemplateSource(path, view.getSourceCode());
                while (sourceMap.size() > maxCache)
                {
                    TemplateSource first = sourceList.first();
                    sourceList.remove(first);
                    sourceMap.remove(first.getTemplateName());

                    TemplateView temp = cache.get(first.getTemplateName());
                    if (temp != null) { temp.setSourceCode(null); }
                }

                sourceMap.put(path, source);
                sourceList.add(source);
            }

            // update source code
            source.markAccessed();
            view.setSourceCode(source.getSourceCode());

            // return associated view
            return view;
        }

        protected StringBuilder readInputStream(InputStream input) 
            throws IOException {
            
            StringBuilder buffer = new StringBuilder(65535);
            
            // copy the input stream to a source buffer
            int ch = -1;
            while ((ch = input.read()) >= 0) {
                if (ch == '\r') { continue; }
                buffer.append((char) ch);
            }
            
            return buffer;
        }
        
        protected void parseTemplate(TemplateView view)
            throws Exception
        {
            // search for valid stream
            InputStream input = this.findTemplate(view, view.getSimpleName());
            
            // build and return output
            try { this.processTemplate(view, input); }
            finally { input.close(); }

            // build call hierarchy
            TemplateRepository repo = TemplateRepository.getInstance();
            for (TemplateInfo info : TemplateRepository.getInstance().getCallers(view.getSimpleName()))
            {
                TemplateInfo callerTemplate =
                    repo.getTemplateInfo(info.getShortName());
                TemplateView callerView =
                    new TemplateView(info.getShortName(), callerTemplate);

                // search for valid stream
                InputStream callerInput =
                    this.findTemplate(callerView, callerView.getSimpleName());
                
                // build and return output
                try { this.processTemplate(callerView, callerInput); }
                finally { input.close(); }

                for (TemplateView.Callee callee : callerView.getCallees())
                {
                    if (callee.getName().equals(view.getSimpleName()))
                    {
                        view.addCaller(new TemplateView.Caller(info.getShortName(), callee.getLine()));
                    }
                }
            }
        }

        protected String findTemplate(String parent, String name)
        {
            String path = parent.replace('/', '.') + "." + name;
            TemplateRepository repo = TemplateRepository.getInstance();
            if (repo.getTemplateInfo(path) != null) { return path; }
            else if (repo.getTemplateInfo(name) != null) { return name; }
            else { return null; }
        }

        protected InputStream findTemplate(TemplateView view, String name)
            throws FileNotFoundException
        {
            String template = name.replace('.', '/') + ".tea";
            if (!template.startsWith("/")) { 
                template = "/".concat(template);
            }

            InputStream input = null;
            for (int i = 0; input == null && i < paths.length; i++)
            {
                URL url = null;
                String path = paths[i];
                String fullName = path.concat(template);
                try { url = new URL(fullName); }
                catch (MalformedURLException e1) {
                    ServletContext servletContext = config.getServletContext();
                    try { url = servletContext.getResource(fullName); }
                    catch (Exception e2) {
                        log.debug("unable to find template source: " + fullName);
                        log.debug(e1);
                        log.debug(e2);
                        throw new FileNotFoundException(name);
                    }
                }

                if (url != null) {
                    try {
                        input = url.openStream();
                        view.setLocation(url.toExternalForm());
                    }
                    catch (IOException ioe) {
                        log.debug("unable to open input stream: " + url);
                        log.debug(ioe);
                    }
                }
            }

            if (input == null)
            {
                throw new FileNotFoundException(name);
            }

            return new BufferedInputStream(input);
        }

        protected void processTemplate(TemplateView view, InputStream input)
            throws Exception {
            
            // walk the source tree injecting tags onto keywords,
            // setting up newline boundaries, adding callee info, and cleaning
            // the source code
            StringBuilder buffer = readInputStream(input);
            StringReader reader = new StringReader(buffer.toString());
            Scanner scanner = new Scanner(new SourceReader(reader, "<%", "%>"));
            Parser parser = new Parser(scanner);
            SourceWalker walker = new SourceWalker(buffer);
            walker.visit(parser.parse());
            walker.finish(view);
        }

        protected String trim(String string)
        {
            return string.trim().replace("&nbsp;", "").replace("&#160;", "");
        }

        protected Class<?> findType(String name)
            throws Exception
        {
            if (name.contains(".")) { return Class.forName(name); }
            else
            {
                try { return Class.forName("java.lang.".concat(name)); }
                catch (ClassNotFoundException cnfe)
                {
                    return Class.forName("java.util.".concat(name));
                }
            }
        }
    }


    protected static final List<String> KEYWORDS = Arrays.asList
    (
        "if",
        "else",
        "foreach",
        "for",
        "while",
        "capture"
    );

    protected static enum State
    {
        DEFAULT,
        TAG_OPEN,
        TEMPLATE,
        COMMENT,
        COMMENT_START,
        SINGLE_COMMENT,
        MULTI_COMMENT,
        MULTI_COMMENT_END,
        TEMPLATE_END,
        COMMENT_END,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        STATEMENT,
        STATEMENT_END,
        STATEMENT_KEYWORD,
        CALL,
        PARAMS,
        DECLARATION,
        FUNCTION_PARAMS,
        CALL_PARAMS
    }
}
