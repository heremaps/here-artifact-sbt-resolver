#!/bin/bash
set -ev

export SONATYPE_USERNAME=$OSSRH_USERNAME
export SONATYPE_PASSWORD=$OSSRH_PASSWORD
export PGP_PASSPHRASE=$GPG_PASSPHRASE

# Decrypt gpg key
openssl aes-256-cbc -K $encrypted_81c98acad902_key -iv $encrypted_81c98acad902_iv -in .travis/codesigning.asc.enc -out .travis/codesigning.asc -d

# Import gpg key
gpg --fast-import .travis/codesigning.asc

# Deploy to Maven Central
sbt sonatypeDropAll
sbt publishSigned
sbt sonatypeRelease
