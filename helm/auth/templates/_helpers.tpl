{{- define "auth.name" -}}
{{- default .Values.auth.name "auth" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "auth.fullname" -}}
{{- $name := default .Values.auth.name "auth" }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

