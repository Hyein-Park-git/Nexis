# NexisServer/Agent

## NEXIS Monitoring Platform - Server

NEXIS 서버 프로그램입니다. 에이전트로 부터 CPU, Memory 등의 자원 사용률 데이터를 수집합니다.
<br><br>

---

## 설치 파일 다운로드
 
| OS | 링크 |
|---|---|
| Windows | [NexisServer_Setup_v1.exe](https://drive.google.com/file/d/1svs0As39z-SBRNtI2SsdqWU_o20I4b0d/view?usp=drive_link) | 49.9MB |
| Linux | [nexis-server-1.0-1.noarch.rpm](https://drive.google.com/file/d/1QTmhpOjlLXt1Eh8XtWLFq1aCMuTertFZ/view?usp=drive_link) | 47MB |
 
<br><br>

---

## 설치 방법 (Windows)
> 자세한 내용은 [티스토리](https://hailey-p.tistory.com/20) 참조

<br><br>

1. `NexisServer_Setup_v1.exe` 실행 *(관리자 권한으로 실행)*

2. 설치 경로 지정

3. 로그 경로 지정

4. DB 정보 입력

5. 시작 메뉴 폴더 지정

6. 바탕화면 바로가기 아이콘 생성 확인

7. 설치 전 선택 항목 확인

8. 설치 완료

9. 설치 경로에서 `server.properties` 확인

10. Nexis Server 실행

11. 정상 실행 시 트레이 아이콘에서 확인 가능

12. 로그 위치에서 로그 확인 가능

13. 서버 종료 시 트레이 아이콘에서 Exit

<br><br>

---

## 설치 방법 (Linux)

1. `nexis-server-1.0-1.noarch.rpm` 파일 확인

2. rpm 설치
```bash
rpm -ivh nexis-server-1.0-1.noarch.rpm
```

3. 설치 완료 시 `/etc/systemd/system/nexis-server.service` 자동 등록

4. 설치 디렉토리 `/opt/nexis-server` 내 `server.properties` 확인

5. nexis-server 실행 및 프로세스 확인
```bash
systemctl start nexis-server
systemctl status nexis-server
```

6. 설치 시 지정한 경로에서 로그 확인 가능

---
<br><br>

## NEXIS Monitoring Platform - Agent

설치된 호스트의 CPU, Memory 등의 자원 사용량 데이터를 서버로 송신하는 에이전트 프로그램입니다.
<br><br>

---

## 설치 파일 다운로드

| OS | 파일 | 크기 |
|---|---|---|
| Windows | [`NexisAgent_Setup_v1.exe`](https://drive.google.com/file/d/1uwpLPXGDY1xVOgcxXNGs-edRo4p2DKEF/view?usp=drive_link) | 11.77MB |
| Linux | [`nexis-agent-1.0-1.noarch.rpm`](https://drive.google.com/file/d/1R7kihlhjY92bqKpG3fyyh-onbnCtUh77/view?usp=drive_link) | 9.12MB |

<br><br>

---

## 설치 방법 (Windows)
>자세한 내용은 [티스토리 참조](https://hailey-p.tistory.com/19)

<br><br>

1. `NexisAgent_Setup_v1.exe` 실행 *(관리자 권한으로 실행)*

2. 설치 경로 지정

3. 로그 경로 지정

4. 서버 IP, 포트 입력 *(default: 9000)*

5. 시작 메뉴 폴더 지정

6. 바탕화면 바로가기 아이콘 생성 확인

7. 설치 전 선택 항목 확인

8. 설치 완료

9. 설치 경로에서 `agent.properties` 확인

10. Nexis Agent 실행

11. 정상 실행 시 에이전트 트레이에서 확인 가능

12. 로그 위치에서 로그 확인 가능

13. 에이전트 종료 시 트레이 아이콘에서 Exit

<br><br>

---

## 설치 방법 (Linux)

1. `nexis-agent-1.0-1.noarch.rpm` 파일 확인

2. rpm 설치
```bash
rpm -ivh nexis-agent-1.0-1.noarch.rpm
```

3. 설치 완료 시 `/etc/systemd/system/nexis-agent.service` 자동 등록

4. 설치 디렉토리 `/opt/nexis-agent` 내 `agent.properties` 확인

5. nexis-agent 실행 및 프로세스 확인
```bash
systemctl start nexis-agent
systemctl status nexis-agent
```

6. 설치 시 지정한 경로에서 로그 확인 가능
