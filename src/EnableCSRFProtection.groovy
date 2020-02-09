import hudson.security.csrf.DefaultCrumbIssuer
import jenkins.model.Jenkins

Jenkins instance = Jenkins.getInstanceOrNull()
instance.setCrumbIssuer(new DefaultCrumbIssuer(true))
instance.save()