# inception

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 33.2](https://img.shields.io/badge/AppVersion-33.2-informational?style=flat-square)

A semantic annotation platform offering intelligent assistance and knowledge management

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| oci://registry-1.docker.io/bitnamicharts | mariadb(mariadb) | 19.0.1 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| nameOverride | string | `""` | Custom Helm Release name.  |
| fullnameOverride | string | `""` | Custom Helm Release full name. |
| replicaCount | int | `1` | Number of replicas fir INCEpTION deployment |
| podAnnotations | object | `{}` |  |
| podLabels | object | `{}` |  |
| initContainers | object | `{}` | User-defined init-container to run before INCEpTION pod, as defined in [Kubernetes documentation](https://kubernetes.io/docs/concepts/workloads/pods/init-containers/) |
| extraContainers | object | `{}` | User-defined extra containers to run alongside INCEpTION pod, as defined in [Kubernetes documentation](https://kubernetes.io/docs/concepts/workloads/pods/#using-pods) |
| extraEnv | object | `{}` | Extra environment variables for INCEpTION's container. |
| extraVolumes | object | `{}` | Extra volume definition for INCEpTION's pod. |
| extraVolumeMounts | object | `{}` | Extra volume mounts for INCEpTION's pod. |
| image | object | `{"pullPolicy":"IfNotPresent","repository":"ghcr.io/inception-project/inception","tag":"33.2"}` | INCEpTION container image settings |
| image.repository | string | `"ghcr.io/inception-project/inception"` | Image repository |
| image.pullPolicy | string | `"IfNotPresent"` | Image Pull Policy |
| image.tag | string | `"33.2"` | Image tag |
| imagePullSecrets | object | `{}` | INCEpTION container image Pull Secret |
| config | object | A pre-configured development deployment (NOT RECOMMENDED FOR PRODUCTION!) | INCEpTION configuration settings |
| config.baseUrl | string | `"/"` | Base URL prefix on which INCEpTION is deployed |
| config.logging | string | `"INFO"` | INCEpTION log level (affects the application and Spring Security framework) |
| config.security | object | `{"acceptedOrigins":"","allowHttp":false}` | INCEpTION security settings |
| config.security.acceptedOrigins | string | `""` | Accepted origin (see [dedicated INCEpTION documentation](https://inception-project.github.io/releases/33.2/docs/admin-guide.html#sect_reverse_proxy)) |
| config.security.allowHttp | bool | `false` | Allowing HTTP unsecure communications |
| config.database | object | `{"auth":{"password":{"existingSecret":{"secretKey":"","secretName":""},"value":""},"username":"inception"},"hostname":"","name":"inception","port":3306,"serverTimezone":"UTC","useSSL":false}` | INCEpTION database connection settings |
| config.database.name | string | `"inception"` | Database name |
| config.database.hostname | string | `""` | Database hostname |
| config.database.port | int | `3306` | Database port |
| config.database.useSSL | bool | `false` | Database SSL communication |
| config.database.auth | object | `{"password":{"existingSecret":{"secretKey":"","secretName":""},"value":""},"username":"inception"}` | Database authentication settings |
| config.database.auth.username | string | `"inception"` | INCEpTION username |
| config.database.auth.password | object | `{"existingSecret":{"secretKey":"","secretName":""},"value":""}` | INCEpTION password |
| config.database.auth.password.value | string | `""` | Password in plain text (NOT RECOMMENDED FOR PRODUCTION!). Ignored if a reference to an external secret is specified. |
| config.database.auth.password.existingSecret | object | `{"secretKey":"","secretName":""}` | Reference to an external Kubernetes secret in the same namespace holding the password's value. |
| config.database.auth.password.existingSecret.secretName | string | `""` | Secret name |
| config.database.auth.password.existingSecret.secretKey | string | `""` | Secret key holding the password's value |
| config.extraConfig | object | `{}` | Extra configuration added at the end of the `settings.properties` file, in `key: value` format. |
| auth | object | `{"defaultAdmin":{"enabled":true,"password":{"existingSecret":{"secretKey":"","secretName":""},"value":"admin123"},"username":"admin"},"oauth2":{"authorizationGrantType":"","autoLogin":false,"clientID":"","clientName":"","clientSecret":{"existingSecret":{"secretKey":"","secretName":""},"value":""},"enabled":false,"issuerURI":"","redirectURI":"","scope":"","usernameAttribute":""},"preAuthentication":{"customUserRoles":{},"enabled":false,"headerPrincipal":"","newUserRole":""},"saml2":{"assertingPartyCertificate":{"existingSecret":{"secretKey":"","secretName":""},"value":""},"assertingPartyEntityID":"","assertingPartySSOUrl":"","autoLogin":false,"enabled":false,"signRequest":{"certificateValue":"","enabled":false,"existingTlsSecret":"","privateKeyValue":""}}}` | INCEpTION's authentication configuration |
| auth.defaultAdmin | object | `{"enabled":true,"password":{"existingSecret":{"secretKey":"","secretName":""},"value":"admin123"},"username":"admin"}` | Default Administrator account configuration (see [dedicated INCEpTION documentation](https://inception-project.github.io/releases/33.2/docs/admin-guide.html#_unsupervised_installation)) |
| auth.defaultAdmin.enabled | bool | `true` | Enabling Default Administrator feature  |
| auth.defaultAdmin.username | string | `"admin"` | Default Administrator username |
| auth.defaultAdmin.password | object | `{"existingSecret":{"secretKey":"","secretName":""},"value":"admin123"}` | Default Administrator password |
| auth.defaultAdmin.password.value | string | `"admin123"` | Password in plain text (NOT RECOMMENDED FOR PRODUCTION!). `bcrypt` hash will be derived from that password and stored in the `settings.properties` file.    Ignored if a reference to an external secret is specified.  |
| auth.defaultAdmin.password.existingSecret | object | `{"secretKey":"","secretName":""}` | Reference to an external Kubernetes secret in the same namespace holding the password's bcrypt hash's value, in the `{bcrypt}XXXXXX` format. |
| auth.defaultAdmin.password.existingSecret.secretName | string | `""` | Secret name. |
| auth.defaultAdmin.password.existingSecret.secretKey | string | `""` | Secret key holding the password's `bcrypt` hash. |
| auth.oauth2 | object | `{"authorizationGrantType":"","autoLogin":false,"clientID":"","clientName":"","clientSecret":{"existingSecret":{"secretKey":"","secretName":""},"value":""},"enabled":false,"issuerURI":"","redirectURI":"","scope":"","usernameAttribute":""}` | OAuth2 authentication settings (see [dedicated INCEpTION documentation](https://inception-project.github.io/releases/33.2/docs/admin-guide.html#sect_security_authentication_oauth2)) |
| auth.oauth2.enabled | bool | `false` | Enabling OAuth2 SSO login |
| auth.oauth2.autoLogin | bool | `false` | Enabling OAuth2 as default login method (skips INCEpTION's default login page) |
| auth.oauth2.clientName | string | `""` | OAuth2 client name, to be displayed on INCEpTION's login page. |
| auth.oauth2.clientID | string | `""` | OAUth2 client ID |
| auth.oauth2.clientSecret | object | `{"existingSecret":{"secretKey":"","secretName":""},"value":""}` | OAuth2 client secret |
| auth.oauth2.clientSecret.value | string | `""` | OAuth2 client secret in plain text (NOT RECOMMENDED FOR PRODUCTION!). Ignored if a reference to an external secret is specified. |
| auth.oauth2.clientSecret.existingSecret | object | `{"secretKey":"","secretName":""}` | Reference to an external Kubernetes secret in the same namespace holding the OAuth2 secret's value. |
| auth.oauth2.clientSecret.existingSecret.secretName | string | `""` | Secret name |
| auth.oauth2.clientSecret.existingSecret.secretKey | string | `""` | Secret key holding the OAuth2 client secret's value.  |
| auth.oauth2.scope | string | `""` | OAuth2 scope |
| auth.oauth2.authorizationGrantType | string | `""` | OAuth2 grant type |
| auth.oauth2.redirectURI | string | `""` | OAuth2 redirect URI |
| auth.oauth2.issuerURI | string | `""` | OAuth2 Issuer URI |
| auth.oauth2.usernameAttribute | string | `""` | OAuth2 claim holding the username attribute. |
| auth.saml2 | object | `{"assertingPartyCertificate":{"existingSecret":{"secretKey":"","secretName":""},"value":""},"assertingPartyEntityID":"","assertingPartySSOUrl":"","autoLogin":false,"enabled":false,"signRequest":{"certificateValue":"","enabled":false,"existingTlsSecret":"","privateKeyValue":""}}` | SAML2 Authentication settings (see [dedicated INCEpTION documentation](https://inception-project.github.io/releases/33.2/docs/admin-guide.html#sect_security_authentication_saml2)). |
| auth.saml2.enabled | bool | `false` | Enabling SAML2 SSO login. |
| auth.saml2.autoLogin | bool | `false` | Enabling SAML2 as default login method (skips INCEpTION's default login page). |
| auth.saml2.signRequest | object | `{"certificateValue":"","enabled":false,"existingTlsSecret":"","privateKeyValue":""}` | SAML2 requests signing with external key and certificate. |
| auth.saml2.signRequest.enabled | bool | `false` | Enabling SAML2 request signing. |
| auth.saml2.signRequest.certificateValue | string | `""` | Public certificate value un plain text. Ignored if a reference to an external TLS Secret is specified. |
| auth.saml2.signRequest.privateKeyValue | string | `""` | Private Key value, in plain text (NOT RECOMMENDED FOR PRODUCTION!). Ignored if a reference to an external secret is specified.  |
| auth.saml2.signRequest.existingTlsSecret | string | `""` | Reference to an external Kubernetes TLS secret in the same namespace holding the certificate/private key pair to sign the SAML2 requests. |
| auth.saml2.assertingPartyCertificate | object | `{"existingSecret":{"secretKey":"","secretName":""},"value":""}` | Certificate authenticating the Identity Provider in SAML2 connection. |
| auth.saml2.assertingPartyCertificate.value | string | `""` | Certificate in plain text. Ignored if a reference to an external Secret is specified. |
| auth.saml2.assertingPartyCertificate.existingSecret | object | `{"secretKey":"","secretName":""}` | Reference to an external Kubernetes Secret in the same namespace holding the Identity Provider certificate. |
| auth.saml2.assertingPartyCertificate.existingSecret.secretName | string | `""` | Name of the Secret. |
| auth.saml2.assertingPartyCertificate.existingSecret.secretKey | string | `""` | Secret key holding the Identity Provider certificate. |
| auth.saml2.assertingPartyEntityID | string | `""` | Identity Provider's SAML2 Asserting ID |
| auth.saml2.assertingPartySSOUrl | string | `""` | Identity Provider's SAML2 SSO URL |
| auth.preAuthentication | object | `{"customUserRoles":{},"enabled":false,"headerPrincipal":"","newUserRole":""}` | PreAuthentication settings, not cumulative with OAuth2 and SAML2 authentication modes (see [dedicated INCEpTION documentation](https://inception-project.github.io/releases/33.2/docs/admin-guide.html#sect_security_authentication_preauth)). |
| auth.preAuthentication.enabled | bool | `false` | Enabling the PreAuthentication feature. |
| auth.preAuthentication.headerPrincipal | string | `""` | Name of the header holding the remote user name. |
| auth.preAuthentication.newUserRole | string | `""` | Role given to new pre-authenticated users. |
| auth.preAuthentication.customUserRoles | object | `{}` | Roles to map to dedicated pre-authenticated users, in the `username: role` format. |
| livenessProbe | object | `{"enabled":true,"failureThreshold":2,"initialDelaySeconds":0,"periodSeconds":5}` | INCEpTION's pod liveness probe. |
| livenessProbe.enabled | bool | `true` | Enabling the liveness probe. |
| livenessProbe.initialDelaySeconds | int | `0` | Liveness probe initial startup delay. |
| livenessProbe.periodSeconds | int | `5` | Liveness probe check interval, in seconds. |
| livenessProbe.failureThreshold | int | `2` | Liveness probe's number of accepted failures. |
| startupProbe | object | `{"enabled":true,"failureThreshold":5,"initialDelaySeconds":30,"periodSeconds":10}` | INCEpTION's pod startup probe. |
| startupProbe.enabled | bool | `true` | Enabling the startup probe. |
| startupProbe.initialDelaySeconds | int | `30` | Startup probe initial startup delay. |
| startupProbe.periodSeconds | int | `10` | Startup probe check interval, in seconds. |
| startupProbe.failureThreshold | int | `5` | Startup probe's number of accepted failures. |
| podSecurityContext | object | `{"fsGroup":2000,"runAsGroup":2000,"runAsNonRoot":true,"runAsUser":2000}` | INCEpTION podSecurityContext. |
| podSecurityContext.runAsUser | int | `2000` | User ID to run as. |
| podSecurityContext.runAsGroup | int | `2000` | Group ID to run as. |
| podSecurityContext.fsGroup | int | `2000` | Group ID authorized to mount external PVCs. |
| podSecurityContext.runAsNonRoot | bool | `true` | Allowing container(s) to run as root. |
| securityContext | object | `{"privileged":false,"readOnlyRootFilesystem":true}` | INCEpTION's container securityContext. |
| securityContext.readOnlyRootFilesystem | bool | `true` | Enforcing read-only root filesystem (`/`). |
| securityContext.privileged | bool | `false` | Enabling running the pod as privileged. |
| resources | object | `{}` | INCEpTION pod's resources |
| autoscaling | object | `{"enabled":false,"maxReplicas":100,"minReplicas":1,"targetCPUUtilizationPercentage":80,"targetMemoryUtilizationPercentage":80}` | INCEpTION pod horizontal autoscaling feature (not recommended) |
| autoscaling.enabled | bool | `false` | Enabling horizontal pod autoscaling. |
| autoscaling.minReplicas | int | `1` | Minimum number of pod replicas. |
| autoscaling.maxReplicas | int | `100` | Maximum number of pod replicas. |
| autoscaling.targetCPUUtilizationPercentage | int | `80` | Target CPU usage percentage to trigger the deployment of a new replica. |
| autoscaling.targetMemoryUtilizationPercentage | int | `80` | Target memory usage percentage to trigger the deployment of a new replica. |
| serviceAccount.create | bool | `true` | Specifies whether a service account should be created |
| serviceAccount.automount | bool | `true` | Automatically mount a ServiceAccount's API credentials? |
| serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| serviceAccount.name | string | `""` | The name of the service account to use.    If not set and create is true, a name is generated using the fullname template |
| nodeSelector | object | `{}` | INCEpTION pod node selector. |
| tolerations | list | `[]` | INCEpTION pod tolerations. |
| affinity | object | `{}` | INCEpTION pod affinities. |
| persistence | object | `{"data":{"accessMode":"ReadWriteOnce","size":"5Gi","storageClassName":"standard"}}` | INCEpTION data persistence settings. |
| persistence.data.storageClassName | string | `"standard"` | Name of the Storage Class to use. |
| persistence.data.size | string | `"5Gi"` | Requested volume size.  |
| persistence.data.accessMode | string | `"ReadWriteOnce"` | Persistent Volume Claim Access Mode |
| networkPolicies | object | `{"enabled":true}` | Network Policies settings. |
| networkPolicies.enabled | bool | `true` | Enabling the default Network Policies. |
| service | object | `{"port":8080,"type":"ClusterIP"}` | INCEpTION service exposition settings |
| service.type | string | `"ClusterIP"` | Service Type  |
| service.port | int | `8080` | Service Port |
| ingress | object | `{"className":"nginx","enabled":true,"hosts":[{"host":"localhost","paths":[{"path":"/","pathType":"Prefix"}]}],"tls":[]}` | Ingress configuration |
| ingress.enabled | bool | `true` | Enabling Ingress |
| ingress.className | string | `"nginx"` | Ingress class name |
| ingress.hosts | list | `[{"host":"localhost","paths":[{"path":"/","pathType":"Prefix"}]}]` | Ingress host settings |
| ingress.tls | list | `[]` | Ingress TLS settings |
| mariadb | object | `{"auth":{"database":"inception","existingSecret":"","password":"Gre@tPwd465!*","username":"inception"},"enabled":true,"image":{"debug":false,"service":{"ports":{"mysql":3306}}},"networkPolicy":{"allowExternal":false,"enabled":true},"primary":{"configuration":"[client]\ndefault-character-set = utf8mb4\n\n[mysql]\ndefault-character-set = utf8mb4\n\n[mysqld]\nmax_allowed_packet=500M\ncharacter-set-client-handshake = FALSE\ncharacter-set-server = utf8mb4\ncollation-server = utf8mb4_bin"}}` | Embedded Bitnami MariaDB Chart settings (see [dedicated Chart documentation](https://artifacthub.io/packages/helm/bitnami/mariadb))  |
| mariadb.enabled | bool | `true` | Enabling embedded MariaDB. |
| mariadb.image | object | `{"debug":false,"service":{"ports":{"mysql":3306}}}` | MariaDB container image settings. |
| mariadb.image.debug | bool | `false` | Enabling debug logging. |
| mariadb.image.service | object | `{"ports":{"mysql":3306}}` | MariaDB service exposition settings. |
| mariadb.image.service.ports | object | `{"mysql":3306}` | Service ports settings |
| mariadb.image.service.ports.mysql | int | `3306` | Mariadb `mysql`port |
| mariadb.primary | object | `{"configuration":"[client]\ndefault-character-set = utf8mb4\n\n[mysql]\ndefault-character-set = utf8mb4\n\n[mysqld]\nmax_allowed_packet=500M\ncharacter-set-client-handshake = FALSE\ncharacter-set-server = utf8mb4\ncollation-server = utf8mb4_bin"}` | MariaDB primary server settingd |
| mariadb.primary.configuration | string | `"[client]\ndefault-character-set = utf8mb4\n\n[mysql]\ndefault-character-set = utf8mb4\n\n[mysqld]\nmax_allowed_packet=500M\ncharacter-set-client-handshake = FALSE\ncharacter-set-server = utf8mb4\ncollation-server = utf8mb4_bin"` | MariaDB Primary configuration to be injected as ConfigMap |
| mariadb.auth | object | `{"database":"inception","existingSecret":"","password":"Gre@tPwd465!*","username":"inception"}` | MariaDB authentication settings for the custom database |
| mariadb.auth.database | string | `"inception"` | Name for a custom database to create |
| mariadb.auth.username | string | `"inception"` | Name for a custom user to create |
| mariadb.auth.password | string | `"Gre@tPwd465!*"` | Password for the new user, in plain text (NOT RECOMMENDED FOR PRODUCTION!). Ignored if existing secret is provided. |
| mariadb.auth.existingSecret | string | `""` | Use existing secret for password details (auth.rootPassword, auth.password, auth.replicationPassword will be ignored and picked up from this secret).     The secret has to contain the keys `mariadb-root-password`, `mariadb-replication-password` and `mariadb-password`. |
| mariadb.networkPolicy | object | `{"allowExternal":false,"enabled":true}` | Network Policies settings. |
| mariadb.networkPolicy.enabled | bool | `true` | Enable creation of NetworkPolicy resources. |
| mariadb.networkPolicy.allowExternal | bool | `false` | The Policy model to apply. |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.14.2](https://github.com/norwoodj/helm-docs/releases/v1.14.2)
