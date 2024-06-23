{{/*
Expand the name of the chart.
*/}}
{{- define "inception.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "inception.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "inception.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "inception.labels" -}}
helm.sh/chart: {{ include "inception.chart" . }}
{{ include "inception.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "inception.selectorLabels" -}}
app.kubernetes.io/name: {{ include "inception.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "inception.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "inception.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Database configuration
*/}}
{{- define "inception.deployment.database.hostname" -}}
{{- include "inception.fullname" . }}-mariadb
{{- end }}

{{- define "inception.secret.databaseAuth.name" -}}
{{- include "inception.fullname" .}}-database-auth
{{- end }}

{{/*
Application configuration
*/}}
{{- define "inception.configmap.appConf.name" -}}
{{- include "inception.fullname" .}}-application-configuration
{{- end }}

{{/*
Application persistence
*/}}
{{- define "inception.persistence.pvc.name" -}}
data-{{ include "inception.fullname" . }}
{{- end }}