# inception

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 33.2](https://img.shields.io/badge/AppVersion-33.2-informational?style=flat-square)

A semantic annotation platform offering intelligent assistance and knowledge management

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| oci://registry-1.docker.io/bitnamicharts | mariadb(mariadb) | 18.2.6 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| nameOverride | string | `""` |  |
| fullnameOverride | string | `""` |  |
| replicaCount | int | `1` |  |
| podAnnotations | object | `{}` |  |
| podLabels | object | `{}` |  |
| initContainers | object | `{}` |  |
| extraContainers | object | `{}` |  |
| image.repository | string | `"ghcr.io/inception-project/inception"` |  |
| image.pullPolicy | string | `"IfNotPresent"` |  |
| image.tag | string | `"33.2"` |  |
| imagePullSecrets | object | `{}` |  |
| config.baseUrl | string | `"/"` |  |
| config.logging | string | `"INFO"` |  |
| config.security.acceptedOrigins | string | `""` |  |
| config.security.allowHttp | bool | `false` |  |
| config.database.name | string | `"inception"` |  |
| config.database.hostname | string | `""` |  |
| config.database.port | int | `3306` |  |
| config.database.useSSL | bool | `false` |  |
| config.database.serverTimezone | string | `"UTC"` |  |
| config.database.auth.username | string | `"inception"` |  |
| config.database.auth.password.value | string | `"Gre@tPwd465!*"` |  |
| config.database.auth.password.existingSecret.secretName | string | `""` |  |
| config.database.auth.password.existingSecret.secretKey | string | `""` |  |
| config.extraEnv | object | `{}` |  |
| config.extraConfig | object | `{}` |  |
| auth.defaultAdmin.enabled | bool | `true` |  |
| auth.defaultAdmin.username | string | `"admin"` |  |
| auth.defaultAdmin.password.value | string | `"admin123"` |  |
| auth.defaultAdmin.password.existingSecret.secretName | string | `""` |  |
| auth.defaultAdmin.password.existingSecret.secretKey | string | `""` |  |
| auth.oauth2.enabled | bool | `false` |  |
| auth.oauth2.autoLogin | bool | `false` |  |
| auth.oauth2.clientName | string | `""` |  |
| auth.oauth2.clientID | string | `""` |  |
| auth.oauth2.clientSecret.value | string | `""` |  |
| auth.oauth2.clientSecret.existingSecret.secretName | string | `""` |  |
| auth.oauth2.clientSecret.existingSecret.secretKey | string | `""` |  |
| auth.oauth2.scope | string | `""` |  |
| auth.oauth2.authorizationGrantType | string | `""` |  |
| auth.oauth2.redirectURI | string | `""` |  |
| auth.oauth2.issuerURI | string | `""` |  |
| auth.oauth2.userNameAttribute | string | `""` |  |
| auth.saml2.enabled | bool | `false` |  |
| auth.saml2.autoLogin | bool | `false` |  |
| auth.saml2.signRequest.enabled | bool | `false` |  |
| auth.saml2.signRequest.certificateValue | string | `""` |  |
| auth.saml2.signRequest.privateKeyValue | string | `""` |  |
| auth.saml2.signRequest.existingTlsSecret | string | `""` |  |
| auth.saml2.assertingPartyCertificate.value | string | `""` |  |
| auth.saml2.assertingPartyCertificate.existingSecret.secretName | string | `""` |  |
| auth.saml2.assertingPartyCertificate.existingSecret.secretKey | string | `""` |  |
| auth.saml2.assertingPartyEntityID | string | `""` |  |
| auth.saml2.assertingPartySSOUrl | string | `""` |  |
| auth.preAuthentication.enabled | bool | `false` |  |
| auth.preAuthentication.headerPrincipal | string | `""` |  |
| auth.preAuthentication.newUserRole | string | `""` |  |
| auth.preAuthentication.customUserRoles | object | `{}` |  |
| podSecurityContext.runAsUser | int | `2000` |  |
| podSecurityContext.runAsGroup | int | `2000` |  |
| podSecurityContext.fsGroup | int | `2000` |  |
| podSecurityContext.runAsNonRoot | bool | `true` |  |
| securityContext.readOnlyRootFilesystem | bool | `true` |  |
| securityContext.privileged | bool | `false` |  |
| resources | object | `{}` |  |
| livenessProbe.enabled | bool | `true` |  |
| livenessProbe.initialDelaySeconds | int | `0` |  |
| livenessProbe.periodSeconds | int | `5` |  |
| livenessProbe.failureThreshold | int | `2` |  |
| startupProbe.enabled | bool | `true` |  |
| startupProbe.initialDelaySeconds | int | `30` |  |
| startupProbe.periodSeconds | int | `10` |  |
| startupProbe.failureThreshold | int | `5` |  |
| autoscaling.enabled | bool | `false` |  |
| autoscaling.minReplicas | int | `1` |  |
| autoscaling.maxReplicas | int | `100` |  |
| autoscaling.targetCPUUtilizationPercentage | int | `80` |  |
| autoscaling.targetMemoryUtilizationPercentage | int | `80` |  |
| storage | object | `{}` |  |
| nodeSelector | object | `{}` |  |
| tolerations | list | `[]` |  |
| affinity | object | `{}` |  |
| serviceAccount.create | bool | `true` |  |
| serviceAccount.automount | bool | `true` |  |
| serviceAccount.annotations | object | `{}` |  |
| serviceAccount.name | string | `""` |  |
| networkPolicies.enabled | bool | `true` |  |
| service.type | string | `"ClusterIP"` |  |
| service.port | int | `8080` |  |
| ingress.enabled | bool | `true` |  |
| ingress.className | string | `"nginx"` |  |
| ingress.hosts[0].host | string | `"localhost"` |  |
| ingress.hosts[0].paths[0].path | string | `"/"` |  |
| ingress.hosts[0].paths[0].pathType | string | `"Prefix"` |  |
| ingress.tls | list | `[]` |  |
| mariadb.enabled | bool | `true` |  |
| mariadb.image.debug | bool | `true` |  |
| mariadb.image.service.ports.mysql | int | `3306` |  |
| mariadb.networkPolicy.allowExternal | bool | `false` |  |
| mariadb.auth.database | string | `"inception"` |  |
| mariadb.auth.username | string | `"inception"` |  |
| mariadb.auth.password | string | `"Gre@tPwd465!*"` |  |
| mariadb.auth.configuration | string | `"[client]\ndefault-character-set = utf8mb4\n\n[mysql]\ndefault-character-set = utf8mb4\n\n[mysqld]\ncharacter-set-client-handshake = FALSE\ncharacter-set-server = utf8mb4\ncollation-server = utf8mb4_bin"` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.3](https://github.com/norwoodj/helm-docs/releases/v1.11.3)
