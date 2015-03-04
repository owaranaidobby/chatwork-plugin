package com.vexus2.jenkins.chatwork.jenkinschatworkplugin;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class ChatworkClient {

  private final String apiKey;

  private final String proxySv;
  private final String proxyPort;

  private final String roomId;

  private static final String API_URL = "https://api.chatwork.com/v1";

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
    URL obj = new URL(url);
    HttpsURLConnection con;

    if (!isEnabledProxy()) {
      con = (HttpsURLConnection) obj.openConnection();
    } else {
      Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxySv, Integer.parseInt(this.proxyPort)));
      con = (HttpsURLConnection) obj.openConnection(proxy);
    }

    con.setRequestMethod("POST");
    con.setRequestProperty("X-ChatWorkToken", this.apiKey);
    con.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");

    String urlParameters = "body=" + message;

    con.setDoOutput(true);

    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    try {
      wr.write(urlParameters.getBytes("utf-8"));
      wr.flush();

    } finally {
      IOUtils.closeQuietly(wr);

    }

    con.connect();

    int responseCode = con.getResponseCode();
    if (responseCode != 200) {
      throw new ChatworkException("Response is not valid. Check your API Key or Chatwork API status. response_code = " + responseCode + ", message = " + con.getResponseMessage());
    }

    return true;
  }

  public boolean isEnabledProxy(){
    if(StringUtils.isEmpty(proxySv) || StringUtils.isEmpty(proxyPort) || StringUtils.equals(proxySv, "NOPROXY")){
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
}
