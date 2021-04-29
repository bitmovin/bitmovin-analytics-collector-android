#!/bin/sh

setEnvVariable() {
  EXPORT_TOKEN="\n# Created by bitmovin-analytics-collector-android publish script\nexport $1=$2"
  if test -e ~/.bashrc; then
    echo $EXPORT_TOKEN >>~/.bashrc
    echo "Appending to '~/.bashrc' export of ENV variable '$1'"
  fi
  if test -e ~/.bash_profile; then
    echo $EXPORT_TOKEN >>~/.bash_profile
    echo "Appending to '~/.bash_profile' export of ENV variable '$1'"
  fi
}

notifyApi() {
  PLATFORM=$1
  VERSION=$2
  BUNDLE_NAME=$3
  curl "https://api.bitmovin.com/v1/admin/analytics/releases/$PLATFORM/$VERSION" -H "X-Api-Key:$ANALYTICS_API_RELEASE_TOKEN" -H "Content-Type:application/json" \
    --data-binary "{
        \"group\": \"com.bitmovin.analytics\",
        \"repositoryUrl\":\"http://bitmovin.bintray.com/maven\",
        \"name\": \"$BUNDLE_NAME\"
      }" >/dev/null 2>&1

  if [ $? -eq 0 ]; then
    echo "Published Release $PLATFORM:$VERSION:$BUNDLE_NAME to Bitmovin API"
  else
    echo "Failed to publish release to Bitmovin API"
  fi
}

if test -e ~/.bashrc; then
  source ~/.bashrc
fi

if test -e ~/.bash_profile; then
  source ~/.bash_profile
fi

if [ -z "$ANALYTICS_GH_TOKEN" ]; then
  echo "ANALYTICS_GH_TOKEN not found in environment variables. You need to provide the Github Access Token (LastPass: 'bitAnalyticsCircleCi')"
  echo "Enter the token:"
  read ANALYTICS_GH_TOKEN
  echo ""
  setEnvVariable "ANALYTICS_GH_TOKEN" $ANALYTICS_GH_TOKEN
fi

if [ -z "$ANALYTICS_API_RELEASE_TOKEN" ]; then
  echo "ANALYTICS_API_RELEASE_TOKEN not found in environment variables. You need to provide the bitmovin API Key for the user 'dhi+analytics-admin-user', starting with with 'bb2...' (admin.bitmovin.com/users/4c5d7ec9-6fc0-4c75-8534-532b3b2e7426)"
  echo "Enter the token:"
  read ANALYTICS_API_RELEASE_TOKEN
  echo ""
  setEnvVariable "ANALYTICS_API_RELEASE_TOKEN" $ANALYTICS_API_RELEASE_TOKEN
fi

echo "! Before publishing, make sure the version has been already bumped in the 'README.md' and 'CHANGELOG.md' and already merged as release PR into 'develop' branch !"
read -p "(Press enter to continue)"
echo ""
echo "! Before publishing, make sure you have created 'bitmovin.properties' file with correct credentials (LastPass: 'bitmovin artifactory') !"
read -p "(Press enter to continue)"
echo ""
echo "! After publishing, change the 'developLocal' gradle property to 'false' manually and start all the examples locally to make sure that the outgoing payload doesn't include '-local' in the version string (and pull the right published artifacts from artifactory) !"
read -p "(Press enter to continue)"
echo ""
echo "! After publishing, Don't forget to update the changelog in Contentful !"
read -p "(Press enter to continue)"
echo ""
echo "Enter the version (without leading 'v')":
read VERSION
echo ""

if [[ $VERSION =~ ^1. ]]; then
  IS_V1_RELEASE=true
else
  IS_V1_RELEASE=false
fi

echo "---------- Summary ----------"
echo 'ANALYTICS_GH_TOKEN='$ANALYTICS_GH_TOKEN
echo 'ANALYTICS_API_RELEASE_TOKEN='$ANALYTICS_API_RELEASE_TOKEN
echo 'VERSION='$VERSION
echo ""
echo "Artifacts to publish:"
echo "  - com.bitmovin.analytics:collector:$VERSION (:collector project)"
if $IS_V1_RELEASE; then
  echo "  - com.bitmovin.analytics:collector-bitmovin-player:$VERSION (:collector-bitmovin-player-v1 project)"
  echo "  - com.bitmovin.analytics:collector-exoplayer:$VERSION (:collector-exoplayer-v1 project)"
else
  echo "  - com.bitmovin.analytics:collector-bitmovin-player:$VERSION (:collector-bitmovin-player project)"
  echo "  - com.bitmovin.analytics:collector-exoplayer:$VERSION (:collector-exoplayer project)"
fi
echo "\nAre all tokens, artifacts and versions correct ?"
read -p "(Press enter to continue)"

echo "\nGit Checkout and pull 'develop' branch..."
git checkout develop
git pull

echo "\nCheck correct code style..."
if ! ./gradlew spotlessCheck --daemon; then
  echo "Code style violations detected, please fix them first on develop as otherwise the build will fail."
  exit
fi

#TODO next release: check if version parameter works (for CircleCI, we also need to add libs-release-local from the .m2/settings.xml, so it will build the other bitmovin and exo collectors (otherwise it can't resolve the dependency on collector before distributing to bintray)
echo "\n:collector project build and publishing..."
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector:clean || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector:build || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector:assembleRelease || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector:artifactoryPublish || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector:publishToMavenLocal || exit
echo "\n:collector project built and published!"

if $IS_V1_RELEASE; then
  echo "\n:collector-bitmovin-player-v1 project build and publishing..."
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-bitmovin-player-v1:clean || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-bitmovin-player-v1:build || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-bitmovin-player-v1:assembleRelease || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-bitmovin-player-v1:artifactoryPublish || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-bitmovin-player-v1:publishToMavenLocal || exit
  echo "\n:collector-bitmovin-player-v1 project built and published!"

  echo "\n:collector-exoplayer-v1 project build and publishing..."
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-exoplayer-v1:clean || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-exoplayer-v1:build || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-exoplayer-v1:assembleRelease || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-exoplayer-v1:artifactoryPublish || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-exoplayer-v1:publishToMavenLocal || exit
  echo "\n:collector-exoplayer-v1 project built and published!"
else
  echo "\n:collector-bitmovin-player project build and publishing..."
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-bitmovin-player:clean || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-bitmovin-player:build || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-bitmovin-player:assembleRelease || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-bitmovin-player:artifactoryPublish || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-bitmovin-player:publishToMavenLocal || exit
  echo "\n:collector-bitmovin-player project built and published!"

  echo "\n:collector-exoplayer project build and publishing..."
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-exoplayer:clean || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-exoplayer:build || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-exoplayer:assembleRelease || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-exoplayer:artifactoryPublish || exit
  ./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-exoplayer:publishToMavenLocal || exit
  echo "\n:collector-exoplayer project built and published!"
fi

echo "\nGit release"
echo "\nGit Checkout and pull 'main' branch..."
git checkout main
git pull

echo "\nGit merge develop..."
git merge develop

echo "\nGit create tag 'v$VERSION' ..."
git tag -a v$VERSION -m "v$VERSION" #TODO check if tag exists in the beginning. If yes, ask if the user wants to override the version and force overriding of tag

#TODO exit if error
echo "\n Git push 'main' and tag 'v$VERSION' to internal repo."
git push origin main v$VERSION

echo "\n Git push 'main' and tag 'v$VERSION' to public repo."
git push git@github.com:bitmovin/bitmovin-analytics-collector-android.git main v$VERSION

echo "Creating release in public repo."
#TODO override existing release
curl \
  -u bitAnalyticsCircleCi:$ANALYTICS_GH_TOKEN \
  -X POST \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/bitmovin/bitmovin-analytics-collector-android/releases \
  -d "{\"tag_name\":\"v$VERSION\", \"name\": \"v$VERSION\", \"draft\": false}"

file="./bitmovin.properties"

if [ -f "$file" ]; then
  echo "$file found."

  while IFS='=' read -r key value; do
    key=$(echo $key | tr '.' '_')
    eval ${key}=\${value}
  done <"$file"
else
  echo "$file not found."
fi

echo "Distributing the artifacts to bintray..."

curl -H "Content-Type: application/json" -X POST -u ${artifactoryUser}:${artifactoryPassword} "https://bitmovin.jfrog.io/bitmovin/api/copy/libs-release-local/com/bitmovin/analytics/collector/${VERSION}?to=/public-releases/com/bitmovin/analytics/collector/${VERSION}"
curl -H "Content-Type: application/json" -X POST -u ${artifactoryUser}:${artifactoryPassword} "https://bitmovin.jfrog.io/bitmovin/api/copy/libs-release-local/com/bitmovin/analytics/collector-bitmovin-player/${VERSION}?to=/public-releases/com/bitmovin/analytics/collector-bitmovin-player/${VERSION}"
curl -H "Content-Type: application/json" -X POST -u ${artifactoryUser}:${artifactoryPassword} "https://bitmovin.jfrog.io/bitmovin/api/copy/libs-release-local/com/bitmovin/analytics/collector-exoplayer/${VERSION}?to=/public-releases/com/bitmovin/analytics/collector-exoplayer/${VERSION}"

echo "\nDistributed the artifacts to bintray."

notifyApi "android-bitmovin" $VERSION "collector-bitmovin-player"
notifyApi "android-exo" $VERSION "collector-exoplayer"

echo "Don't forget to update the changelog in Contentful."
open "https://app.contentful.com/spaces/blfijbdi3ei3/entries?id=Dg0lZXAPC2p1fVj6&order.fieldId=updatedAt&order.direction=descending&folderId=KJ8HH1mX0jJQ5mHx&searchText=ANDROID&contentTypeId=release&displayedFieldIds=contentType&displayedFieldIds=updatedAt&displayedFieldIds=author&filters.0.key=fields.product&filters.0.val=ANALYTICS"
