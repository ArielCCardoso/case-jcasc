import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import jenkins.plugins.git.GitSCMSource
import jenkins.model.Jenkins

def sharedLibId = System.getenv('SHARED_LIBRARY_ID') ?: 'shared-libraries'
def sharedLibName = System.getenv('SHARED_LIBRARY_NAME') ?: 'Shared_Libraries'
def sharedLibBranch = System.getenv('SHARED_LIBRARY_SCM_BRANCH') ?: 'master'
def sharedLibRemote = System.getenv('SHARED_LIBRARY_SCM_REMOTE') ?: 'git@git.local:devops/shared-libraries.git'

//def sharedLibrary = Jenkins.instance.getDescriptor('org.jenkinsci.plugins.workflow.libs.GlobalLibraries')
def sharedLibrary = Jenkins.instanceOrNull.getDescriptor("org.jenkinsci.plugins.workflow.libs.GlobalLibraries")
//GlobalLibraries sharedLibrary = new GlobalLibraries()
GitSCMSource scmSource = new GitSCMSource(sharedLibRemote.toString())
scmSource.setId(sharedLibId.toString())

SCMSourceRetriever scmSourceRetriever = new SCMSourceRetriever(scmSource)

LibraryConfiguration lib = new LibraryConfiguration(sharedLibName.toString(), scmSourceRetriever)
lib.setDefaultVersion(sharedLibBranch.toString())
lib.setImplicit(true)

sharedLibrary.setLibraries([lib])
sharedLibrary.save()
