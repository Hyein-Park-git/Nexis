# NexisServer

**NEXIS Monitoring Platform - Server**
<br><br>

---

## 설치 파일 다운로드
 
| OS | 링크 |
|---|---|
| Windows | [다운로드](https://drive.google.com/file/d/1kyUYE_nKScJxLZoUGiVUjbbCBMREHt7x/view?usp=drive_link) |
| Linux | [다운로드](https://drive.google.com/file/d/1eOw2V505TxieqwWi-npolaVeRTFnb6bn/view?usp=drive_link) |
 
<br><br>

## 설치 방법 (Windows)
> 자세한 내용은 [티스토리 참조](https://hailey-p.tistory.com/20)


1. `NexisServer_Setup_v1.exe` 실행 *(관리자 권한으로 실행)*

2. 설치 경로 지정

3. 로그 경로 지정

4. DB 정보 입력

5. 시작 메뉴 폴더 지정

6. 바탕화면 바로가기 아이콘 생성 확인

7. 설치 전 선택 항목 확인

8. 설치 진행 화면

9. 설치 완료 화면

10. 설치 경로에서 `server.properties` 확인

11. Nexis Server 실행

12. 정상 실행 시 트레이 아이콘에서 확인 가능

13. 로그 위치에서 로그 확인 가능

14. 서버 종료 시 트레이 아이콘에서 Exit

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
