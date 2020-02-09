import hudson.model.*
import jenkins.model.*

Jenkins jenkins = Jenkins.getInstanceOrNull()
HashSet<String> newProtocols = new HashSet<>(jenkins.getAgentProtocols());
newProtocols.removeAll(Arrays.asList(
        "JNLP3-connect", "JNLP2-connect", "JNLP-connect", "CLI-connect"
))
jenkins.setAgentProtocols(newProtocols)
jenkins.save()

Thread.start {
      sleep 10000
      println "--> setting agent port for jnlp"
      def env = System.getenv()
      int port = env['JENKINS_SLAVE_AGENT_PORT'].toInteger()
      //Jenkins.instance.setSlaveAgentPort(port)
      Jenkins.instanceOrNull.setSlaveAgentPort(port)
      println "--> setting agent port for jnlp... done"
}