{{- define "labels" }}
chart: {{ .Chart.Name }}
version: {{ .Chart.Version }}
release: {{ .Release.Name }}
heritage: {{ .Release.Service }}
{{- end }}
