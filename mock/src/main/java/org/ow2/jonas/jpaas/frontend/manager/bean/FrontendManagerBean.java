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
package org.ow2.jonas.jpaas.frontend.manager.bean;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.ow2.jonas.jpaas.frontend.manager.api.FrontendManagerBeanException;
import org.ow2.jonas.jpaas.sr.facade.api.ISrPaasApacheJkRouterFacade;
import org.ow2.jonas.jpaas.sr.facade.api.ISrPaasFrontendFacade;
import org.ow2.jonas.jpaas.sr.facade.api.ISrPaasResourceIaasComputeLink;
import org.ow2.jonas.jpaas.sr.facade.vo.ApacheJkVO;
import org.ow2.jonas.jpaas.sr.facade.vo.IaasComputeVO;
import org.ow2.jonas.jpaas.sr.facade.vo.PaasFrontendVO;
import org.ow2.jonas.jpaas.sr.facade.vo.VirtualHostVO;
import org.ow2.util.log.Log;
import org.ow2.util.log.LogFactory;

import java.util.*;


@Component
@Provides
@Instantiate
public class FrontendManagerBean implements org.ow2.jonas.jpaas.frontend.manager.api.IFrontendManager {

    /**
     * The logger
     */
    private Log logger = LogFactory.getLog(org.ow2.jonas.jpaas.frontend.manager.bean.FrontendManagerBean.class);

    /**
     * SR facade router
     */
    @Requires
    private ISrPaasApacheJkRouterFacade srApacheJkEjb;

    /**
     * SR facade paasResource - iaasCompute link
     */
    @Requires
    private ISrPaasResourceIaasComputeLink srPaasResourceIaasComputeLink;

    /**
     * SR facade Frontend
     */
    @Requires
    private ISrPaasFrontendFacade srPaasFrontendFacade;

    /**
     * Constructor
     */
    public FrontendManagerBean() {
    }


    /**
     * Create a PaasFrontend
     *
     * @param frontendName Name of the frontend to create
     * @param apiUrl       Url of the agent API
     * @throws org.ow2.jonas.jpaas.frontend.manager.api.FrontendManagerBeanException
     *
     */
    @Override
    public void createFrontend(String frontendName, String apiUrl) throws FrontendManagerBeanException {

        logger.info("Frontend '" + frontendName + "' creating ....");
        PaasFrontendVO paasFrontendVO = new PaasFrontendVO(frontendName, apiUrl, new LinkedList<VirtualHostVO>());
        try {
            srPaasFrontendFacade.createFrontend(paasFrontendVO);
        } catch (Exception e) {
            throw new FrontendManagerBeanException("Cannot create the Frontend " + frontendName + ".", e);
        }
    }

    /**
     * Remove a PaasFrontend
     *
     * @param frontendName Name of the frontend to create
     * @throws org.ow2.jonas.jpaas.frontend.manager.api.FrontendManagerBeanException
     *
     */
    @Override
    public void removeFrontend(String frontendName) throws FrontendManagerBeanException {

        logger.info("Frontend '" + frontendName + "' deleting ....");

        // get the Frontend from SR
        PaasFrontendVO paasFrontend = null;
        List<PaasFrontendVO> frontendVOList = srPaasFrontendFacade.findFrontends();
        for (PaasFrontendVO tmp : frontendVOList) {
            if (tmp.getName().equals(frontendName)) {
                paasFrontend = tmp;
                break;
            }
        }
        if (paasFrontend == null) {
            throw new FrontendManagerBeanException("Frontend '" + frontendName + "' doesn't exist !");
        }

        try {
            srPaasFrontendFacade.deleteFrontend(paasFrontend.getId());
        } catch (Exception e) {
            throw new FrontendManagerBeanException("Cannot delete the Frontend " + frontendName + ".", e);
        }
    }

    /**
     * Create a vhost
     *
     * @param frontendName         Name of the Frontend
     * @param vhostName            Name of the vhost to create
     * @param targetRouterNameList Target router list
     * @throws org.ow2.jonas.jpaas.frontend.manager.api.FrontendManagerBeanException
     *
     */
    @Override
    public void createVhost(String frontendName, String vhostName, List<String> targetRouterNameList)
            throws FrontendManagerBeanException {

        logger.info("Vhost '" + vhostName + "' creating ....");

        // get the Frontend from SR
        PaasFrontendVO paasFrontend = null;
        List<PaasFrontendVO> frontendVOList = srPaasFrontendFacade.findFrontends();
        for (PaasFrontendVO tmp : frontendVOList) {
            if (tmp.getName().equals(frontendName)) {
                paasFrontend = tmp;
                break;
            }
        }
        if (paasFrontend == null) {
            throw new FrontendManagerBeanException("Frontend '" + frontendName + "' doesn't exist !");
        }
        // get the url of the Agent API
        String apiUrl = paasFrontend.getApiUrl();


        VirtualHostVO virtualHost = null;
        String vhostID;
        //Temporary : Use the same vhost. Test if a vhost exists, use the same vhostID if it's the case,
        // if not create the vhost on the Apache Server
        List<VirtualHostVO> virtualHostVOList = paasFrontend.getVirtualHosts();
        if (!virtualHostVOList.isEmpty()) {
            vhostID = virtualHostVOList.get(0).getVhostId();
        } else {
            logger.debug("No vhost on the machine: create one");

            vhostID = String.valueOf((new Random()).nextLong());
        }

        //Do nothing if there is already a vhost with the same name
        boolean vhostExists = false;
        for (VirtualHostVO vhostTmp : virtualHostVOList) {
            if (vhostTmp.getName().equals(vhostName)) {
                vhostExists = true;
                break;
            }
        }
        if (!vhostExists) {
            virtualHost = new VirtualHostVO(vhostName, vhostID);
            paasFrontend.getVirtualHosts().add(virtualHost);
            paasFrontend = srPaasFrontendFacade.updateFrontend(paasFrontend);


            for (String routerName : targetRouterNameList) {
                // get the router from SR
                ApacheJkVO apacheJk = null;
                List<ApacheJkVO> apacheJkVOList = srApacheJkEjb.findApacheJkRouters();
                for (ApacheJkVO tmp : apacheJkVOList) {
                    if (tmp.getName().equals(routerName)) {
                        apacheJk = tmp;
                        break;
                    }
                }
                if (apacheJk == null) {
                    throw new FrontendManagerBeanException("Router '" + routerName + "' doesn't exist !");
                }

                IaasComputeVO iaasCompute = srPaasResourceIaasComputeLink.findIaasComputeByPaasResource(apacheJk.getId());
                if (iaasCompute == null) {
                    throw new FrontendManagerBeanException("Cannot find the IaaS Compute for the Router '" + routerName + "!");
                }

                String[] splitName = vhostName.split(".jpaas.org");
                if (splitName.length == 0) {
                    throw new FrontendManagerBeanException("Error : cannot get the instance Name.");
                }

            }

            paasFrontend = srPaasFrontendFacade.updateFrontend(paasFrontend);

        }

    }

    /**
     * Remove a vhost
     *
     * @param frontendName  Name of the Frontend
     * @param vhostName     Name of the vhost to create
     * @throws org.ow2.jonas.jpaas.frontend.manager.api.FrontendManagerBeanException
     *
     */
    @Override
    public void removeVhost(String frontendName, String vhostName) throws FrontendManagerBeanException {

        logger.info("Vhost '" + vhostName + "' deleting ....");

        // get the Frontend from SR
        PaasFrontendVO paasFrontend = null;
        List<PaasFrontendVO> frontendVOList = srPaasFrontendFacade.findFrontends();
        for (PaasFrontendVO tmp : frontendVOList) {
            if (tmp.getName().equals(frontendName)) {
                paasFrontend = tmp;
                break;
            }
        }
        if (paasFrontend == null) {
            throw new FrontendManagerBeanException("Frontend '" + frontendName + "' doesn't exist !");
        }
        // get the url of the Agent API
        String apiUrl = paasFrontend.getApiUrl();

        // get the vhost
        VirtualHostVO virtualHost = null;
        List<VirtualHostVO> virtualHostVOList = paasFrontend.getVirtualHosts();
        for (VirtualHostVO tmp : virtualHostVOList) {
            if (tmp.getName().equals(vhostName)) {
                virtualHost = tmp;
                break;
            }
        }
        if (virtualHost == null) {
            throw new FrontendManagerBeanException("Vhost '" + vhostName + "' doesn't exist !");
        }

        srPaasFrontendFacade.removeVirtualHost(paasFrontend.getId(), vhostName);

    }

}
