package site.balpyo.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor    
public class AIGenerateUtils {
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final WebClient.Builder webClientBuilder;

    public String createPromptString(String topic, String keywords, Integer sec) {

        // 초기화한 값에 해당하는 글자 수와 시간 비율 계산
        int initialCharacterCount = 425; // 초기화한 공백 포함 글자 수
        double characterPerSecond = (double) initialCharacterCount / 60.0; // 초당 평균 글자 수

        log.info("-------------------- Character per second : " + characterPerSecond);

        // 주어진 시간(sec)에 해당하는 글자 수 계산
        int targetCharacterCount = (int) (sec * characterPerSecond);

        log.info("-------------------- 주어진 시간(" + sec + "초)에 해당하는 예상 글자수 : " + targetCharacterCount);

        // 주어진 시간(sec)에 해당하는 바이트 수 계산
        int targetByteCount = targetCharacterCount * 3; // 한글은 3바이트로 가정

        log.info("-------------------- 예상 바이트수 : " + targetByteCount + "byte");


        String bad_example_prompt = """
                ## It's bad response example

                Given the complexity of generating a script that meets the exact byte requirement of 6417 bytes, including spaces, special characters, and line breaks, but excluding certain characters as specified, it's challenging to ensure precision in the first attempt. However, I can provide you with a script on the topic \"부산에서 제일 인기있는 메뉴는 뭘까?\" highlighting popular foods in Busan, which you can then adjust as needed to meet your specific byte requirement. Here's a sample script to get you started:\n\n---\n\n안녕하세요, 여러분. 오늘 제가 여러분께 소개할 주제는 바로 '부산에서 제일 인기있는 메뉴는 뭘까?'입니다. 부산하면 떠오르는 이미지가 있으신가요? 아름다운 해변, 시원한 바람, 그리고 무엇보다도 맛있는 음식, 맞나요? 네, 오늘은 그 중에서도 부산을 대표하는 인기 메뉴에 대해 이야기해보려고 합니다.\n\n첫 번째로 소개할 음식은 바로 '밀면'입니다. 부산의 뜨거운 여름, 시원한 밀면 한 그릇이야말로 최고의 별미죠. 밀면은 밀가루로 만든 면에 육수와 얼음을 넣어 먹는 음식으로, 간단해 보이지만 그 맛이 일품입니다. 특히 부산 사람들은 여름철에 밀면을 자주 찾는다고 해요. 밀면 위에 올라가는 고명으로는 얇게 썬 오이, 계란 반쪽, 그리고 특제 소스가 빠질 수 없죠.\n\n두 번째로 소개할 음식은 '동래파전'입니다. 동래 지역은 옛날부터 파전으로 유명했는데요, 부산에 방문하면 꼭 한 번쯤은 맛보아야 할 음식 중 하나입니다. 바삭바삭한 파전에 막걸리 한 잔은 정말 꿀맛이죠. 특히 비 오는 날에는 파전과 막걸리의 인기가 더욱 높아진답니다.\n\n세 번째로는 '부산어묵'을 빼놓을 수 없겠죠. 부산하면 어묵, 어묵하면 부산이라 할 정도로 유명한 음식입니다. 부산어묵은 그 맛과 품질이 전국적으로 인정받고 있어요. 특히 겨울철에 따뜻한 어묵국물은 추위를 녹이는데 제격이죠. 부산의 어묵은 다양한 모양과 크기로 제공되어, 먹는 재미가 있습니다.\n\n네 번째로 추천드릴 음식은 '해운대 회'입니다. 부산하면 해운대를 빼놓을 수 없고, 해운대하면 신선한 회를 빼놓을 수 없죠. 해운대 해변가에는 신선한 회를 즐길 수 있는 수많은 횟집이 즐비해 있습니다. 바다를 바라보며 먹는 신선한 회는 그 맛이 일품입니다. 특히, 부산은 해산물의 천국이라 다양한 종류의 회를 맛볼 수 있어요.\n\n마지막으로 소개할 음식은 '씨앗호떡'입니다. 부산의 겨울 거리를 걷다 보면 꼭 마주치게 되는 간식이죠. 달콤한 설탕과 견과류가 가득 찬 씨앗호떡은 한 입 베어 물면 겨울의 추위도 잊게 만듭니다. 부산을 방문한 관광객들 사이에서도 꼭 맛봐야 할 음식으로 손꼽히고 있어요.\n\n여러분, 오늘 소개해 드린 부산의 인기 메뉴는 어떠셨나요? 밀면부터 시작해 동래파전, 부산어묵, 해운대 회, 그리고 씨앗호떡까지. 부산에 가시게 되면 꼭 한 번씩 맛보시길 바랍니다. 맛있는 음식과 함께 즐거운 추억도 많이 만드시길 바랍니다. 감사합니다.\n\n---\n\nThis script introduces popular dishes in Busan and encourages the audience to try them out. Adjusting it to meet the exact byte requirement would involve closely monitoring the length and possibly adding or removing content to fit the specifications.

                """;

        String good_example_prompt = """
                ## It's good response example

                안녕하세요! 오늘은 \"제주도 성산 맛집은 어디지?\"라는 주제로 여러분과 함께할 예정입니다. 제주도 하면 떠오르는 것들이 많지만, 그중에서도 음식은 빼놓을 수 없는 매력 중 하나입니다. 특히, 성산 일대는 그 매력을 두 배로 느낄 수 있는 곳이죠. 오늘 저와 함께 성산의 숨겨진 맛집을 탐방해 보실까요?\n\n먼저, 제주도 하면 빼놓을 수 없는 음식 중 하나는 바로 흑돼지 바비큐입니다. 성산에는 이 흑돼지를 전문으로 하는 맛집이 여럿 있는데요, 그중에서도 가장 추천드리고 싶은 곳은 '성산흑돼지마을'입니다. 이곳의 흑돼지는 육즙이 풍부하고 고소한 맛이 일품인데요, 제주도의 청정 자연에서 자란 흑돼지만을 사용하기 때문에 그 맛이 더욱 특별합니다.\n\n다음으로 소개해 드릴 맛집은 '성산일출봉해물라면'입니다. 제주도 하면 해물도 빼놓을 수 없죠. 이곳의 해물라면은 신선한 해물과 진한 육수가 어우러져, 한 그릇으로도 충분히 든든하고 만족스러운 식사가 됩니다. 특히, 일출을 보며 먹는 해물라면의 맛은 잊을 수 없는 추억이 될 거예요.\n\n제주도의 또 다른 매력은 바로 감귤입니다. 성산에는 '감귤밭 카페'라는 곳이 있는데요, 이곳에서는 신선한 감귤로 만든 다양한 음료와 디저트를 즐길 수 있습니다. 감귤밭 한가운데 위치해 있어, 감귤의 향기를 맡으며 여유로운 시간을 보낼 수 있는 것이 특징입니다.\n\n마지막으로, 제주도의 바다를 느낄 수 있는 맛집을 소개해 드리겠습니다. '성산포해녀의집'입니다. 이곳에서는 해녀가 직접 잡은 신선한 해산물을 맛볼 수 있는데요, 특히, 성게미역국은 이곳의 대표 메뉴입니다. 제주도의 바다를 한 스푼에 담은 듯한 깊고 진한 맛을 느낄 수 있습니다.\n\n오늘 소개해 드린 성산의 맛집들은 모두 그 지역만의 독특한 매력을 담고 있습니다. 제주도의 아름다운 자연과 어우러진 이 맛집들은 여러분의 여행을 더욱 특별하게 만들어 줄 것입니다. 제주도 성산에서의 맛있는 추억, 꼭 만들어 보시길 바랍니다.\n\n여러분, 제주도 성산의 맛집 탐방은 여기까지입니다. 제주도의 맛과 멋을 함께 느낄 수 있는 이번 여행이 여러분에게 즐거운 추억이 되었기를 바랍니다. 다음에 또 다른 맛집과 함께 돌아오겠습니다. 감사합니다.
                """;


        log.info("-------------------- Created Prompt String");
            
        String requestPrompt = "# AI role : You are a Korean presentation script writer.\n" +
                "You should write a script for a presentation in Korean.\n" + 
                "The topic is " + topic + ", and the keywords are " + keywords + ".\n" +
                "Please generate a script of " + targetByteCount + " bytes.\n" +
                "Count every character, including spaces, special characters, and line breaks, as one byte.\n" +
                "When creating a script, exclude characters such as '(', ')', ''', '-', '[', ']' and '_'.\n" +
                "Please write a script that is easy to read and understand.\n" +
                "I want a text form in which people can read and speak directly.\n"+
                "Because the script will be used for a presentation, please make sure it is engaging and informative.\n" +
                "This is to prevent bugs that may occur in scripts that include request values in the response.\n" +
                "It must be exactly " + targetByteCount + " bytes long. \n" +
                "# example\n" + 
                bad_example_prompt + "\n" +
                good_example_prompt + "\n"
                +
                "GPT, you're smart enough to provide me with a script of " + targetByteCount + " bytes, right? Can you do that?";

        return requestPrompt;
    }



    public ResponseEntity<Map> requestGPTTextGeneration(String prompt, float temperature, int maxTokens ,String API_KEY) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_KEY);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4-0125-preview");
        requestBody.put("messages", Arrays.asList(message));
        requestBody.put("temperature", temperature);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(ENDPOINT, requestEntity, Map.class);

        return response;
    }


    public Mono<Map> requestScriptGeneration(String prompt, float temperature, int maxTokens ,String API_KEY) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_KEY);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4-0125-preview");
        requestBody.put("messages", Arrays.asList(message));
        requestBody.put("temperature", temperature);

        Mono<Map> response = webClientBuilder.build()
                .post()
                .uri(ENDPOINT)
                .header("Authorization", "Bearer " + API_KEY)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class);

        return response;
    }

}