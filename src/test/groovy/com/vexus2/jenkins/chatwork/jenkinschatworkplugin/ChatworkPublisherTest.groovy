package com.vexus2.jenkins.chatwork.jenkinschatworkplugin

import hudson.EnvVars
import hudson.model.AbstractBuild
import hudson.model.Result
import hudson.model.TaskListener
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

import static org.mockito.Matchers.any
import static org.mockito.Mockito.*

@RunWith(Enclosed)
class ChatworkPublisherTest {
  static String readFixture(String fileName){
    ChatworkPublisherTest.class.getClassLoader().getResource("com/vexus2/jenkins/chatwork/jenkinschatworkplugin/${fileName}").getText()
  }

  static class analyzePayload {
    @Test
    void "should create PullRequest message"(){
      // via. https://developer.github.com/v3/pulls/#get-a-single-pull-request
      String parameterDefinition = readFixture("payload_PullRequest.json")

      String expected = """
octocat created Pull Request into Hello-World,

new-feature
https://github.com/octocat/Hello-World/pull/1
""".trim()

      assert ChatworkPublisher.analyzePayload(parameterDefinition) == expected
    }

    @Test
    void "should create compare message"(){
      String parameterDefinition = readFixture("payload_compare.json")

      String expected = """
octocat pushed into Hello-World,
- 1st commit
- 2nd commit

https://github.com/octocat/Hello-World/compare/master...topic
""".trim()

      assert ChatworkPublisher.analyzePayload(parameterDefinition) == expected
    }

    @Test
    void "When neither PullRequest nor compare, should return null"(){
      String parameterDefinition = readFixture("payload_empty.json")
      assert ChatworkPublisher.analyzePayload(parameterDefinition) == null
    }
  }

  static class resolveMessage {
    ChatworkPublisher publisher

    @Before
    void setUp(){
      // via. https://gist.github.com/gjtorikian/5171861
      String payload = readFixture("payload_webhook.json")

      String roomId = "00000000"
      String defaultMessage = '$PAYLOAD_SUMMARY'
      boolean notifyOnSuccess = true
      boolean notifyOnFail = true
      publisher = new ChatworkPublisher(roomId, defaultMessage, notifyOnSuccess, notifyOnFail)

      AbstractBuild mockBuild = mock(AbstractBuild)
      when(mockBuild.getBuildVariables()).thenReturn(['payload': payload])
      when(mockBuild.getEnvironment(any(TaskListener))).thenReturn(new EnvVars(['JAVA_HOME': '/Library/Java/JavaVirtualMachines/1.7.0u.jdk/Contents/Home']))
      when(mockBuild.getResult()).thenReturn(Result.SUCCESS)

      publisher.build = mockBuild
    }

    @Test
    void "When contains payload param, should resolve payload"(){
      String excepted = """
Garen Torikian pushed into testing,
- Test
- This is me testing the windows client.
- Rename madame-bovary.txt to words/madame-bovary.tx...

https://github.com/octokitty/testing/compare/17c497ccc7cc...1481a2de7b2a
""".trim()

      assert publisher.resolveMessage() == excepted
    }
  }
}
