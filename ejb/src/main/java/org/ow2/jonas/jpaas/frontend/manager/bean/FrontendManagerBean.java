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

import org.ow2.easybeans.osgi.annotation.OSGiResource;
import org.ow2.jonas.jpaas.apache.manager.util.api.xml.Directive;
import org.ow2.jonas.jpaas.apache.manager.vhost.manager.api.xml.Vhost;
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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ws.rs.core.MultivaluedMap;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Stateless(mappedName = "FrontendManagerBean")
@Local(org.ow2.jonas.jpaas.frontend.manager.api.IFrontendManager.class)
@Remote(org.ow2.jonas.jpaas.frontend.manager.api.IFrontendManager.class)
public class FrontendManagerBean implements org.ow2.jonas.jpaas.frontend.manager.api.IFrontendManager {

    /**
     * The logger
     */
    private Log logger = LogFactory.getLog(org.ow2.jonas.jpaas.frontend.manager.bean.FrontendManagerBean.class);

    /**
     * Http accepted status
     */
    private static final int HTTP_STATUS_ACCEPTED = 202;

    /**
     * Http Ok status
     */
    private static final int HTTP_STATUS_OK = 200;

    /**
     * Http no content status
     */
    private static final int HTTP_STATUS_NO_CONTENT = 204;

    /**
     * Http Created status
     */
    private static final int HTTP_STATUS_CREATED = 201;


    /**
     * REST request type
     */
    private enum REST_TYPE {
        PUT, POST, GET, DELETE
    }

    /**
     * SR facade router
     */
    @OSGiResource
    private ISrPaasApacheJkRouterFacade srApacheJkEjb;

    /**
     * SR facade paasResource - iaasCompute link
     */
    @OSGiResource
    private ISrPaasResourceIaasComputeLink srPaasResourceIaasComputeLink;

    /**
     * SR facade Frontend
     */
    @OSGiResource
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
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("address", "*:80");

            Vhost vhost = sendRequestWithReply(
                    REST_TYPE.POST,
                    getUrl(apiUrl, "/vhostmanager/vhost"),
                    params,
                    Vhost.class);

            vhostID = String.valueOf(vhost.getId());
        }
        virtualHost = new VirtualHostVO(vhostName, vhostID);
        paasFrontend.getVirtualHosts().add(virtualHost);
        paasFrontend = srPaasFrontendFacade.updateFrontend(paasFrontend);

/*        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("address", "*:80");
        params.add("servername", vhostName);

        Vhost vhost = sendRequestWithReply(
                REST_TYPE.POST,
                getUrl(apiUrl, "/vhostmanager/vhost"),
                params,
                Vhost.class);

        VirtualHostVO virtualHostVO = new VirtualHostVO(vhostName, String.valueOf(vhost.getId()));


        paasFrontend.getVirtualHosts().add(virtualHostVO);
        paasFrontend = srPaasFrontendFacade.updateFrontend(paasFrontend);*/


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

            String proxyUrl = "http://" + iaasCompute.getIpAddress() + "/";
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/" + vhostName + "/"); // Temporary. ToDo : use "/" instead of /vhostname.
            params.add("url", proxyUrl);

            Directive proxyDirective = sendRequestWithReply(
                    REST_TYPE.POST,
                    getUrl(apiUrl, "/proxymanager/vhost/" + vhostID + "/proxypass"),
                    params,
                    Directive.class);


            Map<String,String> proxyList = new Hashtable<String, String>();
            proxyList.put(String.valueOf(proxyDirective.getId()), proxyDirective.getValue());
            logger.debug("proxyList : " + proxyList.size());


            virtualHostVOList = paasFrontend.getVirtualHosts();
            for (VirtualHostVO tmp : virtualHostVOList) {
                if (tmp.getName().equals(vhostName)) {
                    tmp.setProxypassDirectives(proxyList);
                    break;
                }
            }

        }

        paasFrontend = srPaasFrontendFacade.updateFrontend(paasFrontend);
        // Ask for a reload
        sendRequestWithReply(
                REST_TYPE.POST,
                getUrl(apiUrl, "apache-manager/server/action/reload"),
                null,
                null);

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

        //Temporary : do not delete the vhost, delete the corresponding ProxyPass
        /*sendRequestWithReply(
                REST_TYPE.DELETE,
                getUrl(apiUrl, "/vhostmanager/vhost/" + virtualHost.getVhostId()),
                null,
                null);*/
        Map<String,String> proxyList = virtualHost.getProxypassDirectives();

        for(Iterator<Map.Entry<String, String>> it = proxyList.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> entry = it.next();
            if(entry.getValue().contains("/" + vhostName + "/ ")) {
                it.remove();

                sendRequestWithReply(
                        REST_TYPE.DELETE,
                        getUrl(apiUrl, "/proxymanager/vhost/" + virtualHost.getVhostId() + "/proxypass/" + entry.getKey()),
                        null,
                        null);
            }
        }

        srPaasFrontendFacade.removeVirtualHost(paasFrontend.getId(), vhostName);

        // Ask for a reload
        sendRequestWithReply(
                REST_TYPE.POST,
                getUrl(apiUrl, "apache-manager/server/action/reload"),
                null,
                null);

    }




    /**
     * @param agentApi the api url
     * @param path the path to add
     * @return the HTTP URL
     */
    private String getUrl(final String agentApi, final String path) {
        return agentApi + "/" + path;
    }

    /**
     * Send a REST request and get response
     *
     * @param type
     *            Http type of the request
     * @param url
     *            request path
     * @param params
     *            XML content of the request
     * @param responseClass
     *            response class
     * @return ResponseClass response class
     */
    private <ResponseClass> ResponseClass sendRequestWithReply(REST_TYPE type,
            String url, MultivaluedMap<String, String> params,
            java.lang.Class<ResponseClass> responseClass)
            throws org.ow2.jonas.jpaas.frontend.manager.api.FrontendManagerBeanException {

        Client client = Client.create();

        WebResource webResource = client.resource(removeRedundantForwardSlash(url));

        if (params != null) {
            webResource = webResource.queryParams(params);
        }

        ClientResponse clientResponse;
        switch (type) {
            case PUT:
                clientResponse = webResource.put(ClientResponse.class);
                break;
            case GET:
                clientResponse = webResource.get(ClientResponse.class);
                break;
            case POST:
                clientResponse = webResource.post(ClientResponse.class);
                break;
            case DELETE:
                clientResponse = webResource.delete(ClientResponse.class);
                break;
            default:// put
                clientResponse = webResource.put(ClientResponse.class);
                break;
        }

        int status = clientResponse.getStatus();

        if (status != HTTP_STATUS_ACCEPTED && status != HTTP_STATUS_OK
                && status != HTTP_STATUS_NO_CONTENT && status != HTTP_STATUS_CREATED) {
            throw new org.ow2.jonas.jpaas.frontend.manager.api.FrontendManagerBeanException(
                    "Error on JOnAS agent request : " + status);
        }

        ResponseClass r = null;

        if (status != HTTP_STATUS_NO_CONTENT) {
            //ToDo Apache-Manager REST interfaces need to be harmonized
/*            if (clientResponse.getType() != MediaType.APPLICATION_XML_TYPE) {
                throw new FrontendManagerBeanException(
                        "Error on JOnAS agent response, unexpected type : "
                                + clientResponse.getType());
            }*/

            if (responseClass != null)
                r = clientResponse.getEntity(responseClass);
        }

        client.destroy();

        return r;

    }

    /**
     * Remove redundant forward slash in a String url
     * @param s a String url
     * @return The String url without redundant forward slash
     */
    private String removeRedundantForwardSlash(String s) {
        String tmp = s.replaceAll("/+", "/");
        return tmp.replaceAll(":/", "://");
    }
}
