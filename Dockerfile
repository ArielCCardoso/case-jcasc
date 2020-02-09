FROM jenkins/jenkins:2.204.2-alpine

LABEL BaseImage="jenkins/jenkins:2.204.2-alpine "               \
      Author="Ariel C. Cardoso <arielccardoso@live.com>"        \
      ConfigurationType="JCasC <Jenkins Configuration as Code>"

USER jenkins

COPY --chown=root:root jenkins.sh /usr/local/bin/jenkins.sh
COPY zInit.groovy /usr/share/jenkins/ref/init.groovy.d/zInit.groovy
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt

ENV JAVA_OPTS -Dhudson.footerURL=https://jenkins.arielcardoso.net
ENV TZ=America/Sao_Paulo
ENV JENKINS_VERSION=2.204.2

RUN echo 2.204 > /usr/share/jenkins/ref/jenkins.install.UpgradeWizard.state
RUN echo 2.204 > /usr/share/jenkins/ref/jenkins.install.InstallUtil.lastExecVersion
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt