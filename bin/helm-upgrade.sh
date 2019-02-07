#!/usr/bin/env bash

#
# Run `helm upgrade` with specified options
#

set -euo pipefail

echo "helm-upgrade.sh"

working_directory="$( pwd )"

script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

while [ $# -gt 0 ]; do
  case "$1" in
    --chart-path=*)
      CHART_PATH="${1#*=}"
      ;;
    --values-template=*)
      VALUES_TEMPLATE="${1#*=}"
      ;;
    --namespace=*)
      NAMESPACE="${1#*=}"
      ;;
    --release-name=*)
      RELEASE_NAME="${1#*=}"
      ;;
    *)
      printf "Warning: unknown argument ${1}\n"
  esac
  shift
done

echo "CHART_PATH: ${CHART_PATH}"
echo "VALUES_TEMPLATE: ${VALUES_TEMPLATE}"
echo "NAMESPACE: ${NAMESPACE}"
echo "RELEASE_NAME: ${RELEASE_NAME}"

if [ -z "$CHART_PATH" ] || [ -z "$NAMESPACE" ] || [ -z "$RELEASE_NAME" ]; then
  echo "Error: missing required arguments! See output above."
  exit 1
fi

application_path=$( if [[ $BRANCH == "master" ]]; then echo "/documentation-searcher"; else echo "/documentation-searcher-${BRANCH}"; fi; )
# NOTE: "${application_path}-ext" ingress will be created with basic http auth
application_url="https://docs.dev.opuscapita.com${application_path}"

ext_login=$( cat "$script_dir"/../helm/ext-credentials/login )
ext_password=$( cat "$script_dir"/../helm/ext-credentials/password )

static_page_enabled=$( if [[ $BRANCH == "master" ]]; then echo true; else echo false; fi; )
static_page_html_content=$( cat "$script_dir"/../helm/docs.dev.opuscapita.com.html | sed s/EXT_LOGIN/"$ext_login"/g | sed s/EXT_PASSWORD/"$ext_password"/g | base64 )

function escape_commas() {
  read data;
  echo "$data" | sed -e 's/\,/\\,/g'
}

smtp_password=$( echo "$DOCUMENTATION_SEARCHER_SMTP_PASS" | escape_commas )
smtp_properties=$( echo "$DOCUMENTATION_SEARCHER_SMTP_PROPERTIES" | escape_commas )
mail_from=$( echo "$DOCUMENTATION_SEARCHER_MAIL_FROM" | escape_commas )
mail_to=$( echo "$DOCUMENTATION_SEARCHER_MAIL_TO" | escape_commas )
mail_cc=$( echo "$DOCUMENTATION_SEARCHER_MAIL_CC" | escape_commas )
mail_report_to=$( echo "$DOCUMENTATION_SEARCHER_MAIL_REPORT_TO" | escape_commas )

apt-get -qq update
apt-get -qq install apache2-utils -y
htpasswd=$( htpasswd -nb "$ext_login" "$ext_password" )

# FIXME before merging: use azure disk in deployment
# !-- Avoid azureDisk reattachement to another node
AZURE_DISK_NODE_NAME=`kubectl get pod --all-namespaces -l app=documentation-searcher -o json | jq .items[0].spec.nodeName`
case "${AZURE_DISK_NODE_NAME}" in
 null) AZURE_DISK_NODE_NAME="" ;;
esac
echo "azureDisk node hostname: ${AZURE_DISK_NODE_NAME}"
# Avoid azureDisk reattachement to another node ---!

# configure $HELM_HOME
helm init --client-only

# put Helm dependencies into correct folder
cd $CHART_PATH
helm dependency update

# install/update release
helm upgrade \
  --install \
  --force \
  --set mountAzureDisk=$( if [[ $BRANCH == "master" ]]; then echo true; else echo false; fi; ) \
  --set image.repository="${DOCKER_IMAGE_REPOSITORY}" \
  --set image.tag="${DOCKER_IMAGE_TAG}" \
  --set ingress.path="$application_path" \
  --set ingress.htpasswd="$htpasswd" \
  --set azureDisk.uri="${DOCUMENTATION_SEARCHER_AZURE_DISK_URI}" \
  --set azureDisk.nodeName="${AZURE_DISK_NODE_NAME}" \
  --set share.user="${DOCUMENTATION_SEARCHER_SHARE_USER}" \
  --set share.password="${DOCUMENTATION_SEARCHER_SHARE_PASS}" \
  --set smtp.host="${DOCUMENTATION_SEARCHER_SMTP_HOST}" \
  --set smtp.port="${DOCUMENTATION_SEARCHER_SMTP_PORT}" \
  --set smtp.user="${DOCUMENTATION_SEARCHER_SMTP_USER}" \
  --set smtp.password="$smtp_password" \
  --set smtp.properties="$smtp_properties" \
  --set mail.from="$mail_from" \
  --set mail.to="$mail_to" \
  --set mail.cc="$mail_cc" \
  --set mail.reportTo="$mail_report_to" \
  --set vcs.ref="${COMMIT}" \
  --set staticPage.enabled="$static_page_enabled" \
  --set staticPage.base64HtmlContent="$static_page_html_content" \
  --set github-status-deployment-link.github.user="${GH_NAME}" \
  --set github-status-deployment-link.github.password="${GH_PASS}" \
  --set github-status-deployment-link.github.project="${GITHUB_PROJECT}" \
  --set github-status-deployment-link.github.ref="${COMMIT}" \
  --set github-status-deployment-link.url="${application_url}" \
  --set selfkiller.azureAks.resourceGroup="${MINSK_CORE_K8S_AZURE_RG}" \
  --set selfkiller.azureAks.clusterName="${MINSK_CORE_K8S_AZURE_NAME}" \
  --set selfkiller.image.repository="${DOCKER_IMAGE_REPOSITORY}" \
  --set selfkiller.image.tag="${DOCKER_IMAGE_TAG}" \
  --set selfkiller.github.project="${GITHUB_PROJECT}" \
  --set selfkiller.github.branch="${BRANCH}" \
  --set slack-notifications.webhook="${MINSK_CORE_SLACK_CI_WEBHOOK_URL}" \
  --set slack-notifications.github.user="${CIRCLE_USERNAME}" \
  --set slack-notifications.github.project="${GITHUB_PROJECT}" \
  --set slack-notifications.github.branch="${BRANCH}" \
  --set slack-notifications.github.ref="${COMMIT}" \
  --set slack-notifications.ci.jobUrl="${CIRCLE_BUILD_URL}" \
  --set slack-notifications.link.url="${application_url}" \
  --set dockerSecret="${IMAGE_PULL_SECRET_NAME}" \
  --set sync-secrets.secrets="{docs.dev.opuscapita.com-tls,dockerhub,machineuser-vault-master-secret}" \
  --namespace $NAMESPACE \
  $RELEASE_NAME \
  .

# restore working directory to initial pwd
cd $working_directory
