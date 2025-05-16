#!/bin/bash

# Script to extract and set up a Java Discord bot

echo "Setting up Java Discord bot..."

# Check if the zip file exists
if [ ! -f "JavaAlpha-main.zip" ]; then
    echo "Error: JavaAlpha-main.zip not found. Please make sure the zip file is in the current directory."
    exit 1
fi

# Create a directory for extraction
mkdir -p extracted

# Extract the zip file
echo "Extracting zip file..."
unzip -o JavaAlpha-main.zip -d extracted/

# Navigate to the extracted directory
cd extracted/

# Find the project directory (it may be nested)
PROJECT_DIR=$(find . -type f -name "pom.xml" -o -name "build.gradle" | head -n 1 | xargs dirname)

if [ -z "$PROJECT_DIR" ]; then
    echo "Error: Could not find Maven or Gradle build files in the extracted content."
    exit 1
fi

echo "Found project in: $PROJECT_DIR"
cd "$PROJECT_DIR"

# Determine if it's a Maven or Gradle project
if [ -f "pom.xml" ]; then
    echo "Maven project detected."
    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        echo "Maven not found. Please install Maven to build this project."
        exit 1
    fi
    
    # Build the project
    echo "Building project with Maven..."
    mvn clean package
    
    # Check if build was successful
    if [ $? -ne 0 ]; then
        echo "Maven build failed. Please check the logs for errors."
        exit 1
    fi
    
    echo "Project built successfully."
    
elif [ -f "build.gradle" ]; then
    echo "Gradle project detected."
    # Check if Gradle is installed
    if ! command -v gradle &> /dev/null; then
        echo "Gradle not found. Please install Gradle to build this project."
        exit 1
    fi
    
    # Build the project
    echo "Building project with Gradle..."
    gradle build
    
    # Check if build was successful
    if [ $? -ne 0 ]; then
        echo "Gradle build failed. Please check the logs for errors."
        exit 1
    fi
    
    echo "Project built successfully."
else
    echo "Error: The project doesn't appear to be a Maven or Gradle project."
    exit 1
fi

# Check for config files that might need setup
if [ -f "config.properties.example" ] || [ -f "config.yml.example" ] || [ -f "application.properties.example" ] || [ -f "application.yml.example" ]; then
    echo "Found example configuration files. You may need to create actual config files from the examples and add your Discord token."
fi

# Return to original directory
cd - > /dev/null

echo "Setup completed. Use run.sh to start the bot."
