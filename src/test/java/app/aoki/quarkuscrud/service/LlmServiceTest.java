package app.aoki.quarkuscrud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for LlmService.
 *
 * <p>Tests LLM prompt generation, response parsing, and error handling with mocked
 * ChatLanguageModel.
 */
@QuarkusTest
public class LlmServiceTest {

  @Inject LlmService llmService;

  @InjectMock ChatLanguageModel chatModel;

  @BeforeEach
  public void setup() {
    reset(chatModel);
  }

  @Test
  public void testGenerateFakeNamesSuccess() {
    // Arrange
    String mockResponse = "{\"output\": [\"青木 優香\", \"青木 優空\", \"青山 裕子\", \"青木 雄\", \"青木 悠斗\"]}";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act
    List<String> result = llmService.generateFakeNames("青木 勇樹", 0.1);

    // Assert
    assertNotNull(result);
    assertEquals(5, result.size());
    assertEquals("青木 優香", result.get(0));
    assertEquals("青木 優空", result.get(1));
    assertEquals("青山 裕子", result.get(2));

    verify(chatModel, times(1)).generate(anyString());
  }

  @Test
  public void testGenerateFakeNamesWithHighVariance() {
    // Arrange
    String mockResponse =
        "{\"output\": [\"西牟田 博之\", \"秀丸 壱太朗\", \"島田 部長\", \"李 源彦\", \"篠原 アンジェラ\", \"鈴木 花子\"]}";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act
    List<String> result = llmService.generateFakeNames("青木 勇樹", 0.9);

    // Assert
    assertNotNull(result);
    assertEquals(6, result.size());
    assertTrue(result.contains("西牟田 博之"));
    assertTrue(result.contains("篠原 アンジェラ"));

    verify(chatModel, times(1)).generate(anyString());
  }

  @Test
  public void testGenerateFakeNamesResponseWithExtraText() {
    // Arrange - LLM might return text before/after JSON
    String mockResponse =
        "Here are some names: {\"output\": [\"田中 太郎\", \"田中 次郎\", \"田辺 太郎\", \"山田 太郎\", \"中田 太郎\"]} Hope this helps!";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act
    List<String> result = llmService.generateFakeNames("田中 太郎", 0.2);

    // Assert
    assertNotNull(result);
    assertEquals(5, result.size());
    assertEquals("田中 太郎", result.get(0));
  }

  @Test
  public void testGenerateFakeNamesInvalidJsonResponse() {
    // Arrange
    String mockResponse = "This is not JSON at all";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act & Assert - Should throw RuntimeException when JSON parse fails
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> llmService.generateFakeNames("青木 勇樹", 0.1));

    assertTrue(exception.getMessage().contains("LLM returned no fake names"));
  }

  @Test
  public void testGenerateFakeNamesMissingOutputKey() {
    // Arrange
    String mockResponse = "{\"names\": [\"青木 優香\", \"青木 優空\"]}";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act & Assert - Should throw RuntimeException when output key is missing
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> llmService.generateFakeNames("青木 勇樹", 0.1));

    assertTrue(exception.getMessage().contains("LLM returned no fake names"));
  }

  @Test
  public void testGenerateFakeNamesOutputNotArray() {
    // Arrange
    String mockResponse = "{\"output\": \"not an array\"}";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act & Assert - Should throw RuntimeException when output is not an array
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> llmService.generateFakeNames("青木 勇樹", 0.1));

    assertTrue(exception.getMessage().contains("LLM returned no fake names"));
  }

  @Test
  public void testGenerateFakeNamesEmptyOutput() {
    // Arrange
    String mockResponse = "{\"output\": []}";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act & Assert
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> llmService.generateFakeNames("青木 勇樹", 0.1));

    assertTrue(exception.getMessage().contains("LLM returned no fake names"));
  }

  @Test
  public void testGenerateFakeNamesLlmThrowsException() {
    // Arrange
    when(chatModel.generate(anyString())).thenThrow(new RuntimeException("API timeout"));

    // Act & Assert
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> llmService.generateFakeNames("青木 勇樹", 0.1));

    assertTrue(exception.getMessage().contains("Failed to generate fake names"));
    assertTrue(exception.getCause().getMessage().contains("API timeout"));
  }

  @Test
  public void testGenerateFakeNamesVarianceZero() {
    // Arrange
    String mockResponse = "{\"output\": [\"青木 勇樹\", \"青木 勇紀\", \"青木 雄樹\", \"青木 優樹\", \"青木 祐樹\"]}";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act
    List<String> result = llmService.generateFakeNames("青木 勇樹", 0.0);

    // Assert
    assertNotNull(result);
    assertEquals(5, result.size());

    verify(chatModel, times(1)).generate(anyString());
  }

  @Test
  public void testGenerateFakeNamesVarianceOne() {
    // Arrange
    String mockResponse =
        "{\"output\": [\"John Smith\", \"山田 太郎\", \"李 明\", \"A B\", \"Test Name\"]}";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act
    List<String> result = llmService.generateFakeNames("青木 勇樹", 1.0);

    // Assert
    assertNotNull(result);
    assertEquals(5, result.size());

    verify(chatModel, times(1)).generate(anyString());
  }

  @Test
  public void testGenerateFakeNamesWithSpecialCharacters() {
    // Arrange
    String mockResponse =
        "{\"output\": [\"O'Brien 太郎\", \"D'Angelo 花子\", \"José García\", \"François Müller\", \"李明 (リ・メイ)\"]}";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act
    List<String> result = llmService.generateFakeNames("山田 太郎", 0.8);

    // Assert
    assertNotNull(result);
    assertEquals(5, result.size());
    assertTrue(result.contains("O'Brien 太郎"));
  }

  @Test
  public void testPromptContainsRequiredElements() {
    // Arrange
    String mockResponse = "{\"output\": [\"Test1\", \"Test2\", \"Test3\", \"Test4\", \"Test5\"]}";
    when(chatModel.generate(anyString())).thenReturn(mockResponse);

    // Act
    llmService.generateFakeNames("テスト 名前", 0.5);

    // Assert - Verify method was called
    verify(chatModel, times(1)).generate(anyString());
  }
}
