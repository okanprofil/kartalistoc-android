#!/bin/sh

##############################################################################
# Gradle wrapper script for UNIX
##############################################################################

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

die() {
    echo "$*"
    exit 1
}

cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
    CYGWIN*) cygwin=true ;;
    Darwin*) darwin=true ;;
    MSYS* | MINGW*) msys=true ;;
    NONSTOP*) nonstop=true ;;
esac

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Determine the Java command to use
if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ]; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

# Resolve links
PRG="$0"
while [ -h "$PRG" ]; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=$(dirname "$PRG")"/$link"
    fi
done

SAVED="$(pwd)"
cd "$(dirname "$PRG")/" >/dev/null || exit
APP_HOME="$(pwd -P)"
cd "$SAVED" >/dev/null || exit

# Download gradle-wrapper.jar if missing
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle Wrapper..."
    mkdir -p "$APP_HOME/gradle/wrapper"
    curl -sL -o "$WRAPPER_JAR" "https://github.com/gradle/gradle/raw/v8.0.0/gradle/wrapper/gradle-wrapper.jar"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS \
    -classpath "$WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain "$@"
