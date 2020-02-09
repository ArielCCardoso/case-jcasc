import jenkins.model.*
import hudson.security.*
import jenkins.security.s2m.AdminWhitelistRule

Jenkins jenkins = Jenkins.getInstanceOrNull()

println "Start...  Enable Master Slave Security"

jenkins.getInjector().getInstance(AdminWhitelistRule.class).setMasterKillSwitch(false)
jenkins.save()

println "Done... Enable Master Slave Security"