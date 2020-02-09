import jenkins.model.Jenkins
import com.microsoft.jenkins.azuread.AzureSecurityRealm

Jenkins jenkins = Jenkins.getInstanceOrNull()

def tenant = System.getenv('AZ_TENANT_ID')
def clientId = System.getenv('AZ_SP_CLIENT_ID')
def clientSecret = System.getenv('AZ_SP_CLIENT_SECRET')

AzureSecurityRealm securityRealm = new AzureSecurityRealm("${tenant}","${clientId}","${clientSecret}", 86400)
securityRealm.setFromRequest(true)
jenkins.setSecurityRealm(securityRealm)
//jenkins.setSecurityRealm(new AzureSecurityRealm("${tenant}","${clientId}","${clientSecret}"))

jenkins.save()
