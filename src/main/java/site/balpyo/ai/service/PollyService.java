package site.balpyo.ai.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.balpyo.ai.dto.PollyDTO;
import site.balpyo.ai.dto.SynthesizeSpeechResultDTO;
import site.balpyo.ai.dto.upload.UploadResultDTO;
import site.balpyo.common.s3.S3Client;


import java.io.*;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * @author dongheonlee
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PollyService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    private final S3Client s3Client;

    /**
     * 입력된 텍스트와 선택된 빠르기에 따라 음성파일으로 변환하여 반환한다.
     *
     * @param pollyDTO
     * @return mp3 오디오 파일
     */
    public SynthesizeSpeechResultDTO synthesizeSpeech(PollyDTO pollyDTO) {

        String inputText = pollyDTO.getText();
        int speed = pollyDTO.getSpeed();

        log.info("-------------------- 클라이언트가 요청한 대본 :" + inputText);
        log.info("-------------------- 클라이언트가 요청한 빠르기 :" + speed);

        BasicAWSCredentials awsCreds = new BasicAWSCredentials(s3Client.getAccessKey(), s3Client.getSecretKey());
        AmazonPolly amazonPolly = AmazonPollyClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.AP_NORTHEAST_2) // AWS 리전 설정
                .build();


        // 빠르기 계산
        float relativeSpeed = calculateRelativeSpeed(speed);

        log.info("-------------------- 선택한 빠르기 :" + relativeSpeed);

        // SSML 텍스트 생성
        String ssmlText = buildSsmlText(inputText, relativeSpeed);

        // SynthesizeSpeechRequest 생성 및 설정
        SynthesizeSpeechRequest synthesizeSpeechRequest = new SynthesizeSpeechRequest()
                .withText(ssmlText)
                .withTextType(TextType.Ssml)
                .withOutputFormat(OutputFormat.Mp3) // MP3 형식
                .withVoiceId(VoiceId.Seoyeon) // 한국어 음성 변환 보이스
                .withTextType("ssml"); // SSML 형식 사용

        // Speech Marks 요청 설정
        SynthesizeSpeechRequest speechMarkRequest = new SynthesizeSpeechRequest()
                .withText(ssmlText)
                .withTextType(TextType.Ssml)
                .withVoiceId(VoiceId.Seoyeon)
                .withOutputFormat(OutputFormat.Json) // JSON 형식으로 Speech Mark 정보 요청
                .withSpeechMarkTypes(SpeechMarkType.Word);

        // Speech Mark 정보 요청
        InputStream inputStream = amazonPolly.synthesizeSpeech(speechMarkRequest).getAudioStream();

        // Speech Marks 데이터를 담을 리스트
        List<Map<String, Object>> speechMarksList = new ArrayList<>();

        // 결과 처리 및 String으로 변환
        try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Map<String, Object> speechMarkData = getStringObjectMap(line);
                System.out.println(line);

                speechMarksList.add(speechMarkData);
            }
            // 현재 재생 시간을 밀리초 단위로 변환하고
            // 프론트에서 현재 재생 시간이 단어의 시작 시간과 끝 시간 사이에 있을 때 텍스트 색칠하면..?
        } catch (Exception e) {
            log.error("Speech Mark 정보 읽기 실패: ", e);
        }

        try { // 텍스트를 음성으로 변환하여 InputStream으로 반환
            SynthesizeSpeechResult synthesizeSpeechResult = amazonPolly.synthesizeSpeech(synthesizeSpeechRequest);
            InputStream audioStream = synthesizeSpeechResult.getAudioStream();
            log.info("-------------------- 요청된 문자열 개수 : " + synthesizeSpeechResult.getRequestCharacters());
            log.info("-------------------- 음성변환 요청 성공");

            // SynthesizeSpeechResultDTO 인스턴스 생성 및 반환
            return new SynthesizeSpeechResultDTO(audioStream, speechMarksList);
        } catch (AmazonPollyException e) {
            log.error("-------------------- 음성 변환 실패: " + e.getErrorMessage());
            throw e;
        }
    }

    private static Map<String, Object> getStringObjectMap(String line) {
        JSONObject jsonObject = new JSONObject(line);

        Map<String, Object> speechMarkData = new HashMap<>();
        speechMarkData.put("time", jsonObject.getInt("time"));
        speechMarkData.put("type", jsonObject.getString("type"));
        speechMarkData.put("start", jsonObject.getInt("start"));
        speechMarkData.put("end", jsonObject.getInt("end"));
        speechMarkData.put("value", jsonObject.getString("value"));
        return speechMarkData;
    }

    /**
     * mp3 audio 생성 시, 빠르기 설정 메소드
     */
    private static float calculateRelativeSpeed(int speed) {
        // 기본 속도
        float baseSpeed = 1.1f;

        switch (speed) {
            case -2:
                return baseSpeed * 0.9f;
            case -1:
                return baseSpeed * 0.975f;
            case 1:
                return baseSpeed * 1.125f;
            case 2:
                return baseSpeed * 1.15f;
            default:
                return baseSpeed;
        }
    }

    /**
     * SSML 텍스트 생성 메소드
     */
    private String buildSsmlText(String inputText, float relativeSpeed) {
        StringBuilder ssmlBuilder = new StringBuilder();
        ssmlBuilder.append("<speak>");
        ssmlBuilder.append(String.format("<prosody rate=\"%f%%\">", relativeSpeed * 100));

        for (int i = 0; i < inputText.length(); i++) {
            char ch = inputText.charAt(i);

            switch (ch) {
                case ',':
                    // 쉼표일 때 숨쉬기 태그 추가
                    ssmlBuilder.append("<break time=\"400ms\"/>");
                    break;
                case '.':
                    ssmlBuilder.append("<break time=\"601ms\"/>");
                    break;
                case '!':
                    ssmlBuilder.append("<break time=\"600ms\"/>");
                    break;
                case '?':
                    ssmlBuilder.append("<break time=\"801ms\"/>");
                    break;
                case '\n':
                    // 다음 문자가 개행 문자이면 숨소리 추가
                    if (i + 1 < inputText.length() && inputText.charAt(i + 1) == '\n') {
                        ssmlBuilder.append("<amazon:breath/>");
                        // 이미 \n\n을 처리했으므로 추가로 하나 더 넘어감
                        i++;
                    } else {
                        // 한 개의 개행 문자일 때 200ms 휴식 추가
                        ssmlBuilder.append("<break time=\"200ms\"/>");
                    }
                    break;
                case '숨':
                    if (inputText.startsWith("숨 고르기+1", i)) {
                        ssmlBuilder.append("<break time=\"1000ms\"/>");
                        i += 7; // "숨 고르기+1"의 길이만큼 인덱스 증가
                    }
                    break;
                case 'P':
                    if (inputText.startsWith("PPT 넘김+2", i)) {
                        ssmlBuilder.append("<break time=\"2000ms\"/>");
                        i += 8; // "PPT 넘김+2"의 길이만큼 인덱스 증가
                    }
                    break;    
                default:
                    // 기본 문자 처리
                    ssmlBuilder.append(ch);
                    break;
            }
        }

        ssmlBuilder.append("</prosody>");
        ssmlBuilder.append("</speak>");
        return ssmlBuilder.toString();
    }

    public UploadResultDTO synthesizeAndUploadSpeech(PollyDTO pollyDTO) {

        SynthesizeSpeechResultDTO synthesizeSpeechResultDTO = synthesizeSpeech(pollyDTO);
        InputStream audioStream = synthesizeSpeechResultDTO.getAudioStream(); // 음성 파일 생성
        List<Map<String, Object>> speechMarksList = synthesizeSpeechResultDTO.getSpeechMarks();

        // 파일 이름 생성
        String fileName = UUID.randomUUID() + ".mp3";

        log.info("--------------------- " + fileName);

        // S3에 업로드
        Map<String, Object> audioInfo = uploadToS3(audioStream, fileName);

        String baseUploadURL = audioInfo.get("baseUploadURL").toString();
        int durationInSeconds = (int) audioInfo.get("durationInSeconds");
        log.info("--------------------- " + baseUploadURL);
        log.info("--------------------- " + durationInSeconds);

        return UploadResultDTO.builder()
                .profileUrl(baseUploadURL)
                .playTime(durationInSeconds)
                .speechMarks(speechMarksList)
                .build();
    }

    private Map<String, Object> uploadToS3(InputStream inputStream, String fileName) {
        log.info("--------------------- " + fileName);
    
        // InputStream의 크기를 계산하기 위해 ByteArrayOutputStream을 사용
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error("Error reading input stream", e);
        }
    
        byte[] data = byteArrayOutputStream.toByteArray();
        InputStream byteArrayInputStream = new ByteArrayInputStream(data);
    
        // S3에 업로드할 ObjectMetadata 생성
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length); // Content-Length 설정
    
        // S3에 업로드
        s3Client.getAmazonS3().putObject(bucketName, fileName, byteArrayInputStream, metadata);
    
        // ACL 설정
        setAcl(s3Client.getAmazonS3(), fileName);
    
        // 업로드된 파일의 URL 생성
        String baseUploadURL = "https://balpyo-bucket.s3.ap-northeast-2.amazonaws.com/" + fileName;
    
        log.info("업로드 위치------" + baseUploadURL);
    
        // 임시 파일로 저장하여 처리
        int durationInSeconds = 0; // 초기화
    
        try {
            URL url = new URL(baseUploadURL);
            InputStream targetStream = url.openStream();
            fileName = Paths.get(url.getPath()).getFileName().toString();
            File localFile = new File(System.getProperty("java.io.tmpdir"), fileName);
    
            log.info("Download------" + localFile);
            Files.copy(targetStream, localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            targetStream.close(); // 스트림 닫기
    
            // MP3 파일의 재생 시간 계산
            MP3File mp3File = new MP3File(localFile);
            log.info("mp3 file" + mp3File);
    
            MP3AudioHeader audioHeader = (MP3AudioHeader) mp3File.getAudioHeader();
            durationInSeconds = audioHeader.getTrackLength();
    
            log.info("------------ 재생시간: " + durationInSeconds + "초");
    
            // 임시 파일 삭제
            localFile.delete();
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        // 결과를 Map에 담아 반환
        Map<String, Object> result = new HashMap<>();
        result.put("baseUploadURL", baseUploadURL);
        result.put("durationInSeconds", durationInSeconds);
        return result;
    }
    


    public void setAcl(AmazonS3 s3, String objectPath) {
        AccessControlList objectAcl = s3.getObjectAcl(bucketName, objectPath);
        objectAcl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
        s3.setObjectAcl(bucketName, objectPath, objectAcl);
    }
}