#!/bin/sh
# Generate TypeScript Axios client from OpenAPI specification
# Uses OpenAPI Generator CLI v7.10.0

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OPENAPI_SPEC="$SCRIPT_DIR/../src/main/resources/META-INF/openapi.yaml"
OUTPUT_DIR="$SCRIPT_DIR/generated-client"
GENERATOR_JAR="$SCRIPT_DIR/openapi-generator-cli.jar"
GENERATOR_VERSION="7.10.0"
GENERATOR_URL="https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/${GENERATOR_VERSION}/openapi-generator-cli-${GENERATOR_VERSION}.jar"

echo "=== OpenAPI Client Generator ==="
echo "OpenAPI Spec: $OPENAPI_SPEC"
echo "Output Directory: $OUTPUT_DIR"

# Check if OpenAPI spec exists
if [ ! -f "$OPENAPI_SPEC" ]; then
    echo "ERROR: OpenAPI specification not found at $OPENAPI_SPEC"
    exit 1
fi

# Download OpenAPI Generator CLI if not exists
if [ ! -f "$GENERATOR_JAR" ]; then
    echo "Downloading OpenAPI Generator CLI v${GENERATOR_VERSION}..."
    wget -q -O "$GENERATOR_JAR" "$GENERATOR_URL"
    echo "Downloaded successfully"
else
    echo "OpenAPI Generator CLI already exists"
fi

# Clean output directory if exists
if [ -d "$OUTPUT_DIR" ]; then
    echo "Cleaning existing generated client..."
    rm -rf "$OUTPUT_DIR"
fi

# Generate TypeScript Axios client
echo "Generating TypeScript Axios client..."
java -jar "$GENERATOR_JAR" generate \
    -i "$OPENAPI_SPEC" \
    -g typescript-axios \
    -o "$OUTPUT_DIR" \
    --additional-properties=supportsES6=true,supportsAbortController=true,useSingleRequestParameter=false \
    --skip-validate-spec

echo "Client generation completed successfully!"
echo ""
echo "Generated files location: $OUTPUT_DIR"
echo ""
echo "To install dependencies, run:"
echo "  cd $SCRIPT_DIR && npm ci"
echo ""
echo "To run E2E tests, run:"
echo "  cd $SCRIPT_DIR && npm test"
