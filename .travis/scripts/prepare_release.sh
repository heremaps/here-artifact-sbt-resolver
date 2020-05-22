#!/bin/bash
set -ev

# Prepare release
git config user.name "Travis CI"
git config user.email "ARTIFACT_SERVICE_SUPPORT@here.com"

PREVIOUS_RELEASE_TAG=$(git describe --abbrev=0)
if [[ ${PREVIOUS_RELEASE_TAG} =~ ^[0-9]+\.[0-9]+\.[0-9]+.*$ ]]; then
  # Set current released version
  sbt -DcurrentVersion=${PREVIOUS_RELEASE_TAG} prepareRelease
else
  echo "Cannot parse the latest release tag: ${PREVIOUS_RELEASE_TAG}. Aborting release."
  exit 1
fi

git remote add origin-travis https://${GITHUB_TOKEN}@${GITHUB_REPO}
git push origin-travis --tags