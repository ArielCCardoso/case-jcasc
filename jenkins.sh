#! /bin/bash -e

### Permissions
# 700 ~/.ssh
# 400 ~/.ssh/id_rsa
# 400 ~/.ssh/id_rsa.pub
# 600 ~/.ssh/config

sshDir=~/.ssh
sshPrivateKey=$sshDir/id_rsa
sshPublicKey=$sshDir/id_rsa.pub
sshConfig=$sshDir/config
gitConfig=~/.gitconfig

secretDir=/var/run/secrets
secretKey=${secretDir}/.ssh/id_rsa
secretKeyPub=${secretDir}/.ssh/id_rsa.pub
secretSshConfig=${secretDir}/.ssh/config
secretGitConfig=${secretDir}/.git/.gitconfig

if [ -d ${sshDir} ]; then
    chmod 700 ${sshDir}
    if [ -f ${sshPrivateKey} ]; then
        chmod 400 ${sshPrivateKey}
    elif [ -f ${secretKey} ]; then
        cp ${secretKey} ${sshPrivateKey} && chmod 400 ${sshPrivateKey}
    elif [ -n "${SSH_PRIVATE_KEY}" ] && [ "${SSH_PRIVATE_KEY}" != "" ]; then
        echo "${SSH_PRIVATE_KEY}" > ${sshPrivateKey} && chmod 400 ${sshPrivateKey}
    fi
    if [ -f ${sshPublicKey} ]; then
        chmod 400 ${sshPublicKey}
    elif [ -f ${secretKeyPub} ]; then
        cp ${secretKeyPub} ${sshPublicKey} && chmod 400 ${sshPublicKey}
    elif [ -n "${SSH_PUBLIC_KEY}" ] && [ "${SSH_PUBLIC_KEY}" != "" ]; then
        echo "${SSH_PUBLIC_KEY}" > ${sshPublicKey} && chmod 400 ${sshPublicKey}
    fi
    if [ -f ${sshConfig} ]; then
        chmod 600 ${sshConfig}
    elif [ -f ${secretSshConfig} ]; then
        cp ${secretSshConfig} ${sshConfig} && chmod 600 ${sshConfig}
    elif [ -n "${SSH_CONFIG}" ] && [ "${SSH_CONFIG}" != "" ]; then
        echo "${SSH_CONFIG}" > ${sshConfig} && chmod 600 ${sshConfig}
    fi
else
    mkdir ${sshDir} && chmod 700 ${sshDir}
    if [ -f ${sshPrivateKey} ]; then
        chmod 400 ${sshPrivateKey}
    elif [ -f ${secretKey} ]; then
        cp ${secretKey} ${sshPrivateKey} && chmod 400 ${sshPrivateKey}
    elif [ -n "${SSH_PRIVATE_KEY}" ] && [ "${SSH_PRIVATE_KEY}" != "" ]; then
        echo "${SSH_PRIVATE_KEY}" > ${sshPrivateKey} && chmod 400 ${sshPrivateKey}
    fi
    if [ -f ${sshPublicKey} ]; then
        chmod 400 ${sshPublicKey}
    elif [ -f ${secretKeyPub} ]; then
        cp ${secretKeyPub} ${sshPublicKey} && chmod 400 ${sshPublicKey}
    elif [ -n "${SSH_PUBLIC_KEY}" ] && [ "${SSH_PUBLIC_KEY}" != "" ]; then
        echo "${SSH_PUBLIC_KEY}" > ${sshPublicKey} && chmod 400 ${sshPublicKey}
    fi
    if [ -f ${sshConfig} ]; then
        chmod 600 ${sshConfig}
    elif [ -f ${secretSshConfig} ]; then
        cp ${secretSshConfig} ${sshConfig} && chmod 600 ${sshConfig}
    elif [ -n "${SSH_CONFIG}" ] && [ "${SSH_CONFIG}" != "" ]; then
        echo "${SSH_CONFIG}" > ${sshConfig} && chmod 600 ${sshConfig}
    fi
fi
if [ -f ${gitConfig} ]; then
    chmod 600 ${gitConfig}
elif [ -f ${secretGitConfig} ]; then
    cp ${secretGitConfig} ${gitConfig} && chmod 600 ${gitConfig}
elif [ -n "${GIT_CONFIG}" ] && [ "${GIT_CONFIG}" != "" ]; then
    echo "${GIT_CONFIG}" > ${gitConfig} && chmod 600 ${gitConfig}
fi
if [ -f ${sshPrivateKey} ]; then
    eval $(ssh-agent -s)  &&  ssh-add ${sshPrivateKey}
fi

: "${JENKINS_WAR:="/usr/share/jenkins/jenkins.war"}"
: "${JENKINS_HOME:="/var/jenkins_home"}"
touch "${COPY_REFERENCE_FILE_LOG}" || { echo "Can not write to ${COPY_REFERENCE_FILE_LOG}. Wrong volume permissions?"; exit 1; }
echo "--- Copying files at $(date)" >> "$COPY_REFERENCE_FILE_LOG"
find /usr/share/jenkins/ref/ \( -type f -o -type l \) -exec bash -c '. /usr/local/bin/jenkins-support; for arg; do copy_reference_file "$arg"; done' _ {} +

# if `docker run` first argument start with `--` the user is passing jenkins launcher arguments
if [[ $# -lt 1 ]] || [[ "$1" == "--"* ]]; then

  # read JAVA_OPTS and JENKINS_OPTS into arrays to avoid need for eval (and associated vulnerabilities)
  java_opts_array=()
  while IFS= read -r -d '' item; do
    java_opts_array+=( "$item" )
  done < <([[ $JAVA_OPTS ]] && xargs printf '%s\0' <<<"$JAVA_OPTS")

  readonly agent_port_property='jenkins.model.Jenkins.slaveAgentPort'
  if [ -n "${JENKINS_SLAVE_AGENT_PORT:-}" ] && [[ "${JAVA_OPTS:-}" != *"${agent_port_property}"* ]]; then
    java_opts_array+=( "-D${agent_port_property}=${JENKINS_SLAVE_AGENT_PORT}" )
  fi

  if [[ "$DEBUG" ]] ; then
    java_opts_array+=( \
      '-Xdebug' \
      '-Xrunjdwp:server=y,transport=dt_socket,address=5005,suspend=y' \
    )
  fi

  jenkins_opts_array=( )
  while IFS= read -r -d '' item; do
    jenkins_opts_array+=( "$item" )
  done < <([[ $JENKINS_OPTS ]] && xargs printf '%s\0' <<<"$JENKINS_OPTS")

  exec java -Duser.home="$JENKINS_HOME" "${java_opts_array[@]}" -jar ${JENKINS_WAR} "${jenkins_opts_array[@]}" "$@"
fi

# As argument is not jenkins, assume user want to run his own process, for example a `bash` shell to explore this image
exec "$@"