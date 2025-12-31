# OpenAI API Mock Server Validation

This document explains how to validate that our WireMock-based OpenAI mock server correctly implements the OpenAI API specification.

## WireMock Implementation

Our `OpenAiMockServerResource` implements the OpenAI Chat Completions API v1 specification:

### API Endpoint
- `POST /chat/completions`

### Request Requirements
- Header: `Authorization: Bearer {api_key}`
- Header: `Content-Type: application/json`
- Body must contain `model` and `messages` fields

### Response Format
```json
{
  "id": "chatcmpl-{id}",
  "object": "chat.completion",
  "created": {unix_timestamp},
  "model": "{model_name}",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "{response_text}"
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": {int},
    "completion_tokens": {int},
    "total_tokens": {int}
  }
}
```

## Validation Tests

### 1. Automated Validation (Always Runs)

```bash
# Run the validation test
./gradlew test --tests "OpenAiApiValidationTest.testWithWireMock"
```

This test verifies:
- WireMock server starts correctly
- Endpoint accepts requests with proper authentication
- Response matches OpenAI API specification
- Application can parse and use the response

### 2. Optional Real API Validation

You can validate against a real OpenAI-compatible API by setting environment variables:

#### Testing with OpenAI

```bash
export TEST_OPENAI_BASE_URL=https://api.openai.com/v1
export TEST_OPENAI_API_KEY=sk-your-api-key-here
export TEST_OPENAI_MODEL=gpt-3.5-turbo

./gradlew test --tests "OpenAiApiValidationTest.testWithRealOpenAiApi"
```

#### Testing with Azure OpenAI

```bash
export TEST_OPENAI_BASE_URL=https://your-resource.openai.azure.com/openai/deployments/your-deployment
export TEST_OPENAI_API_KEY=your-azure-key
export TEST_OPENAI_MODEL=gpt-35-turbo

./gradlew test --tests "OpenAiApiValidationTest.testWithRealOpenAiApi"
```

#### Testing with LocalAI (Self-hosted)

```bash
# Start LocalAI
docker run -p 8080:8080 localai/localai:latest

export TEST_OPENAI_BASE_URL=http://localhost:8080/v1
export TEST_OPENAI_API_KEY=not-needed  # LocalAI doesn't require auth
export TEST_OPENAI_MODEL=gpt-3.5-turbo

./gradlew test --tests "OpenAiApiValidationTest.testWithRealOpenAiApi"
```

## Manual Testing in Dev Mode

To test the actual application with a real OpenAI API:

```bash
# Set credentials
export AZURE_OPENAI_API_KEY=your-api-key
export AZURE_OPENAI_ENDPOINT=https://api.openai.com/v1
export AZURE_OPENAI_DEPLOYMENT_NAME=gpt-3.5-turbo

# Start application in dev mode
./gradlew quarkusDev

# In another terminal, test the endpoint
curl -X POST http://localhost:8080/api/auth/guest -H "Content-Type: application/json"
# Extract token from response and use it:

curl -X POST http://localhost:8080/api/llm/fake-names \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "inputName": "青木 勇樹",
    "variance": 0.1
  }'
```

## Verification Checklist

When validating the WireMock implementation:

- [x] Request requires `Authorization` header with Bearer token
- [x] Request requires `Content-Type: application/json` header
- [x] Request body must have `model` field
- [x] Request body must have `messages` array field
- [x] Response has `id` field with format `chatcmpl-{id}`
- [x] Response has `object` field with value `chat.completion`
- [x] Response has `created` timestamp field
- [x] Response has `model` field
- [x] Response has `choices` array with at least one choice
- [x] Each choice has `index`, `message`, and `finish_reason` fields
- [x] Message has `role: "assistant"` and `content` fields
- [x] Response has `usage` object with token counts
- [x] Unauthorized requests return 401 status

## References

- [OpenAI API Documentation](https://platform.openai.com/docs/api-reference/chat/create)
- [WireMock Documentation](https://wiremock.org/docs/)
- [Quarkus LangChain4j Documentation](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
