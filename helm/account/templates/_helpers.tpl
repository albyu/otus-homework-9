{{- define "account.name" -}}
{{- default .Values.account.name "account" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "account.fullname" -}}
{{- $name := default .Values.account.name "account" }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

