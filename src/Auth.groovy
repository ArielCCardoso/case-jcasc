import jenkins.model.*
import hudson.security.*

def JENKINS_USER = System.getenv('JENKINS_USER') ?: 'admin'
def JENKINS_PASSWORD = System.getenv('JENKINS_PASSWORD') ?: 'password'

Jenkins jenkins = Jenkins.getInstanceOrNull()
jenkins.setSecurityRealm(new HudsonPrivateSecurityRealm(false))
jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy())

def user = jenkins.getSecurityRealm().createAccount(JENKINS_USER, JENKINS_PASSWORD)
user.save()

def strategy = new hudson.security.FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
jenkins.setAuthorizationStrategy(strategy)

jlc = JenkinsLocationConfiguration.get()
jlc.setUrl(System.getenv('JENKINS_ENDPOINT') ?: 'https://jenkins.localhost')
jlc.setAdminAddress(System.getenv('JENKINS_ADMIN_ADDRESS') ?: 'arielccardoso@live.com')
jlc.save()

jenkins.save()