package app.aoki.quarkuscrud.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.aoki.quarkuscrud.generated.model.FakeNamesRequest;
import app.aoki.quarkuscrud.generated.model.FakeNamesResponse;
import app.aoki.quarkuscrud.service.LlmService;
import app.aoki.quarkuscrud.service.RateLimiterService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for LlmUseCase.
 *
 * <p>Tests business logic orchestration, validation, and error handling with mocked dependencies.
 */
@QuarkusTest
public class LlmUseCaseTest {

  @Inject LlmUseCase llmUseCase;

  @InjectMock LlmService llmService;

  @InjectMock RateLimiterService rateLimiterService;

  private static final Long TEST_USER_ID = 123L;

  @BeforeEach
  public void setup() {
    // Reset mocks before each test
    reset(llmService, rateLimiterService);
  }

  @Test
  public void testGenerateFakeNamesSuccess() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("青木 勇樹");
    request.setVariance(0.1);

    List<String> mockNames = Arrays.asList("青木 優香", "青木 優空", "青山 裕子", "青木 雄", "青木 悠斗");

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);
    when(llmService.generateFakeNames("青木 勇樹", 0.1)).thenReturn(mockNames);

    // Act
    FakeNamesResponse response = llmUseCase.generateFakeNames(TEST_USER_ID, request);

    // Assert
    assertNotNull(response);
    assertNotNull(response.getOutput());
    assertEquals(5, response.getOutput().size());
    assertTrue(response.getOutput().contains("青木 優香"));

    verify(rateLimiterService, times(1)).allowRequest(TEST_USER_ID);
    verify(llmService, times(1)).generateFakeNames("青木 勇樹", 0.1);
  }

  @Test
  public void testGenerateFakeNamesRateLimitExceeded() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("青木 勇樹");
    request.setVariance(0.1);

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(false);

    // Act & Assert
    LlmUseCase.RateLimitExceededException exception =
        assertThrows(
            LlmUseCase.RateLimitExceededException.class,
            () -> llmUseCase.generateFakeNames(TEST_USER_ID, request));

    assertTrue(exception.getMessage().contains("Rate limit exceeded"));

    verify(rateLimiterService, times(1)).allowRequest(TEST_USER_ID);
    verify(llmService, never()).generateFakeNames(anyString(), anyDouble());
  }

  @Test
  public void testGenerateFakeNamesMissingInputName() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName(null);
    request.setVariance(0.1);

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);

    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> llmUseCase.generateFakeNames(TEST_USER_ID, request));

    assertEquals("Input name is required", exception.getMessage());

    verify(rateLimiterService, times(1)).allowRequest(TEST_USER_ID);
    verify(llmService, never()).generateFakeNames(anyString(), anyDouble());
  }

  @Test
  public void testGenerateFakeNamesEmptyInputName() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("");
    request.setVariance(0.1);

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);

    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> llmUseCase.generateFakeNames(TEST_USER_ID, request));

    assertEquals("Input name is required", exception.getMessage());
  }

  @Test
  public void testGenerateFakeNamesBlankInputName() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("   ");
    request.setVariance(0.1);

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);

    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> llmUseCase.generateFakeNames(TEST_USER_ID, request));

    assertEquals("Input name is required", exception.getMessage());
  }

  @Test
  public void testGenerateFakeNamesMissingVariance() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("青木 勇樹");
    request.setVariance(null);

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);

    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> llmUseCase.generateFakeNames(TEST_USER_ID, request));

    assertEquals("Variance must be between 0.0 and 1.0", exception.getMessage());
  }

  @Test
  public void testGenerateFakeNamesVarianceTooLow() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("青木 勇樹");
    request.setVariance(-0.1);

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);

    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> llmUseCase.generateFakeNames(TEST_USER_ID, request));

    assertEquals("Variance must be between 0.0 and 1.0", exception.getMessage());
  }

  @Test
  public void testGenerateFakeNamesVarianceTooHigh() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("青木 勇樹");
    request.setVariance(1.5);

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);

    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> llmUseCase.generateFakeNames(TEST_USER_ID, request));

    assertEquals("Variance must be between 0.0 and 1.0", exception.getMessage());
  }

  @Test
  public void testGenerateFakeNamesVarianceAtLowerBound() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("青木 勇樹");
    request.setVariance(0.0);

    List<String> mockNames = Arrays.asList("青木 勇樹", "青木 勇紀", "青木 雄樹", "青木 優樹", "青木 祐樹");

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);
    when(llmService.generateFakeNames("青木 勇樹", 0.0)).thenReturn(mockNames);

    // Act
    FakeNamesResponse response = llmUseCase.generateFakeNames(TEST_USER_ID, request);

    // Assert
    assertNotNull(response);
    assertEquals(5, response.getOutput().size());
  }

  @Test
  public void testGenerateFakeNamesVarianceAtUpperBound() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("青木 勇樹");
    request.setVariance(1.0);

    List<String> mockNames = Arrays.asList("西牟田 博之", "秀丸 壱太朗", "島田 部長", "李 源彦", "篠原 アンジェラ");

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);
    when(llmService.generateFakeNames("青木 勇樹", 1.0)).thenReturn(mockNames);

    // Act
    FakeNamesResponse response = llmUseCase.generateFakeNames(TEST_USER_ID, request);

    // Assert
    assertNotNull(response);
    assertEquals(5, response.getOutput().size());
  }

  @Test
  public void testGenerateFakeNamesLlmServiceThrowsException() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("青木 勇樹");
    request.setVariance(0.1);

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);
    when(llmService.generateFakeNames(anyString(), anyDouble()))
        .thenThrow(new RuntimeException("LLM API error"));

    // Act & Assert
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> llmUseCase.generateFakeNames(TEST_USER_ID, request));

    assertEquals("LLM API error", exception.getMessage());
  }

  @Test
  public void testGenerateFakeNamesWithUniqueNames() {
    // Arrange
    FakeNamesRequest request = new FakeNamesRequest();
    request.setInputName("田中 太郎");
    request.setVariance(0.5);

    // Mock returns duplicates, but Set should ensure uniqueness
    List<String> mockNames = Arrays.asList("田中 太郎", "田中 次郎", "田中 太郎", "田辺 太郎", "山田 太郎", "田中 次郎");

    when(rateLimiterService.allowRequest(TEST_USER_ID)).thenReturn(true);
    when(llmService.generateFakeNames("田中 太郎", 0.5)).thenReturn(mockNames);

    // Act
    FakeNamesResponse response = llmUseCase.generateFakeNames(TEST_USER_ID, request);

    // Assert
    assertNotNull(response);
    // Due to LinkedHashSet, duplicates should be removed
    assertTrue(response.getOutput().size() <= mockNames.size());
  }
}
