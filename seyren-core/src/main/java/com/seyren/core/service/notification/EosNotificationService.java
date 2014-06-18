/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seyren.core.service.notification;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jws.WebParam;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.WebServiceRef;

import EOS.EosService;
import EOS.IEosService;
import EOS.NotifyNodeFailure;
import EOS.ObjectFactory;
import com.sun.org.apache.bcel.internal.generic.LOOKUPSWITCH;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.LoggerFactory;

import com.seyren.core.domain.Alert;
import com.seyren.core.domain.AlertType;
import com.seyren.core.domain.Check;
import com.seyren.core.domain.Subscription;
import com.seyren.core.domain.SubscriptionType;
import com.seyren.core.exception.NotificationFailedException;
import com.seyren.core.util.config.SeyrenConfig;



/**
 * Created by johnmca on 5/13/14.
 */
@Named
public class EosNotificationService implements NotificationService {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EosNotificationService.class);

    private final SeyrenConfig seyrenConfig;
    private final String baseUrl;

//    @WebServiceRef(wsdlLocation = "http://eos.sb.karmalab.net:8000/eosservice?wsdl");
//    static EosService eservice;

    @Inject
    public EosNotificationService(SeyrenConfig seyrenConfig) {
        this.seyrenConfig = seyrenConfig;
        this.baseUrl = "https://eos";
    }

    protected EosNotificationService(SeyrenConfig seyrenConfig, String baseUrl) {
        this.seyrenConfig = seyrenConfig;
        this.baseUrl = baseUrl;
    }


    @Override
    public void sendNotification(Check check, Subscription subscription, List<Alert> alerts) throws NotificationFailedException {
        String hostname = "foo";
        try {
            if (check.getState() == AlertType.ERROR) {
                String message = getEosMessage(check);
                sendMessage(hostname, check.getName(), message);
            } else {
                LOGGER.warn("Did not send notification to Eos for check in state: {}", check.getState());
            }
        } catch (Exception e) {
            throw new NotificationFailedException("Failed to send notification to Eos", e);
        }
    }

    private String getEosMessage(Check check) {
        String message = "Check <a href=" + seyrenConfig.getBaseUrl() + "/#/checks/" + check.getId() + ">" + check.getName() + "</a> has entered its " + check.getState().toString() + " state.";
        return message;
    }

    private void sendMessage(String hostname, String key, String message) {
        LOGGER.info("Posting: HostName={}, Key={}, Message={}", hostname, key, message);
        //HttpClient client = new DefaultHttpClient();
        //String url = "http://eos.sb.karmalab.net:8000/eosservice?wsdl";
        //HttpPost post = new HttpPost(url);
//        EOS.ObjectFactory factory = new ObjectFactory();
//        JAXBElement<String> createHostName = factory.createNotifyNodeFailureHostname(hostname);
//        JAXBElement<String> createFailureKey = factory.createNotifyNodeFailureFailureKey(key);
//        JAXBElement<String> createDetails = factory.createNotifyNodeFailureDetails(message);


        try {
            // Send message to EOS here
//            NotifyNodeFailure notifyObject= new NotifyNodeFailure();
//            notifyObject.setHostname(createHostName);
//            notifyObject.setFailureKey(createFailureKey);
//            notifyObject.setDetails(createDetails);


            EosService svc = new EosService();

            IEosService nsvc = svc.getWSHttpBindingIEosService();
            nsvc.notifyNodeFailure(hostname, key, message);
            nsvc.notifyNodeFailure()

//            LOGGER.info(svc.getPorts().toString());
//            java.util.Iterator it = svc.getPorts();
//            while (it.hasNext()) {
//               LOGGER.info("value = " + it.next());
//            }


        }
        catch (Exception e) {
            LOGGER.warn("Error posting to EOS", e);
        } //finally {
            //post.releaseConnection();
        //}

    }

    @Override
    public boolean canHandle(SubscriptionType subscriptionType) {
        return subscriptionType == SubscriptionType.EOS;
    }
}
