package site.balpyo.fcm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.balpyo.fcm.dto.ApiResponseWrapper;
import site.balpyo.fcm.dto.FcmSendDTO;
import site.balpyo.fcm.entity.SuccessCode;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/fcm")
public class FcmController {

    private final FcmServiceImpl fcmService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponseWrapper<Object>> pushMessage(@RequestBody FcmSendDTO fcmSendDTO) {
        

        log.debug("[+] 푸시 메시지를 전송합니다. ");
        Mono<Integer> result;
        try {
            result = fcmService.sendMessageTo(fcmSendDTO);
            ApiResponseWrapper<Object> arw = ApiResponseWrapper
            .builder()
            .result(result)
            .resultCode(SuccessCode.SELECT_SUCCESS.getStatus())
            .resultMsg(SuccessCode.SELECT_SUCCESS.getMessage())
            .build();
            
            return new ResponseEntity<>(arw, HttpStatus.OK);

        } catch (IOException e) {
            log.error("[-] 푸시 메시지 전송에 실패했습니다. ");
            ApiResponseWrapper<Object> arw = ApiResponseWrapper
            .builder()
            .result(null)
            .resultCode(SuccessCode.SELECT_FAIL.getStatus())
            .resultMsg(SuccessCode.SELECT_FAIL.getMessage())
            .build();

            return new ResponseEntity<>(arw, HttpStatus.INTERNAL_SERVER_ERROR);
        } 

      
    }

}