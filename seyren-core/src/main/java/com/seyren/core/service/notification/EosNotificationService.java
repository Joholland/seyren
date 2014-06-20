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
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFactory;
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
import org.springframework.beans.factory.serviceloader.ServiceFactoryBean;


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
        LOGGER.info("Send EOS Notification");
        try {
            if (check.getState() == AlertType.ERROR) {
                for(Alert alert: alerts) {
                    String hostname = getHostName(alert);
                    String message = getEosMessage(check);
                    sendMessage(hostname, check.getName(), message);
                }

            } else {
                LOGGER.warn("Did not send notification to Eos for check in state: {}", check.getState());
            }
        } catch (Exception e) {
            throw new NotificationFailedException("Failed to send notification to Eos", e);
        }
    }

    private String getHostName(Alert alert) {
        LOGGER.info(alert.getTarget());
        String[] target = alert.getTarget().split("\\.");
        String hostname = target[3];
        return hostname;
    }

    private String getEosMessage(Check check) {
        String message = "Check <a href=" + seyrenConfig.getBaseUrl() + "/#/checks/" + check.getId() + ">" + check.getName() + "</a> has entered its " + check.getState().toString() + " state.";
        return message;
    }

    private void sendMessage(String hostname, String key, String message) {
        LOGGER.info("NotifyEOS: HostName={}, Key={}, Message={}", hostname, key, message);

        try {
            boolean result;
            
            // http://stackoverflow.com/questions/2490737/how-to-change-webservice-url-endpoint
            // Check out option 2 for the accepted answer. [WLW]
//          EosService eosSvc = new EosService(wsdlLocation, serviceName);
            EosService eosSvc = new EosService();
            
            IEosService iEosSvc = eosSvc.getBasicHttpBindingIEosService();
            result = iEosSvc.notifyNodeFailure(hostname, key, message);
            LOGGER.info("Action=NotifyEOS, Result=" +result);
        }
        catch (Exception e) {
            LOGGER.warn("Action=NotifyEOS, Result=ERROR", e);
        }
    }

    @Override
    public boolean canHandle(SubscriptionType subscriptionType) {
        return subscriptionType == SubscriptionType.EOS;
    }
}
