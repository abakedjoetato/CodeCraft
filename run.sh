#!/bin/bash

# Script to run the Java Discord bot

echo "Starting Java Discord bot..."

# Find the project directory
cd extracted/
PROJECT_DIR=$(find . -type f -name "pom.xml" -o -name "build.gradle" | head -n 1 | xargs dirname)

if [ -z "$PROJECT_DIR" ]; then
    echo "Error: Could not find Maven or Gradle build files. Run setup.sh first."
    exit 1
fi

echo "Found project in: $PROJECT_DIR"
cd "$PROJECT_DIR"

# Check for Discord token in environment
if [ -z "$DISCORD_TOKEN" ]; then
    echo "Warning: DISCORD_TOKEN environment variable not set."
    echo "If the bot requires a token, it may fail to start or you may need to configure it in a properties file."
fi

# Determine if it's a Maven or Gradle project and run accordingly
if [ -f "pom.xml" ]; then
    echo "Running Maven project..."
    
    # Try to find the main class from pom.xml
    MAIN_CLASS=$(grep -o '<mainClass>.*</mainClass>' pom.xml 2>/dev/null | sed 's/<mainClass>//;s/<\/mainClass>//')
    
    if [ -z "$MAIN_CLASS" ]; then
        # Look for jar files in target directory
        JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" -not -name "*-with-dependencies.jar" | sort | head -n 1)
        
        if [ -f "target/*-with-dependencies.jar" ]; then
            JAR_FILE=$(find target -name "*-with-dependencies.jar" | head -n 1)
        fi
        
        if [ -z "$JAR_FILE" ]; then
            echo "Error: Could not find JAR file to run. Make sure the project built correctly."
            exit 1
        fi
        
        echo "Running JAR: $JAR_FILE"
        java -jar "$JAR_FILE"
    else
        echo "Running main class: $MAIN_CLASS"
        mvn exec:java -Dexec.mainClass="$MAIN_CLASS"
    fi
    
elif [ -f "build.gradle" ]; then
    echo "Running Gradle project..."
    
    # Try to find the main class from build.gradle
    MAIN_CLASS=$(grep -o 'mainClassName\s*=\s*['"'"'"][^'"'"'"]*['"'"'"]' build.gradle 2>/dev/null | sed 's/mainClassName\s*=\s*['"'"'"]\(.*\)['"'"'"]/\1/')
    
    if [ -z "$MAIN_CLASS" ]; then
        # Look for jar files in build/libs directory
        JAR_FILE=$(find build/libs -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | sort | head -n 1)
        
        if [ -z "$JAR_FILE" ]; then
            echo "Error: Could not find JAR file to run. Make sure the project built correctly."
            exit 1
        fi
        
        echo "Running JAR: $JAR_FILE"
        java -jar "$JAR_FILE"
    else
        echo "Running main class: $MAIN_CLASS"
        gradle run
    fi
    
else
    echo "Error: The project doesn't appear to be a Maven or Gradle project."
    exit 1
fi
