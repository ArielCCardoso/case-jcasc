import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*

Jenkins jenkins = Jenkins.getInstanceOrNull()
jenkins.save()