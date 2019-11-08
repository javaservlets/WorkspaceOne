package com.example.vmware;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import org.forgerock.util.encode.Base64;
import java.net.URL;

import static com.sun.identity.idm.AMIdentityRepository.debug;

public class UserInfo {
    String server;
    String tenant_key;
    String encoded_credentials;
    HttpGet http; // reusable obj since we keep setting/reduplicating the same headers

    public UserInfo(String server, String tenant_key, String usr, String pwd) { // I think I m only setting vals n via the constructor to make scratch testing as 'real' as possible
        this.server = server;
        this.tenant_key = tenant_key;
        this.encoded_credentials = Base64.encode((usr + ":" + pwd).getBytes());
        log(encoded_credentials);
    }

    public String getStatus(String qry, String search_by) { //qry could be for either enrollment OR compliance
        String enrolled = "";
        URL url = null;
        log(" getstatus: " + qry + " :: " + search_by);

        try {
            HttpClient httpclient = HttpClients.createDefault();
            this.http = new HttpGet(server + "/" + qry + search_by);
            this.setHeaders();

            HttpResponse response = httpclient.execute(http);
            HttpEntity entity = response.getEntity();
            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                String entity_str = EntityUtils.toString(responseEntity);
                log(" userInfo. getStatus: " + entity_str);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entity_str.contains("true")) { //&& entity_str.contains("compliantStatus")
                    log(entity_str);
                    enrolled = "compliant";
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entity_str.contains("false")) {
                    log(entity_str);
                    enrolled = "noncompliant";
                } else if (entity_str.contains("Device not found")) { // do not check httpStatus since srv can throw diff codes n this situation
                    enrolled = "unknown";
                }

            } else {
                enrolled = "connection error";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return enrolled;
    }

    public String getToken() { //todo needs to be called! //rj??
        String cook = "";
        try {
            HttpClient httpclient = HttpClients.createDefault();
            this.http = new HttpGet(server);
            this.setHeaders();

            HttpResponse response = httpclient.execute(http);
            HttpEntity entity = response.getEntity();

            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                log("userInfo.getToken: " + EntityUtils.toString(responseEntity));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return cook;
        }
    }

    private void setHeaders() {
        this.http.setHeader("Accept", "application/json");
        this.http.setHeader("Authorization", "Basic " + this.encoded_credentials);
        this.http.setHeader("aw-tenant-code", this.tenant_key);
    }

    private String stripQuote(String val) {
        return (val.replace("\"", ""));
    }

    private static String stripNoise(String parent, String child) {
        String noise = "";
        try {
            JSONObject jobj = new JSONObject(parent);
            Object idtkn = jobj.getString(child);
            noise = idtkn.toString();

            if (noise.startsWith("[")) { // get only 'value' from "["value"]"
                noise = noise.substring(1, noise.length() - 1);
            }
            if (noise.startsWith("\"")) {
                noise = noise.substring(1, noise.length() - 1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return noise;
        }
    }


    public static void log(String str) {
        System.out.println("+++  userInfo:   " + str);
        debug.error("+++      " + str); // should be 'message' instead?
    }
}