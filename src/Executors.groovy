import jenkins.model.Jenkins

Jenkins jenkins = Jenkins.getInstanceOrNull()
jenkins.setNumExecutors(0)
jenkins.save()
