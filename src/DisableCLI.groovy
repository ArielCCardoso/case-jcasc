@java.lang.Deprecated
import jenkins.model.Jenkins

Jenkins jenkins = Jenkins.getInstanceOrNull()

println "Start...  Disable Jenkins CLI"

jenkins.getDescriptor("jenkins.CLI").get().setEnabled(false)

println "Done... Disable Jenkins CLI"