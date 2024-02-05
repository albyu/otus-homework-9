{{- define "notification.name" -}}
{{- default .Values.notification.name "notification" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "notification.fullname" -}}
{{- $name := default .Values.notification.name "notification" }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

