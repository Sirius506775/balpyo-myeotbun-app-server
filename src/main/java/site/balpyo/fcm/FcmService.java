package site.balpyo.fcm;

import java.io.IOException;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import site.balpyo.fcm.dto.FcmSendDTO;

@Service
public interface FcmService {

    Mono<Integer> sendMessageTo(FcmSendDTO fcmSendDTO) throws IOException;
    
}
