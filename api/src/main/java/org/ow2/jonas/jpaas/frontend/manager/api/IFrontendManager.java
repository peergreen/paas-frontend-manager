/**
 * Copyright 2012 Bull S.A.S.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ow2.jonas.jpaas.frontend.manager.api;

import java.util.List;

public interface IFrontendManager {

    /**
     * Create a PaasFrontend
     * @param frontendName Name of the frontend to create
     * @param apiUrl Url of the agent API
     * @throws FrontendManagerBeanException
     */
    public void createFrontend(String frontendName, String apiUrl) throws FrontendManagerBeanException;

    /**
     * Remove a PaasFrontend
     * @param frontendName Name of the frontend to create
     * @throws FrontendManagerBeanException
     */
    public void removeFrontend(String frontendName) throws FrontendManagerBeanException;

    /**
     * Create a vhost
     * @param frontendName         Name of the Frontend
     * @param vhostName Name of the vhost to create
     * @param targetRouterNameList Target router list
     * @throws FrontendManagerBeanException
     */
    public void createVhost(String frontendName, String vhostName, List<String> targetRouterNameList)
            throws FrontendManagerBeanException;

    /**
     * Remove a vhost
     * @param frontendName         Name of the Frontend
     * @param vhostName Name of the vhost to create
     * @throws FrontendManagerBeanException
     */
    public void removeVhost(String frontendName, String vhostName) throws FrontendManagerBeanException;

}
