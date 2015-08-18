package com.vexus2.jenkins.chatwork.jenkinschatworkplugin.api

import co.freeside.betamax.Betamax
import co.freeside.betamax.MatchRule
import co.freeside.betamax.Recorder
import co.freeside.betamax.TapeMode
import org.junit.Rule
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

@RunWith(Enclosed)
class ChatworkClientSpec{
  static class sendMessage extends Specification {
    static final String TAPE_NAME = "ChatWork_v1_POST_rooms_messages"

    ChatworkClient client

    String roomId = "00000000"

    @Rule
    Recorder recorder = new Recorder()

    def setup(){
      String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
      String proxySv = "NOPROXY"
      String proxyPort = "80"
      client = new ChatworkClient(apiKey, proxySv, proxyPort)
      client.setProxyHost("localhost", recorder.getProxyPort())
    }

    @Betamax(tape=ChatworkClientSpec.sendMessage.TAPE_NAME, mode = TapeMode.READ_ONLY, match = [MatchRule.host, MatchRule.path])
    def "sendMessage should send message"(){
      expect:
      client.sendMessage(roomId, "testMessage")
    }

    @Ignore
    @Betamax(tape=ChatworkClientSpec.sendMessage.TAPE_NAME, mode = TapeMode.WRITE_ONLY, match = [MatchRule.host, MatchRule.path])
    def "call ChatWork API and save response to src/test/resources/betamax/tapes/ChatWork_v1_POST_rooms_messages.yaml"(){
      expect:
      // TODO: If you want to use, set your actual apiKey and roomId
      client.sendMessage(roomId, "testMessage")
    }
  }

  static class enabledProxy extends Specification {
    @Unroll
    def "proxySv=#proxySv, proxyPort=#proxyPort: isEnabledProxy() == #expected"(){
      setup:
      ChatworkClient client = new ChatworkClient("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", proxySv, proxyPort)

      expect:
      client.isEnabledProxy() == expected

      where:
      proxySv     | proxyPort || expected
      "NOPROXY"   | ""        || false
      "NOPROXY"   | "80"      || false
      "localhost" | "80"      || true
      "localhost" | "str"     || false
      "localhost" | ""        || false
      ""          | "80"      || false
      "localhost" | " "       || false
      " "         | "80"      || false
      ""          | ""        || false
      null        | "80"      || false
      null        | ""        || false
    }
  }
}
