package app.aoki.quarkuscrud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LlmService {

  private static final Logger LOG = Logger.getLogger(LlmService.class);

  @Inject ChatLanguageModel chatModel;

  @Inject ObjectMapper objectMapper;

  private static final String FAKE_NAMES_PROMPT_TEMPLATE =
      """
            <instruction>
            以下の説明に従って、AIにタスクを実行させてください。
            1. 入力として与えられる名前「{{input_name}}」に基づいて、似たような名前を生成してください。
            2. 「{{input_name}}」の姓または名前、あるいはその両方を基にした、意味的または音的に関連する名前を出してください。
            3. 生成された名前は、元の名前に近いものだけ含み、極端に異なるものを除いてください。
            4. 出力は平易な日本語で記述し、`output`キーでラップされたJSONオブジェクトとして提示してください。
            5. 出力例は以下の形式を遵守してください:
               - JSONオブジェクトの中に`output`キーを含む
               - `output`キーの値は、名前の候補を格納したJSON配列
               - JSON配列内の各要素は一意で、5つ以上の名前を含む必要があります
            6. varianceに従って、多様性の度合いを調整せよ。
               (0に近いほど、よく似た名前が出る。1に近いほど、バラバラで関連性が薄い)。
               varianceとは日本語で多様度、という意味で、無次元量の0-1のratio value。
               1に近いと乱雑になる。
            7. 候補数は最低5つ以上、あればもっと出してください。

            正しい出力例:
            青木 優香
            青木 優空
            青山 裕子
            青木 雄
            高木 青斗
            葵木 悠木

            生成に際して以下を参考にしてください。
            - 「姓」の一文字変更や類似した姓の提案 (例: 青木 -> 青山)
            - 「名」の音的・文字的変更や近似値の生成 (例: 勇樹 -> 優香, 優空)
            - 姓または名のバリエーションを適切に組み合わせる
            - varianceが1に近い場合、似ていない名前ってどんな名前なのか、意識する。

            また、出力されるJSONは以下のJSON Schemaに従わなければならない。
            <schema>
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Output Wrapper Schema",
              "type": "object",
              "properties": {
                "output": {
                  "type": "array",
                  "items": {
                    "type": "string",
                    "description": "名前を表す文字列"
                  },
                  "minItems": 5,
                  "uniqueItems": true,
                  "description": "元の名前に似た名前のリストを含むJSON配列形式"
                }
              },
              "required": ["output"],
              "description": "outputキーでラップされた似た名前のリストを検証するスキーマ"
            }
            </schema>
            </instruction>

            <input>
            名前: {{input_name}}
            variance: {{variance}}
            </input>

            <output>
            {
              "output": ["名前1", "名前2", "名前3", "名前4", "名前5"]
            }
            </output>
            """;

  public List<String> generateFakeNames(String inputName, double variance) {
    LOG.infof("Generating fake names for: %s with variance: %.2f", inputName, variance);

    try {
      // Replace placeholders in the prompt template
      String prompt =
          FAKE_NAMES_PROMPT_TEMPLATE
              .replace("{{input_name}}", inputName)
              .replace("{{variance}}", String.valueOf(variance));

      // Call the LLM
      String response = chatModel.generate(prompt);
      LOG.debugf("LLM raw response: %s", response);

      // Parse the JSON response
      List<String> fakeNames = parseResponse(response);

      if (fakeNames.isEmpty()) {
        LOG.warn("LLM returned empty list of fake names");
        throw new RuntimeException("LLM returned no fake names");
      }

      LOG.infof("Generated %d fake names", fakeNames.size());
      return fakeNames;

    } catch (Exception e) {
      LOG.errorf(e, "Failed to generate fake names");
      throw new RuntimeException("Failed to generate fake names: " + e.getMessage(), e);
    }
  }

  private List<String> parseResponse(String response) {
    try {
      // Try to extract JSON from the response
      String jsonStr = extractJson(response);

      // Parse the JSON
      JsonNode jsonNode = objectMapper.readTree(jsonStr);
      JsonNode outputNode = jsonNode.get("output");

      if (outputNode == null || !outputNode.isArray()) {
        LOG.error("Response does not contain 'output' array");
        return new ArrayList<>();
      }

      List<String> names = new ArrayList<>();
      for (JsonNode nameNode : outputNode) {
        if (nameNode.isTextual()) {
          names.add(nameNode.asText());
        }
      }

      return names;

    } catch (Exception e) {
      LOG.errorf(e, "Failed to parse LLM response: %s", response);
      return new ArrayList<>();
    }
  }

  private String extractJson(String response) {
    // Try to find JSON object in the response
    int start = response.indexOf("{");
    int end = response.lastIndexOf("}");

    if (start != -1 && end != -1 && end > start) {
      return response.substring(start, end + 1);
    }

    // If no JSON found, return the whole response
    return response;
  }
}
