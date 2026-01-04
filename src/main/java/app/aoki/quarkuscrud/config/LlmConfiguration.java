package app.aoki.quarkuscrud.config;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Configuration class for LLM models. */
@ApplicationScoped
public class LlmConfiguration {

  @ConfigProperty(name = "quarkus.langchain4j.azure-openai.api-key")
  String apiKey;

  @ConfigProperty(name = "quarkus.langchain4j.azure-openai.endpoint")
  String endpoint;

  @ConfigProperty(name = "quarkus.langchain4j.azure-openai.security-model.deployment-name")
  String securityModelDeploymentName;

  /**
   * Produces a ChatLanguageModel for security checks using a lightweight model (GPT-4.1 mini).
   *
   * @return the security model
   */
  @Produces
  @Named("securityModel")
  @ApplicationScoped
  public ChatLanguageModel securityModel() {
    return AzureOpenAiChatModel.builder()
        .apiKey(apiKey)
        .endpoint(endpoint)
        .deploymentName(securityModelDeploymentName)
        .timeout(Duration.ofSeconds(10))
        .maxRetries(2)
        .logRequests(true)
        .logResponses(true)
        .build();
  }
}
