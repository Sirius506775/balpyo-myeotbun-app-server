package site.balpyo.fcm.dto;

import lombok.*;

/**
 * 모바일에서 전달받은 객체
 *
 * @author : dongheonlee
 * @fileName : FcmSendDTO
 * @since : 06/20/2024
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmSendDTO {
    private String token;

    private String title;

    private String body;

    private String script_id;

}