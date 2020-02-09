import org.csanchez.jenkins.plugins.kubernetes.*
import org.csanchez.jenkins.plugins.kubernetes.model.*
import org.csanchez.jenkins.plugins.kubernetes.pod.retention.Never
import org.csanchez.jenkins.plugins.kubernetes.volumes.*
import jenkins.model.*
import groovy.transform.Field

Jenkins jenkins = Jenkins.getInstanceOrNull()

def jenkinsUrl = System.getenv('JENKINS_ENDPOINTa') ?: "https://jenkins-dev.arielcardoso.net"
def jenkinsTunnel = System.getenv('JENKINS_TUNNEL') ?: "http://jenkins:50000"

def k8sName = System.getenv('KUBERNETES_NAME') ?: "DevOps-k8s"
def k8sServerUrl = System.getenv('KUBERNETES_SERVER_URL') ?: "https://dev-aks-arielccardoso-472ae070.hcp.eastus.azmk8s.io"
def k8sNamespace = System.getenv('KUBERNETES_NAMESPACE') ?: "default"
def k8sCredentialId = System.getenv('KUBERNETES_CREDENTIAL_ID') ?: "aks-auth"

KubernetesCloud k8s = new KubernetesCloud(k8sName.toString())
k8s.setServerUrl(k8sServerUrl.toString())
k8s.setNamespace(k8sNamespace.toString())
k8s.setJenkinsUrl(jenkinsUrl.toString())
k8s.setCredentialsId(k8sCredentialId.toString())
//k8s.setSkipTlsVerify(false)
k8s.setSkipTlsVerify(true)
k8s.setConnectTimeout(30)
k8s.setContainerCapStr("200")
k8s.setRetentionTimeout(60)
k8s.setReadTimeout(0)
k8s.setWaitForPodSec(60)
Never n = new Never()
k8s.setPodRetention(n)
k8s.setJenkinsTunnel(jenkinsTunnel.toString())

/*
      Volumes
*/
// Socket Docker
HostPathVolume dockerSocket = new HostPathVolume("/var/run/docker.sock","/var/run/docker.sock")
// SSH keys and config
//SecretVolume ssh = new SecretVolume("/var/run/secrets/.ssh","jenkins-ssh")
// .gitconfig
//ConfigMapVolume gitConfig = new ConfigMapVolume("/var/run/secrets/.git","jenkins-gitconfig")
// kubectl m2 repo
//PersistentVolumeClaim m2 = new PersistentVolumeClaim("/root/.m2","pvc-m2",false)
// Npm repo
//podT.volumes.add(new PersistentVolumeClaim("TBD","pvc-npm",false))
// Nuget repo
//podT.volumes.add(new PersistentVolumeClaim("TBD","pvc-nuget",false)

// Secrets
//List<PodImagePullSecret> secrets = new ArrayList<PodImagePullSecret>()
//secrets.add(new PodImagePullSecret("auth-docker"))

// Vars Global
@Field List<TemplateEnvVar> vars = new ArrayList<TemplateEnvVar>()
vars.add(new SecretEnvVar("SSH_PRIVATE_KEY","jenkins","SSH_PRIVATE_KEY"))
vars.add(new SecretEnvVar("SSH_PUBLIC_KEY","jenkins","SSH_PUBLIC_KEY"))
vars.add(new SecretEnvVar("SSH_CONFIG","jenkins","SSH_CONFIG"))
vars.add(new SecretEnvVar("GIT_CONFIG","jenkins","GIT_CONFIG"))

List<ContainerTemplate> getContainers(String slave) {

    List<ContainerTemplate> containers = new ArrayList<ContainerTemplate>()
    ContainerTemplate jnlp = new ContainerTemplate("jnlp", "arielccardoso/jcasc-jnlp:3.35-5-alpine", '/entrypoint', '${computer.jnlpmac} ${computer.name}')
    jnlp.setAlwaysPullImage(true)
    jnlp.setWorkingDir("/builds")
    containers.add(jnlp)

    ContainerTemplate azCli = new ContainerTemplate("azure-cli", "arielccardoso/azure-cli:2.x","/entrypoint", "tail -f /dev/null")
    azCli.setTtyEnabled(true)
    azCli.setAlwaysPullImage(true)
    azCli.setWorkingDir("/builds")
    List<TemplateEnvVar> azVars = vars
    azVars.add(new SecretEnvVar("AZ_TENANT_ID","jenkins","AZ_TENANT_ID"))
    azVars.add(new SecretEnvVar("AZ_SUBSCRIPTION_ID","jenkins","AZ_SUBSCRIPTION_ID"))
    azVars.add(new SecretEnvVar("AZ_CLIENT_ID","jenkins","AZ_SP_CLIENT_ID"))
    azVars.add(new SecretEnvVar("AZ_CLIENT_SECRET","jenkins","AZ_SP_CLIENT_SECRET"))
    azCli.setEnvVars(azVars)
    containers.add(azCli)

    ContainerTemplate docker = new ContainerTemplate("docker", "arielccardoso/docker:1.0.0","/entrypoint", "tail -f /dev/null")
    docker.setTtyEnabled(true)
    docker.setAlwaysPullImage(true)
    docker.setWorkingDir("/builds")
    List<TemplateEnvVar> dockerVars = vars
    dockerVars.add(new SecretEnvVar("DOCKER_AUTH","jenkins","DOCKER_AUTH"))
    docker.setEnvVars(dockerVars)
    containers.add(docker)

    if(slave == "kubectl") {
        ContainerTemplate kubectl = new ContainerTemplate("kubectl", "arielccardoso/kubectl:1.14.8", "/entrypoint", "tail -f /dev/null")
        kubectl.setTtyEnabled(true)
        kubectl.setAlwaysPullImage(true)
        kubectl.setWorkingDir("/builds")
        kubectl.setEnvVars(vars)
        containers.add(kubectl)
    } else if(slave == "terraform") {
        ContainerTemplate terraform = new ContainerTemplate("terraform", "arielccardoso/terraform:0.12.20", "/entrypoint", "tail -f /dev/null")
        terraform.setTtyEnabled(true)
        terraform.setAlwaysPullImage(true)
        terraform.setWorkingDir("/builds")
        terraform.setEnvVars(vars)
        containers.add(terraform)
    } else {
        println("O valor fornecido para o slave é invalido! Slave: " + slave)
    }

    return containers
}

/*
      Pod templates
*/

// kubectl Template
PodTemplate podKubectl = new PodTemplate()
podKubectl.setName("Kubectl Slave")
podKubectl.setLabel("kubectl")
podKubectl.setNamespace(k8sNamespace)
//podKubectl.setImagePullSecrets(secrets)
podKubectl.setInstanceCap(100)
podKubectl.setContainers(getContainers("kubectl"))
podKubectl.volumes.add(dockerSocket)

// Azure Functions Template
PodTemplate podTerraform = new PodTemplate()
podTerraform.setName("Terraform Slave")
podTerraform.setLabel("terraform")
podTerraform.setNamespace(k8sNamespace)
//podTerraform.setImagePullSecrets(secrets)
podTerraform.setInstanceCap(100)
podTerraform.setContainers(getContainers("terraform"))
podTerraform.volumes.add(dockerSocket)

k8s.addTemplate(podKubectl)
k8s.addTemplate(podTerraform)
//jenkins.clouds.add(k8s)
jenkins.clouds.replace(k8s)

println "Kubernetes Cloud adicionado..."
jenkins.save()
