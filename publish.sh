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

echo "! Before publishing, make sure the version has been already bumped in the 'README.md' and 'CHANGELOG.md' and already merged as release PR into 'main' branch !"
read -p "(Press enter to continue)"
echo ""
echo "! Before publishing, make sure you have created 'bitmovin.properties' file with correct credentials (LastPass: 'bitmovin artifactory') !"
read -p "(Press enter to continue)"
echo ""
echo "! After publishing, change the 'developLocal' gradle property to 'false' manually and start all the examples locally to make sure that the outgoing payload doesn't include '-local' in the version string (and pull the right published artifacts from artifactory) !"
read -p "(Press enter to continue)"
echo ""
echo "! After publishing, Don't forget to update the changelog in readme.io !"
read -p "(Press enter to continue)"
echo ""
echo "Enter the version (without leading 'v')":
read VERSION
echo ""

echo "---------- Summary ----------"
echo 'ANALYTICS_GH_TOKEN='$ANALYTICS_GH_TOKEN
echo 'ANALYTICS_API_RELEASE_TOKEN='$ANALYTICS_API_RELEASE_TOKEN
echo 'VERSION='$VERSION
echo ""
echo "Artifacts to publish:"
echo "  - com.bitmovin.analytics:collector:$VERSION (:collector project)"
echo "  - com.bitmovin.analytics:collector-bitmovin-player:$VERSION (:collector-bitmovin-player project)"
echo "  - com.bitmovin.analytics:collector-exoplayer:$VERSION (:collector-exoplayer project)"
echo "  - com.bitmovin.analytics:collector-media3-exoplayer:$VERSION (:collector-media3-exoplayer project)"
echo "  - com.bitmovin.analytics:collector-amazon-ivs:$VERSION (:collector-amazon-ivs project)"
echo "  - com.bitmovin.analytics:collector-theoplayer:$VERSION (:collector-theoplayer project)"
echo "\nAre all tokens, artifacts and versions correct ?"
read -p "(Press enter to continue)"

echo "\nGit Checkout and pull 'main' branch..."
git checkout main
git pull

echo "\nCheck correct code style..."
if ! ./gradlew spotlessCheck --daemon; then
  echo "Code style violations detected, please fix them first on main as otherwise the build will fail."
  exit
fi

echo "\n:collector project build and publishing..."
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector:clean || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector:build || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector:assembleRelease || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector:artifactoryPublish || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector:publishToMavenLocal || exit
echo "\n:collector project built and published!"

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

echo "\n:collector-amazon-ivs project build and publishing..."
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-amazon-ivs:clean || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-amazon-ivs:build || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-amazon-ivs:assembleRelease || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-amazon-ivs:artifactoryPublish || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-amazon-ivs:publishToMavenLocal || exit
echo "\n:collector-amazon-ivs project built and published!"

echo "\n:collector-media3-exoplayer project build and publishing..."
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-media3-exoplayer:clean || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-media3-exoplayer:build || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-media3-exoplayer:assembleRelease || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-media3-exoplayer:artifactoryPublish || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-media3-exoplayer:publishToMavenLocal || exit
echo "\n:collector-media3-exoplayer project built and published!"

echo "\n:collector-theoplayer project build and publishing..."
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-theoplayer:clean || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-theoplayer:build || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-theoplayer:assembleRelease || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-theoplayer:artifactoryPublish || exit
./gradlew -DdevelopLocal=false -Dversion="$VERSION" :collector-theoplayer:publishToMavenLocal || exit
echo "\n:collector-theoplayer project built and published!"

echo "\nGit release"
echo "\nGit create tag 'v$VERSION' ..."
git tag -a v$VERSION -m "v$VERSION"

echo "\n Git push tag 'v$VERSION' to internal repo."
git push origin main v$VERSION

echo "\n Git push 'main' and tag 'v$VERSION' to public repo."
git push git@github.com:bitmovin/bitmovin-analytics-collector-android.git main v$VERSION

echo "Creating release in public repo."

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

echo "Copying artifacts from libs-release-local to public-releases in jfrog ..."

curl -H "Content-Type: application/json" -X POST -u ${artifactoryUser}:${artifactoryPassword} "https://bitmovin.jfrog.io/bitmovin/api/copy/libs-release-local/com/bitmovin/analytics/collector/${VERSION}?to=/public-releases/com/bitmovin/analytics/collector/${VERSION}"
curl -H "Content-Type: application/json" -X POST -u ${artifactoryUser}:${artifactoryPassword} "https://bitmovin.jfrog.io/bitmovin/api/copy/libs-release-local/com/bitmovin/analytics/collector-bitmovin-player/${VERSION}?to=/public-releases/com/bitmovin/analytics/collector-bitmovin-player/${VERSION}"
curl -H "Content-Type: application/json" -X POST -u ${artifactoryUser}:${artifactoryPassword} "https://bitmovin.jfrog.io/bitmovin/api/copy/libs-release-local/com/bitmovin/analytics/collector-exoplayer/${VERSION}?to=/public-releases/com/bitmovin/analytics/collector-exoplayer/${VERSION}"
curl -H "Content-Type: application/json" -X POST -u ${artifactoryUser}:${artifactoryPassword} "https://bitmovin.jfrog.io/bitmovin/api/copy/libs-release-local/com/bitmovin/analytics/collector-amazon-ivs/${VERSION}?to=/public-releases/com/bitmovin/analytics/collector-amazon-ivs/${VERSION}"
curl -H "Content-Type: application/json" -X POST -u ${artifactoryUser}:${artifactoryPassword} "https://bitmovin.jfrog.io/bitmovin/api/copy/libs-release-local/com/bitmovin/analytics/collector-media3-exoplayer/${VERSION}?to=/public-releases/com/bitmovin/analytics/collector-media3-exoplayer/${VERSION}"
curl -H "Content-Type: application/json" -X POST -u ${artifactoryUser}:${artifactoryPassword} "https://bitmovin.jfrog.io/bitmovin/api/copy/libs-release-local/com/bitmovin/analytics/collector-theoplayer/${VERSION}?to=/public-releases/com/bitmovin/analytics/collector-theoplayer/${VERSION}"

echo "\nCopied artifacts to public jfrog repo."


echo "\nNotifying bitmovin api about new release..."
notifyApi "android-bitmovin" $VERSION "collector-bitmovin-player"
notifyApi "android-exo" $VERSION "collector-exoplayer"
notifyApi "android-amazon-ivs" $VERSION "collector-amazon-ivs"
notifyApi "android-media3-exo" $VERSION "collector-media3-exoplayer"

## TODO: this needs to be added once we have a stable version
##notifyApi "android-theoplayer" $VERSION "collector-theoplayer"

echo "Don't forget to update the changelog in readme.io"
open "https://dash.readme.com/project/bitmovin-playback/v1/docs/analytics-collector-android-releases"
