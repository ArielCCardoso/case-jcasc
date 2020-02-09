import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType
import org.kohsuke.stapler.Stapler
import org.kohsuke.stapler.StaplerResponse
@Grab('org.yaml:snakeyaml:1.15')
import org.yaml.snakeyaml.Yaml
import groovy.transform.Field
import com.cloudbees.hudson.plugins.folder.Folder
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import jenkins.model.*
import hudson.model.*
import hudson.tasks.LogRotator
import hudson.triggers.*
import jenkins.triggers.*
import groovy.io.FileType
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty
import com.dabsquared.gitlabjenkins.GitLabPushTrigger
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest
import org.kohsuke.stapler.Stapler
import org.kohsuke.stapler.StaplerResponseWrapper
import java.net.URL

/*
    Global variables
*/
@Field Jenkins jenkins = Jenkins.getInstanceOrNull()
@Field def project = []
@Field def global_parameters = []
@Field def environments = []
@Field def subprojects = []
@Field def jobs = []
@Field Yaml yaml = new Yaml()
@Field def object = null
@Field String yamlFile = null
@Field String yamlDir = '/var/jenkins_home/custom/jobs'

/*
    Global Methods
*/

def getProject(Class type, DirectlyModifiableTopLevelItemGroup parent, String name){
    if(parent != null){
        search = parent.fullName + "/" + name
    } else {
        search = name
    }
    _project = jenkins.getItemByFullName(search, type)
    if(_project == null){
        println("Criando o projeto: " + search + ".")
        _project = jenkins.createProject(type,name)
        if(parent != null){
            println("Movendo projeto...")
            hudson.model.Items.move(_project,parent)
        }
    }
    return _project
}

def removeDuplicatesParameters(Object parameters, Object pName){
    Boolean exists = false
    parameters.each{
        p -> if(p.name.toString() == pName.toString()){
            println("A configuração de parametros possui override para o parametro global: ${pName}")
            exists = true
        }
    }
    return exists
}

def setBuildNumber(Job job){
    String urlJobNumber = "https://nexus.localhost/repository/jenkins-jobs-build-number/" + job.fullName

    URL url = new URL(urlJobNumber)
    HttpURLConnection conn = (HttpURLConnection) url.openConnection()
    int responseCode = conn.getResponseCode()

    if(responseCode == 200){
        def stdout = new StringBuilder(), stderr = new StringBuilder()
        cmd = "curl --silent --request GET " + urlJobNumber
        def procGetBuildNumber = cmd.execute()
        procGetBuildNumber.waitForProcessOutput(stdout, stderr)
        //Integer nextBuildNumber = Integer.parseInt(stdout.toString().trim().toInteger()) + 1
        Integer nextBuildNumber = stdout.toString().trim().toInteger() + 1
        job.updateNextBuildNumber(nextBuildNumber)
        println("O BuildNumber do job ${job.displayName} foi definido em: " + nextBuildNumber.toString())
    }
}

def configureParameters(Object parameters, Object globalParameters){
    globalParameters.removeAll {
        if(removeDuplicatesParameters(parameters, it.name)){
            return true
        }
        return false
    }
    parameters.addAll(globalParameters)

    List<ParameterDefinition> parameterDefinitions = []
    for( p in parameters){
        if(p.type.toString() == "Choice") {
            String name = p.name.toString()
            String desc = p.description.toString()
            List<String> listValues = new ArrayList<String>()
            String[] values = new String[p.values.size()]
            for (v in p.values) {
                listValues.add(v.toString())
            }
            values = listValues.toArray(values)
            parameterDefinitions.add(new ChoiceParameterDefinition(name, values, desc))
        } else if (p.type.toString() == "Password"){
            String name = p.name.toString()
            String desc = p.description.toString()
            String password = p.values[0].toString()
            parameterDefinitions.add(new PasswordParameterDefinition(name, password, desc))
        }
    }
    return parameterDefinitions
}

def configureTriggers(Object triggers){
    ArrayList<Trigger> listTriggers = []
    for(t in triggers){
        if(t.type == "GitLabPushTrigger"){
            println("Configurando o trigger do tipo: ${t.type}.")
            GitLabPushTrigger gitLabPushTrigger = new GitLabPushTrigger()
            gitLabPushTrigger.setTriggerOpenMergeRequestOnPush(TriggerOpenMergeRequest.never)
            gitLabPushTrigger.setBranchFilterType(BranchFilterType.All)
            gitLabPushTrigger.setIncludeBranchesSpec("")
            gitLabPushTrigger.setExcludeBranchesSpec("")
            gitLabPushTrigger.setSourceBranchRegex("")
            gitLabPushTrigger.setTargetBranchRegex("")
            gitLabPushTrigger.setPendingBuildName("")
            t.each{
                key, value -> if(key.toString() == "type"){
                    return
                } else {
                    println("Trigger configurado com o paramentro '${key}': ${value}")
                    if (value != null) {
                        if (key.toString() == "secretToken") {
                            gitLabPushTrigger.setSecretToken(value.toString())
                        } else if (key.toString() == "triggerOnPush") {
                            gitLabPushTrigger.setTriggerOnPush(value.toString().toBoolean())
                        } else if (key.toString() == "triggerOnMergeRequest") {
                            gitLabPushTrigger.setTriggerOnMergeRequest(value.toString().toBoolean())
                        } else if (key.toString() == "triggerOnPipelineEvent") {
                            gitLabPushTrigger.setTriggerOnPipelineEvent(value.toString().toBoolean())
                        } else if (key.toString() == "triggerOnAcceptedMergeRequest") {
                            gitLabPushTrigger.setTriggerOnAcceptedMergeRequest(value.toString().toBoolean())
                        } else if (key.toString() == "triggerOnClosedMergeRequest") {
                            gitLabPushTrigger.setTriggerOnClosedMergeRequest(value.toString().toBoolean())
                        } else if (key.toString() == "triggerOnApprovedMergeRequest") {
                            gitLabPushTrigger.setTriggerOnApprovedMergeRequest(value.toString().toBoolean())
                        } else if (key.toString() == "triggerOpenMergeRequestOnPush") {
                            if (value.toString() == "never") {
                                gitLabPushTrigger.setTriggerOpenMergeRequestOnPush(TriggerOpenMergeRequest.never)
                            } else if (value.toString() == "source") {
                                gitLabPushTrigger.setTriggerOpenMergeRequestOnPush(TriggerOpenMergeRequest.source)
                            } else if (value.toString() == "both") {
                                gitLabPushTrigger.setTriggerOpenMergeRequestOnPush(TriggerOpenMergeRequest.both)
                            } else {
                                println("O valor da chave ${key} é inválido. Fornecer um dos seguintes valores: never || source || both")
                            }
                        } else if (key.toString() == "triggerOnNoteRequest") {
                            gitLabPushTrigger.setTriggerOnNoteRequest(value.toString().toBoolean())
                        } else if (key.toString() == "noteRegex") {
                            gitLabPushTrigger.setNoteRegex(value.toString())
                        } else if (key.toString() == "ciSkip") {
                            gitLabPushTrigger.setCiSkip(value.toString().toBoolean())
                        } else if (key.toString() == "skipWorkInProgressMergeRequest") {
                            gitLabPushTrigger.setSkipWorkInProgressMergeRequest(value.toString().toBoolean())
                        } else if (key.toString() == "setBuildDescription") {
                            gitLabPushTrigger.setSetBuildDescription(value.toString().toBoolean())
                        } else {
                            /* TODO implements more properties
                            if(key.toString() == "branchFilterType"){
                                gitLabPushTrigger. (value.toString())
                            }
                            */
                            println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                            println("A chave '${key}' é inválida ou o código não está pronto para configurá-la.")
                            println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                        }
                    } else {
                        println("O trigger ${t.type} não possui o valor para chave '${key}'.")
                    }
                }
            }
            listTriggers.add(gitLabPushTrigger)
        } else if(t.type == "ReverseBuildTrigger"){
            println("Configurando o trigger do tipo: ${t.type}.")
            if(t.upstreamProjects != null) {
                ReverseBuildTrigger reverseBuildTrigger = new ReverseBuildTrigger(t.upstreamProjects.toString())
                listTriggers.add(reverseBuildTrigger)
            } else {
                println("O trigger ${t.type} não possui o valor do agendamento. A chave 'upstreamProjects' está vazia ou não existe.")
            }
        } else if(t.type == "TimerTrigger"){
            println("Configurando o trigger do tipo: ${t.type}.")
            if(t.spec != null) {
                TimerTrigger timerTrigger = new TimerTrigger(t.spec.toString())
                listTriggers.add(timerTrigger)
            } else {
                println("O trigger ${t.type} não possui o valor do agendamento. A chave 'spec' está vazia ou não existe.")
            }
        } else if(t.type == "GitHubTrigger"){
            println("TODO: ${t.type}.")
        } else{
            println("O tipo de trigger '${t.type}' é desconhecido ou não aceito para configuração. Verifique o arquivo ${yamlFile}.")
        }
    }
    return listTriggers
}

def configureJob(Class type, DirectlyModifiableTopLevelItemGroup parent, LinkedHashMap job){

    prj = this.getProject(type, parent, job.name.toString())

    if(prj instanceof Folder){
        println("Projeto do tipo 'Folder'.")
        prj as Folder
    } else if(prj instanceof WorkflowJob){
        println("Projeto do tipo 'WorkflowJob'.")
        prj as WorkflowJob

        // Scm
        if(job.containsKey("scm")) {
            def scm = []
            scm = job.scm
            if(scm.size() != 0) {
                GitSCM gitScm = new GitSCM(scm.url.toString())
                gitScm.branches = [new BranchSpec(scm.branch.toString())]
                prj.setDefinition(new CpsScmFlowDefinition(gitScm, scm.script.toString()))
                println("Adicionada a configuração de Scm no job: ${job.display_name}")
            } else {
                println("A chave 'scm' está vazia para o Job '${job.displayName}'.")
            }
        } else {
            println("O Job '${job.displayName}' não possui nenhum SCM configurado.")
        }

        // GitLab Connection
        if(job.containsKey("gitlab_connection")){
            if(job.gitlab_connection.toString() != null) {
                GitLabConnectionProperty gitLabConn = new GitLabConnectionProperty(job.gitlab_connection.toString())
                prj.addProperty(gitLabConn)
                println("Adicionado a GitLab Connection no job: ${job.display_name}")
                //prj.save()
            } else {
                println("A chave 'gitlab_connection' está vazia para o Job '${job.displayName}'.")
            }
        } else {
            println("O Job '${job.displayName}' não possui nenhuma GitLab Connection.")
        }

        // Build Discard
        if(job.containsKey("build_discard")){
            def logRotation = []
            logRotation = job.build_discard
            if(logRotation.size() != 0) {
                prj.setLogRotator(new LogRotator(logRotation.daysToKeep.toInteger(),logRotation.buildsToKeep.toInteger()))
                println("Configurado Log Rotate no job: ${job.display_name}")
            } else {
                println("A chave 'build_discard' está vazia para o Job '${job.displayName}'.")
            }
        } else {
            println("O Job '${job.display_name}' não possui nenhuma configuração de Log Rotation.")
        }

        // Parameters
        if(job.containsKey("parameters")){
            def parameters = job.parameters
            def global = global_parameters
            if(parameters.size() != 0) {
                p = this.configureParameters(parameters, global)
                prj.addProperty(new ParametersDefinitionProperty(p))
                println("Configurado Parâmetros no job: ${job.display_name}")
            } else {
                println("A chave 'parameters' está vazia para o Job '${job.displayName}'.")
            }
        } else {
            println("O Job '${job.displayName}' não possui nenhuma configuração de Parâmetros.")
        }

        // Triggers
        if(job.containsKey("triggers")){
            triggers = job.triggers
            if(triggers.size() != 0){
                ArrayList<Trigger> listTriggers = this.configureTriggers(triggers)
                for(t in listTriggers){
                    prj.addTrigger(t)
                }
                println("Configurado Triggers no job: ${job.display_name}")
            } else {
                println("A chave 'triggers' está vazia para o Job '${job.displayName}'.")
            }
        } else {
            println("O Job '${job.displayName}' não possui nenhuma configuração de Triggers.")
        }

        // Build Number
        //setBuildNumber(prj)
        prj.save()

        /*// Clear Secret Token
        def tt = prj.getTriggers().get(jenkins.getDescriptorOrDie(GitLabPushTrigger.class))
        tt.setSecretToken("")
        prj.save()*/

    } else {
        println("O tipo de classe do Job informado não é permitido! Class: " + type.name) // type.name = org.jenkinsci.plugins.workflow.job.WorkflowJob || type.simpleName = WorkflowJob
    }

    // Display
    prj.setDisplayName(job.display_name.toString())
    prj.setDescription(job.description.toString())
    prj.save()

    println("Projeto ${prj.fullName} configurado!")
    return prj
}

/*
    Main
*/

def Main(String file){
    try {
        println()
        println("Iniciando o processamento do arquivo: " + file)

        object = yaml.load(new FileReader(file))
        if (object.containsKey("project")) {
            project = object.project
        } else {
            println("O arquivo ${yamlFile} está inconsistente, não possui valores para 'project'... ele não será processado.")
            return
        }
        if (project.containsKey("global_parameters")) {
            global_parameters = project.global_parameters
        } else {
            println("O arquivo ${yamlFile} está inconsistente, não possui valores para 'global_parameters'... ele não será processado.")
            return
        }
        if (project.containsKey("environments")) {
            environments = project.environments
        } else {
            println("O arquivo ${yamlFile} está inconsistente, não possui valores para 'environments'... ele não será processado.")
            return
        }

        println("Iniciando o processamento do projeto: " + project.name.toString())
        Folder prjFolder = this.configureJob(Folder, null, project)
        if (environments.size() != 0) {
            for (env in environments) {
                Folder envProject = this.configureJob(Folder, prjFolder, env)
                if (env.containsKey("subprojects")) {
                    subprojects = env.subprojects
                    if (subprojects.size() != 0) {
                        for (sub in subprojects) {
                            Folder subProject = this.configureJob(Folder, envProject, sub)
                            if (sub.containsKey("jobs")) {
                                jobs = sub.jobs
                                if (jobs.size() != 0) {
                                    for (job in jobs) {
                                        WorkflowJob jobProject = this.configureJob(WorkflowJob, subProject, job)
                                    }
                                } else {
                                    println("O arquivo ${yamlFile} está inconsistente, não possui valores para 'jobs'... Nenhum job foi criado, somente o Projeto.")
                                }
                            } else {
                                println("O arquivo ${yamlFile} está inconsistente, não possui a chave 'jobs'... Nenhum job foi criado, somente o Projeto.")
                            }
                        }
                    } else {
                        println("O projeto ${project.display_name} está com a chave 'subprojects' vazia...")
                        println("O arquivo ${yamlFile} está inconsistente, não possui valores para 'subprojects'...")
                    }
                } else {
                    println("O projeto ${project.display_name} não possui subprojetos... Será processado os Jobs do projeto.")
                    if (env.containsKey("jobs")) {
                        jobs = env.jobs
                        //TODO if
                        if (jobs.size() != 0) {
                            for (job in jobs) {
                                //TODO
                                WorkflowJob jobProject = this.configureJob(WorkflowJob, envProject, job)
                            }
                        } else {
                            println("O arquivo ${yamlFile} está inconsistente, não possui valores para 'jobs'... Nenhum job foi criado, somente o Projeto.")
                        }
                    } else {
                        println("O arquivo ${yamlFile} está inconsistente, não possui a chave 'jobs'... Nenhum job foi criado, somente o Projeto.")
                    }
                }
            }
        } else {
            println("O projeto ${project.display_name} está com a chave 'enviroments' vazia...")
            println("O arquivo ${yamlFile} está inconsistente, não possui valores para 'environments'...")
            return
        }
    } catch(FileNotFoundException e) {
        println(e.message)
        println(e.printStackTrace())
    } catch (Exception e){
        println(e.message)
        println(e.printStackTrace())
    }
}

def files = []
new File(yamlDir.toString()).eachFile(FileType.FILES){
    if(it.name.endsWith('.yaml')){
        files << it.absolutePath
    }
}
for(f in files){
    yamlFile = f.toString()
    Main(yamlFile.toString())
    jenkins.save()
}
