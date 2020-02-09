# Jenkins

Repositório de código do Jenkins.

## Configurações especificas para o Jenkins

- Volume
  - /var/run/docker.sock:/var/run/docker.sock

## Build e Push da imagem

```bash
$docker build --tag arielccardoso/jcasc:2.204.2-alpine .
$docker push arielccardoso/jcasc:2.204.2-alpine
```

### Referências

- https://github.com/jenkinsci/docker/blob/master/README.md