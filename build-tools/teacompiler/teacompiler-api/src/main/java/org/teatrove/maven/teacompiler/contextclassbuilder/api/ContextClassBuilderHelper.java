/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teatrove.maven.teacompiler.contextclassbuilder.api;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.PlexusContainer;

import java.util.Map;
import java.util.List;

/**
 * This is the interface that all helpers will use.  This provides
 * access to the log, project class loader and root package to all
 * ContextClassBuilders.
 * 
 * @author <a href="mailto:brian.r.jackson@gmail.com">Brian Jackson</a>
 * @since Jul 25, 2008 2:01:33 PM
 */
public interface ContextClassBuilderHelper extends ExpressionEvaluator {

    /**
     * Gets the log
     * @return the log
     */
    Log getLog();

    /**
     * A classloader that has access to the project's classes and dependencies.
     * @return A classloader that has access to the project's classes and dependencies.
     */
    ClassLoader getProjectClassLoader();

    /**
     * The package name to use when build dynamic contexts.
     * @return the package name to use when build dynamic contexts.
     */
    String getRootPackage();


    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper#getRuntimeInformation()
     */
    /**
     * Gets the component.
     *
     * @param clazz the clazz
     *
     * @return the component
     *
     * @throws ComponentLookupException the component lookup exception
     */
    public Object getComponent ( Class clazz )
        throws ComponentLookupException;

    /**
     * Gets the component.
     *
     * @param componentKey the component key
     *
     * @return the component
     *
     * @throws ComponentLookupException the component lookup exception
     */
    public Object getComponent ( String componentKey )
        throws ComponentLookupException;

    /**
     * Gets the component.
     *
     * @param role the role
     * @param roleHint the role hint
     *
     * @return the component
     *
     * @throws ComponentLookupException the component lookup exception
     */
    public Object getComponent ( String role, String roleHint )
        throws ComponentLookupException;

    /**
     * Gets the component map.
     *
     * @param role the role
     *
     * @return the component map
     *
     * @throws ComponentLookupException the component lookup exception
     */
    public Map getComponentMap ( String role )
        throws ComponentLookupException;

    /**
     * Gets the component list.
     *
     * @param role the role
     *
     * @return the component list
     *
     * @throws ComponentLookupException the component lookup exception
     */
    public List getComponentList ( String role )
        throws ComponentLookupException;

    /**
     * Gets the container.
     *
     * @return the container
     */
    public PlexusContainer getContainer();
}
