package com.vexus2.jenkins.chatwork.jenkinschatworkplugin

import co.freeside.betamax.Betamax
import co.freeside.betamax.MatchRule
import co.freeside.betamax.Recorder
import co.freeside.betamax.TapeMode
import hudson.model.AbstractBuild
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

import static org.mockito.Mockito.*

@RunWith(Enclosed)
class ChatworkClientTest {
  static final String TAPE_NAME = "ChatWork_v1_POST_rooms_messages"

  static class sendMessage {
    ChatworkClient client

    @Rule
    public Recorder recorder = new Recorder()

    @Before
    void setUp(){
      AbstractBuild build = mock(AbstractBuild)
      String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
      String proxySv = "NOPROXY"
      String proxyPort = "80"
      String channelId = "00000000"
      String defaultMessage = "defaultMessage"
      client = new ChatworkClient(build, apiKey, proxySv, proxyPort, channelId, defaultMessage)
    }

    @Betamax(tape=ChatworkClientTest.TAPE_NAME, mode = TapeMode.READ_ONLY, match = [MatchRule.host, MatchRule.path])
    @Test
    void "should send message"(){
      assert client.sendMessage("testMessage") == true
    }

    @Ignore
    @Betamax(tape=ChatworkClientTest.TAPE_NAME, mode = TapeMode.WRITE_ONLY, match = [MatchRule.host, MatchRule.path])
    @Test
    void "call ChatWork API and save response to src/test/resources/betamax/tapes/ChatWork_v1_POST_rooms_messages.yaml"(){
      // TODO: If you want to use, set your actual apiKey and channelId
      client.sendMessage("testMessage")
    }
  }
}
