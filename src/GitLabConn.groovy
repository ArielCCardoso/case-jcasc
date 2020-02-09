import jenkins.model.Jenkins
import com.dabsquared.gitlabjenkins.connection.*

Jenkins jenkins = Jenkins.getInstanceOrNull()

def gitLabConName = System.getenv('GITLAB_CONNECTION_NAME')
def gitLabConId = System.getenv('GITLAB_API_TOKEN_ID')
def gitLabUrl = System.getenv('GITLAB_URL')

GitLabConnection gitlabConnection = new GitLabConnection("${gitLabConName}",
        "${gitLabUrl}",
        "${gitLabConId}",
        true,
        10,
        10)

GitLabConnectionConfig gitlabConfig = (GitLabConnectionConfig) jenkins.getDescriptor(GitLabConnectionConfig.class)

gitlabConfig.getConnections().clear()
gitlabConfig.setUseAuthenticatedEndpoint(false)
gitlabConfig.addConnection(gitlabConnection)
gitlabConfig.save()

jenkins.save()
