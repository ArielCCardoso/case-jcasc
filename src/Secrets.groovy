import jenkins.model.*
import groovy.transform.Field
import hudson.util.Secret
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import com.cloudbees.plugins.credentials.domains.*
import com.dabsquared.gitlabjenkins.connection.*
import com.microsoft.azure.util.*


@Field Jenkins jenkins = Jenkins.getInstanceOrNull()
@Field Domain domain = Domain.global()
@Field store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

def createSecretText(String id, String description, String secretText){
    println("Criando a credential: ${description}...")
    secret = new Secret(secretText)
    Credentials strCred = new StringCredentialsImpl(CredentialsScope.GLOBAL, id, description, secret)
    store.addCredentials(domain, strCred)
    println("Crendential Id '${id}' criada e adicionada ao escopo GLOBAL do Jenkins.")
    jenkins.save()
}

def createGitLabAPIToken(String id, String description, String apiToken){
    println("Criando a credential: ${description}...")
    token = new Secret(apiToken)
    Credentials gitlabToken = new GitLabApiTokenImpl(CredentialsScope.GLOBAL, id, description, token)
    store.addCredentials(domain, gitlabToken)
    println("Crendential Id '${id}' criada e adicionada ao escopo GLOBAL do Jenkins.")
    jenkins.save()
}

def createUsernamePassword(String id, String description, String username, String password){
    println("Criando a credential: ${description}...")
    Credentials usernamePassword = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, username, password)
    store.addCredentials(domain, usernamePassword)
    println("Crendential Id '${id}' criada e adicionada ao escopo GLOBAL do Jenkins.")
    jenkins.save()
}

def createCertificate(String id, String description, String password, String keyStoreFile){
    println("Criando a credential: ${description}...")
    CertificateCredentialsImpl.KeyStoreSource keyStore = new CertificateCredentialsImpl.FileOnMasterKeyStoreSource(keyStoreFile)
    Credentials certificate = new CertificateCredentialsImpl(CredentialsScope.GLOBAL, id, description, password, keyStore)
    store.addCredentials(domain, certificate)
    println("Crendential Id '${id}' criada e adicionada ao escopo GLOBAL do Jenkins.")
    jenkins.save()
}

def createAZServicePrincipal(String id, String description, String subscriptionId, String clientId, String clientSecret){
    println("Criando a credential: ${description}...")
    Credentials sp = new AzureCredentials(CredentialsScope.GLOBAL, id, description, subscriptionId, clientId, clientSecret)
    store.addCredentials(domain, sp)
    println("Crendential Id '${id}' criada e adicionada ao escopo GLOBAL do Jenkins.")
    jenkins.save()
}

//GitLab API Token
this.createGitLabAPIToken(
        System.getenv('GITLAB_API_TOKEN_ID'),
        System.getenv('GITLAB_API_TOKEN_DESCRIPTION'),
        System.getenv('GITLAB_API_TOKEN')
)

//Service Principal
this.createAZServicePrincipal(
        System.getenv('AZ_SP_ID'),
        System.getenv('AZ_SP_DESCRIPTION'),
        System.getenv('AZ_SUBSCRIPTION_ID'),
        System.getenv('AZ_SP_CLIENT_ID'),
        System.getenv('AZ_SP_CLIENT_SECRET')
)

//Kubernetes Secret Token
this.createSecretText(
        System.getenv('KUBERNETES_CREDENTIAL_ID'),
        System.getenv('KUBERNETES_CREDENTIAL_DESCRIPTION'),
        System.getenv('KUBERNETES_CREDENTIAL')
)
//More credentials...



