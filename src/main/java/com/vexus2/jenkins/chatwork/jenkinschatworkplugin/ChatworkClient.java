package com.vexus2.jenkins.chatwork.jenkinschatworkplugin;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

public class ChatworkClient {

  private final String apiKey;

  private final String proxySv;
  private final String proxyPort;

  private final String roomId;

  private static final String API_URL = "https://api.chatwork.com/v1";

  private final HttpClient httpClient = new HttpClient();

  public ChatworkClient(String apiKey, String proxySv, String proxyPort, String roomId) {
    this.apiKey = apiKey;
    this.proxySv = proxySv;
    this.proxyPort = proxyPort;
    this.roomId = roomId;
  }

  public boolean sendMessage(String message) throws IOException {
    if (StringUtils.isEmpty(apiKey) || StringUtils.isEmpty(roomId)) {
      throw new IllegalArgumentException("API Key or Room ID is empty");
    }

    String url = API_URL + "/rooms/" + this.roomId + "/messages";

    PostMethod method = new PostMethod(url);

    try {
      method.addRequestHeader("X-ChatWorkToken", apiKey);
      method.setParameter("body", message);

      if(isEnabledProxy()){
        setProxyHost(proxySv, Integer.parseInt(this.proxyPort));
      }

      int statusCode = httpClient.executeMethod(method);

      if (statusCode != HttpStatus.SC_OK) {
        String response = method.getResponseBodyAsString();
        throw new ChatworkException("Response is not valid. Check your API Key or Chatwork API status. response_code = " + statusCode + ", message =" + response);
      }

    } finally {
      method.releaseConnection();
    }

    return true;
  }

  public boolean isEnabledProxy(){
    if(StringUtils.isBlank(proxySv) || StringUtils.isBlank(proxyPort) || StringUtils.equals(proxySv, "NOPROXY")){
      return false;
    }

    try {
      Integer.parseInt(proxyPort);

    } catch (NumberFormatException e){
      // proxyPort is not number
      return false;
    }

    return true;
  }

  public void setProxyHost(String hostname, int port){
    httpClient.getHostConfiguration().setProxyHost(new ProxyHost(hostname, port));
  }
}
