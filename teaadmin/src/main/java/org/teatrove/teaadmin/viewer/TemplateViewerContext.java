package org.teatrove.teaadmin.viewer;

public interface TemplateViewerContext {
    
    String[] findTemplates(String term);
   
    Parent[] getParents(String parent);
    
    TemplateView[] getTemplateViews();
    
    void resetTemplateViews();
    
    void resetTemplateView(String name);
    
    TemplateView getTemplateView(String parent, String name)
        throws Exception;
}
