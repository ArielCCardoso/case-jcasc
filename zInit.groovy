String cloneDir = "/var/jenkins_home/custom"
String srcDir = cloneDir + "/src"
String jobsDir = cloneDir + "/jobs"

println("Preparando ambiente para configuração do Jenkins, clone do repositório...")
def srcRepo = System.getenv('JENKINS_SRC_REPO')
def stdout = new StringBuilder(), stderr = new StringBuilder()
def gitClone = ["/bin/sh",  "-c",  "git clone ${srcRepo} ${cloneDir}"]
def procClone = gitClone.execute()
procClone.waitForProcessOutput(stdout, stderr)
println(stdout)
println(stderr)

sleep(3000)
println(".")
println("..")
println("...")
println("....")
println(".....")

// TCP Slave Agent
println("Executando o arquivo 'TcpSlaveAgentPort.groovy'...")
evaluate(new File(srcDir,"TcpSlaveAgentPort.groovy"))
println("Arquivo 'TcpSlaveAgentPort.groovy' processado com sucesso.")
println()

// Auth
println("Executando o arquivo 'Auth.groovy'...")
evaluate(new File(srcDir,"Auth.groovy"))
println("Arquivo 'Auth.groovy' processado com sucesso.")
println()

// Secrets
println("Executando o arquivo 'Secrets.groovy'...")
evaluate(new File(srcDir,"Secrets.groovy"))
println("Arquivo 'Secrets.groovy' processado com sucesso.")
println()

// Access Control
println("Executando o arquivo 'AccessControl.groovy'...")
evaluate(new File(srcDir,"AccessControl.groovy"))
println("Arquivo 'AccessControl.groovy' processado com sucesso.")
println()

// Enable CSFR Protection
println("Executando o arquivo 'EnableCSRFProtection.groovy'...")
evaluate(new File(srcDir,"EnableCSRFProtection.groovy"))
println("Arquivo 'EnableCSFRProtection.groovy' processado com sucesso.")
println()

// Executors
println("Executando o arquivo 'Executors.groovy'...")
evaluate(new File(srcDir,"Executors.groovy"))
println("Arquivo 'Executors.groovy' processado com sucesso.")
println()

// Kubernetes Cloud
println("Executando o arquivo 'Kubernetes.groovy'...")
evaluate(new File(srcDir,"Kubernetes.groovy"))
println("Arquivo 'Kubernetes.groovy' processado com sucesso.")
println()

// Global Shared Library
println("Executando o arquivo 'GlobalSharedLibrary.groovy'...")
evaluate(new File(srcDir,"GlobalSharedLibrary.groovy"))
println("Arquivo 'GlobalSharedLibrary.groovy' processado com sucesso.")
println()

// Azure AD
println("Executando o arquivo 'AzureAD.groovy'...")
evaluate(new File(srcDir,"AzureAD.groovy"))
println("Arquivo 'AzureAD.groovy' processado com sucesso.")
println()

// GitLab Connection
println("Executando o arquivo 'GitLabConn.groovy'...")
evaluate(new File(srcDir,"GitLabConn.groovy"))
println("Arquivo 'GitLabConn.groovy' processado com sucesso.")
println()

// YAML to Jobs
println("Executando o arquivo 'Jobs.groovy'...")
evaluate(new File(srcDir,"Jobs.groovy"))
println("Arquivo 'Jobs.groovy' processado com sucesso.")
println()

// Save Config
println("Executando o arquivo 'Save.groovy'...")
evaluate(new File(srcDir,"Save.groovy"))
println("Arquivo 'Save.groovy' processado com sucesso.")

// Role Authorization - TO DO

def stdoutClean = new StringBuilder(), stderrClean = new StringBuilder()
def cleanSrc = ["/bin/sh",  "-c",  "rm -rf ${cloneDir}"]
def procClean = cleanSrc.execute()
procClean.waitForProcessOutput(stdoutClean, stderrClean)
println(stdoutClean)
println(stderrClean)
println()

println("Configuração do JCasC realizada com sucesso!")