package site.balpyo.fcm;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import site.balpyo.fcm.dto.FcmMessageDTO;
import site.balpyo.fcm.dto.FcmSendDTO;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class FcmServiceImpl implements FcmService {

    private final ObjectMapper objectMapper;

    private final WebClient.Builder webClientBuilder;

    String API_URL = "https://fcm.googleapis.com/v1/projects/balpyo-myeotbun/messages:send";

        
    @Override
    public Mono<Integer> sendMessageTo(FcmSendDTO fcmSendDTO) throws IOException {

        String message = makeMessage(fcmSendDTO);
        String accessToken;

        try {
            accessToken = getAccessToken();
            log.info("-------------------- Generated Access Token ");
        } catch (IOException e) {
            log.error("[-] 액세스 토큰을 가져오는 데 실패했습니다.", e);
            return Mono.error(e); // IOException 발생 시 Mono.error로 반환
        }
    
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getAccessToken());

        return webClientBuilder.build()
                .post()
                .uri(API_URL)
                .headers(httpHeaders -> {
                    httpHeaders.addAll(headers);
                })
                .bodyValue(message)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful() ? 1 : 0)
                .doOnError(e -> {
                    log.error("[-] FCM 메시지 전송에 실패했습니다.", e);
                });
    }


    private String makeMessage(FcmSendDTO fcmSendDTO) throws JsonParseException, JsonProcessingException {

        Map<String, String> data = Map.of("scriptId", fcmSendDTO.getScript_id());

        FcmMessageDTO fcmMessageDTO = FcmMessageDTO.builder()
                .message(FcmMessageDTO.Message.builder()
                        .token(fcmSendDTO.getToken())
                        .notification(FcmMessageDTO.Notification.builder()
                                .title(fcmSendDTO.getTitle())
                                .body(fcmSendDTO.getBody())
                                .image(null)
                                .build()
                        )
                        .data(data) // scriptId 추가
                        .build()).validateOnly(false).build();

        log.info("-------------------- Created Firebase Notification Message");
        // log.info("-------------------- ", fcmMessageDTO.toString());

        return objectMapper.writeValueAsString(fcmMessageDTO);
    }

    private String getAccessToken() throws IOException {
        String firebaseConfigPath = "firebase/balpyo-myeotbun-firebase-adminsdk-7vz0s-bc87366e4d.json";

        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new ClassPathResource(firebaseConfigPath).getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }
}