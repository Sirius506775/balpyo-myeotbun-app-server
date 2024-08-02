package site.balpyo.script.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import site.balpyo.ai.repository.AIGenerateLogRepository;
import site.balpyo.ai.repository.GPTInfoRepository;
import site.balpyo.common.dto.CommonResponse;
import site.balpyo.common.dto.ErrorEnum;
import site.balpyo.guest.entity.GuestEntity;
import site.balpyo.guest.repository.GuestRepository;
import site.balpyo.script.dto.ScriptRequest;
import site.balpyo.script.dto.ScriptResponse;
import site.balpyo.script.entity.ScriptEntity;
import site.balpyo.script.repository.ScriptRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScriptService {

    private final ScriptRepository scriptRepository;
    private final GuestRepository guestRepository;


    public ResponseEntity<CommonResponse> saveEmptyScript(ScriptRequest scriptRequest, String uid) {

        GuestEntity guestEntity = null;
        if (uid != null) {
            guestEntity = guestRepository.findById(uid).orElse(null);
        }

        ScriptEntity scriptEntity = ScriptEntity.builder()
                .title(scriptRequest.getTitle())
                .script(scriptRequest.getScript())
                .secTime(scriptRequest.getSecTime())
                .guestEntity(guestEntity)
                .isGenerating(scriptRequest.isUseAi())
                .build();


        ScriptEntity insertedScriptEntity = scriptRepository.save(scriptEntity);
        ScriptResponse scriptResponse = ScriptResponse.builder()
                .scriptId(insertedScriptEntity.getScript_id())
                .isGenerating(insertedScriptEntity.getIsGenerating())
                .build();

        return CommonResponse.success(scriptResponse);
    }

    public ResponseEntity<CommonResponse> getAllScript(String uid) {
        Optional<GuestEntity> guestEntity = guestRepository.findById(uid);

        if(guestEntity.isEmpty())return CommonResponse.error(ErrorEnum.GUEST_NOT_FOUND);
        List<ScriptResponse> scriptResponses = new ArrayList<>();
        if(guestEntity.get().getScriptEntities().isEmpty())return CommonResponse.success(scriptResponses);
        List<ScriptEntity> scriptEntities = scriptRepository.findAllByGuestEntityUidAndIsGeneratingFalse(uid);


        for(ScriptEntity scriptEntity: scriptEntities){
            ScriptResponse scriptResponse = ScriptResponse.builder()
                    .scriptId(scriptEntity.getScript_id())
                    .uid(uid)
                    .title(scriptEntity.getTitle())
                    .secTime(scriptEntity.getSecTime())
                    .voiceFilePath(scriptEntity.getVoiceFilePath())
                    .isGenerating(scriptEntity.getIsGenerating())
                    .build();

            scriptResponses.add(scriptResponse);
        }

        return CommonResponse.success(scriptResponses);

    }


    public ResponseEntity<CommonResponse> patchScript(ScriptRequest scriptRequest, String uid,Long scriptId) {
        Optional<ScriptEntity> optionalScriptEntity = scriptRepository.findScriptByGuestUidAndScriptId(uid, scriptId);

        if(optionalScriptEntity.isEmpty())return CommonResponse.error(ErrorEnum.SCRIPT_DETAIL_NOT_FOUND);

        ScriptEntity scriptEntity = optionalScriptEntity.get();


        scriptEntity.setScript(scriptRequest.getScript());
        scriptEntity.setTitle(scriptRequest.getTitle());
        scriptEntity.setSecTime(scriptRequest.getSecTime());



        if (scriptRequest.getVoiceFilePath() != null && !scriptRequest.getVoiceFilePath().isEmpty()) {
            scriptEntity.setVoiceFilePath(scriptRequest.getVoiceFilePath());
        }

        scriptRepository.save(scriptEntity);

        return CommonResponse.success("");


    }


    public ResponseEntity<CommonResponse> deleteScript(String uid, Long scriptId) {
        Optional<ScriptEntity> optionalScriptEntity = scriptRepository.findScriptByGuestUidAndScriptId(uid, scriptId);

        if(optionalScriptEntity.isEmpty())return CommonResponse.error(ErrorEnum.SCRIPT_DETAIL_NOT_FOUND);

        ScriptEntity scriptEntity = optionalScriptEntity.get();

        scriptRepository.delete(scriptEntity);

        return CommonResponse.success("");

    }
}