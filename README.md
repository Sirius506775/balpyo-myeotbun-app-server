# 발표몇분 백엔드

## 커밋 메세지 컨벤션

```
Feat :: 새로운 기능 추가
Fix :: 버그 수정
Docs :: 문서 수정
Style :: 코드 포맷팅, 세미콜론 누락, 코드 변경이 없는 경우
Refactor :: 코드 리펙토링
Test :: 테스트 코드, 리펙토링 테스트 코드 추가
Chore :: 빌드 업무 수정, 패키지 매니저 수정
Design :: CSS 등 사용자 UI 디자인 변경
Comment :: 필요한 주석 추가 및 변경
Rename :: 파일 혹은 폴더명을 수정하거나 옮기는 작업만인 경우
Remove :: 파일을 삭제하는 작업만 수행한 경우
!BREAKING CHANGE :: 커다란 API 변경의 경우
!HOTFIX :: 급하게 치명적인 버그를 고쳐야 하는 경우
EX) Chore :: 패키지 수정
```


## git hook setting

### Step1
```
cd .git/hooks
```

### Step2
```
Set-Content commit-msg -Value @'
#!/bin/sh
# Read the commit message from the file.
COMMIT_MSG=$(cat $1)
# Check if the commit message follows the specified pattern.
if ! echo "$COMMIT_MSG" | grep -E "^(Feat|Fix|Docs|Style|Refactor|Test|Chore|Design|Comment|Rename|Remove|!BREAKING CHANGE|!HOTFIX) ::" > /dev/null; then
    echo "The commit message does not follow the specified format."
    echo "Allowed formats: Feat ::, Fix ::, Docs ::, Style ::, Refactor ::, Test ::, Chore ::, Design ::, Comment ::, Rename ::, Remove ::, !BREAKING CHANGE ::, !HOTFIX ::"
    exit 1
fi
'@
```


