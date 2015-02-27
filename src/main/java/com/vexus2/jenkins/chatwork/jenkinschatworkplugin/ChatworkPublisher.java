package com.vexus2.jenkins.chatwork.jenkinschatworkplugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.VariableResolver;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.HashMap;
import java.util.Map;

public class ChatworkPublisher extends Publisher {
  private static final int MAX_COMMIT_MESSAGE_LENGTH = 50;

  private final String rid;

  @Deprecated
  private final String defaultMessage;

  private final String successfulMessage;
  private final String failureMessage;
  private final String unstableMessage;
  private final String notBuiltMessage;
  private final String abortedMessage;

  private final Boolean notifyOnSuccess;
  private final Boolean notifyOnFail;
  private final Boolean notifyOnUnstable;
  private final Boolean notifyOnNotBuilt;
  private final Boolean notifyOnAborted;

  private transient AbstractBuild build;
  private transient BuildListener listener;


  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
  @DataBoundConstructor
  public ChatworkPublisher(String rid, String defaultMessage, Boolean notifyOnSuccess, Boolean notifyOnFail, String unstableMessage, String notBuiltMessage, String abortedMessage, String successfulMessage, String failureMessage, Boolean notifyOnUnstable, Boolean notifyOnNotBuilt, Boolean notifyOnAborted) {
    this.rid = rid;
    this.defaultMessage = StringUtils.trimToEmpty(defaultMessage);

    this.successfulMessage = StringUtils.trimToEmpty(successfulMessage);
    this.failureMessage    = StringUtils.trimToEmpty(failureMessage);
    this.unstableMessage   = StringUtils.trimToEmpty(unstableMessage);
    this.notBuiltMessage   = StringUtils.trimToEmpty(notBuiltMessage);
    this.abortedMessage    = StringUtils.trimToEmpty(abortedMessage);

    this.notifyOnSuccess  = notifyOnSuccess;
    this.notifyOnFail     = notifyOnFail;
    this.notifyOnUnstable = notifyOnUnstable;
    this.notifyOnNotBuilt = notifyOnNotBuilt;
    this.notifyOnAborted  = notifyOnAborted;
  }

  /**
   * We'll use this from the <tt>config.jelly</tt>.
   */
  public String getRid() {
    return rid;
  }

  @Deprecated
  public String getDefaultMessage() {
    return defaultMessage;
  }

  public String getSuccessfulMessage() {
    return getMessageWithDefault(successfulMessage);
  }

  public String getFailureMessage() {
    return getMessageWithDefault(failureMessage);
  }

  public String getUnstableMessage() {
    return getMessageWithDefault(unstableMessage);
  }

  public String getNotBuiltMessage() {
    return getMessageWithDefault(notBuiltMessage);
  }

  public String getAbortedMessage() {
    return getMessageWithDefault(abortedMessage);
  }

  private String getMessageWithDefault(String message){
    // NOTE: backward compatibility
    // if message is null, this plugin upgraded from <= v0.6.2
    return message == null ? defaultMessage : message;
  }

  public Boolean getNotifyOnSuccess() {
    return notifyOnSuccess;
  }

  public Boolean getNotifyOnFail() {
    return notifyOnFail;
  }

  public Boolean getNotifyOnUnstable() {
    return notifyOnUnstable;
  }

  public Boolean getNotifyOnNotBuilt() {
    return notifyOnNotBuilt;
  }

  public Boolean getNotifyOnAborted() {
    return notifyOnAborted;
  }

  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    this.build = build;
    this.listener = listener;
    
    if(this.build.getResult() == Result.SUCCESS && !this.notifyOnSuccess) {
      println("skip post message because notifyOnSuccess is disabled");
      return true;
    }
    if(this.build.getResult() == Result.FAILURE && !this.notifyOnFail) {
      println("skip post message because notifyOnFail is disabled");
      return true;
    }
    if(this.build.getResult() == Result.UNSTABLE && !this.notifyOnUnstable) {
      println("skip post message because notifyOnUnstable is disabled");
      return true;
    }
    if(this.build.getResult() == Result.NOT_BUILT && !this.notifyOnNotBuilt) {
      println("skip post message because notifyOnNotBuilt is disabled");
      return true;
    }
    if(this.build.getResult() == Result.ABORTED && !this.notifyOnAborted) {
      println("skip post message because notifyOnAborted is disabled");
      return true;
    }

    String message = resolveMessage();

    println("[ChatWork post message]");
    println(message);

    if (message == null) {
      return false;
    }

    try {
      ChatworkClient chatworkClient = new ChatworkClient(getDescriptor().getApikey(), getDescriptor().getProxysv(), getDescriptor().getProxyport(), resolveRoomId());
      chatworkClient.sendMessage(message);

      return true;
    } catch (Exception e) {
      e.printStackTrace(listener.getLogger());
      return false;
    }
  }

  // print to build console
  private void println(String message) {
    this.listener.getLogger().println(message);
  }

  private String resolveMessage() {
    return resolve(getJobResultMessage(build.getResult()));
  }

  private String getJobResultMessage(Result result) {
    if(result == Result.SUCCESS){
      return getSuccessfulMessage();
    } else if(result == Result.FAILURE){
      return getFailureMessage();
    } else if(result == Result.UNSTABLE){
      return getUnstableMessage();
    } else if(result == Result.NOT_BUILT){
      return getNotBuiltMessage();
    } else if(result == Result.ABORTED){
      return getAbortedMessage();
    }

    return this.defaultMessage;
  }

  private String resolveRoomId() {
    return resolve(this.rid);
  }

  private String resolve(String message) {
    if(StringUtils.isBlank(message)){
      return null;
    }

    Map<String, String> extraVariables = createExtraVariables();
    return BuildVariableUtil.resolve(message, build, listener, extraVariables);
  }

  private Map<String, String> createExtraVariables() {
    Map<String, String> variables = new HashMap<String, String>();

    VariableResolver<String> buildVariableResolver = build.getBuildVariableResolver();
    String payloadJson = buildVariableResolver.resolve("payload");
    if(StringUtils.isNotBlank(payloadJson)){
      variables.put("PAYLOAD_SUMMARY", analyzePayload(payloadJson));
    }

    variables.put("BUILD_RESULT", build.getResult().toString());

    return variables;
  }

  private static String analyzePayload(String payloadJson) {
    JSONObject json;
    try{
      json = JSONObject.fromObject(payloadJson);

    } catch (JSONException e){
      // payloadJson is not json
      return payloadJson;
    }

    if (json.has("action") && "opened".equals(json.getString("action"))) {
      JSONObject pullRequest = json.getJSONObject("pull_request");
      String title = pullRequest.getString("title");
      String url = pullRequest.getString("html_url");
      String repositoryName = json.getJSONObject("repository").getString("name");
      String pusher = pullRequest.getJSONObject("user").getString("login");

      StringBuilder message = new StringBuilder().append(String.format("%s created Pull Request into %s,\n", pusher, repositoryName));
      message.append(String.format("\n%s", title));
      message.append(String.format("\n%s", url));

      return message.toString();

    } else if(json.has("compare")){
      String compareUrl = json.getString("compare");

      String pusher = json.getJSONObject("pusher").getString("name");
      String repositoryName = json.getJSONObject("repository").getString("name");
      StringBuilder message = new StringBuilder().append(String.format("%s pushed into %s,\n", pusher, repositoryName));

      JSONArray commits = json.getJSONArray("commits");
      int size = commits.size();
      for (int i = 0; i < size; i++) {
        JSONObject value = (JSONObject) commits.get(i);
        String s = value.getString("message");
        message.append(String.format("- %s\n", (s.length() > MAX_COMMIT_MESSAGE_LENGTH) ? s.substring(0, MAX_COMMIT_MESSAGE_LENGTH) + "..." : s));
      }
      message.append(String.format("\n%s", compareUrl));

      return message.toString();
    }

    return null;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  /**
   * Descriptor for {@link ChatworkPublisher}. Used as a singleton.
   * The class is marked as public so that it can be accessed from views.
   * <p/>
   * <p/>
   * See <tt>src/main/resource/com.vexus2.jenkins.chatwork.jenkinschatworkplugin/ChatworkPublisher/*.jelly</tt>
   * for the actual HTML fragment for the configuration screen.
   */
  @Extension
  // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private String apikey;

    public String getApikey() {
      return apikey;
    }

    private String proxysv;

    public String getProxysv() {
      return proxysv;
    }

    private String proxyport;

    public String getProxyport() {
      return proxyport;
    }
    
    private String globalSuccessfulMessage;

    private String globalFailureMessage;

    private String globalUnstableMessage;

    private String globalNotBuiltMessage;

    private String globalAbortedMessage;

    public String getGlobalSuccessfulMessage() {
      return globalSuccessfulMessage;
    }

    public String getGlobalFailureMessage() {
      return globalFailureMessage;
    }

    public String getGlobalUnstableMessage() {
      return globalUnstableMessage;
    }

    public String getGlobalNotBuiltMessage() {
      return globalNotBuiltMessage;
    }

    public String getGlobalAbortedMessage() {
      return globalAbortedMessage;
    }

    public DescriptorImpl() {
      load();
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    public String getDisplayName() {
      return "Notify the ChatWork";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      apikey = formData.getString("apikey");
      proxysv = formData.getString("proxysv");
      proxyport = formData.getString("proxyport");

      globalSuccessfulMessage = formData.getString("globalSuccessfulMessage");
      globalFailureMessage    = formData.getString("globalFailureMessage");
      globalUnstableMessage   = formData.getString("globalUnstableMessage");
      globalNotBuiltMessage   = formData.getString("globalNotBuiltMessage");
      globalAbortedMessage    = formData.getString("globalAbortedMessage");

      save();
      return super.configure(req, formData);
    }
  }
}
