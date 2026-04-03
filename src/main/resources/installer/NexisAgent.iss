; ================================
; Nexis Agent Installer
; ================================

[Setup]
AppName=Nexis Agent
AppVersion=1.0
DefaultDirName={pf}\NexisAgent
DefaultGroupName=Nexis Agent
OutputBaseFilename=NexisAgent_Setup_v1
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64
SetupIconFile=icon.ico
AllowRootDirectory=yes
AllowNoIcons=yes
DisableDirPage=no

[Files]
Source: "Nexis_Agent.exe";   DestDir: "{app}"; Flags: ignoreversion
Source: "agent.properties";  DestDir: "{app}"; Flags: ignoreversion
Source: "icon.ico";          DestDir: "{app}"; Flags: ignoreversion
Source: "icon.png";          DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Nexis Agent";       Filename: "{app}\Nexis_Agent.exe"; IconFilename: "{app}\icon.ico"
Name: "{userdesktop}\Nexis Agent"; Filename: "{app}\Nexis_Agent.exe"; IconFilename: "{app}\icon.ico"

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:"; Flags: unchecked

[Code]
var
  LogPage, ServerPage: TWizardPage;
  LogDirEdit: TEdit;
  LogBrowseBtn: TButton;
  ServerHostEdit, ServerPortEdit: TEdit;
  L: TLabel;
  G_LogDir:     String;
  G_ServerHost: String;
  G_ServerPort: String;

// -------------------- 백슬래시 → 슬래시 변환 --------------------
function ToSlash(const S: String): String;
var
  i: Integer;
  R: String;
begin
  R := '';
  for i := 1 to Length(S) do
  begin
    if S[i] = '\' then
      R := R + '/'
    else
      R := R + S[i];
  end;
  Result := R;
end;

// -------------------- 주석이 아닌 라인만 값 교체 --------------------
procedure SetPropertyValue(Lines: TStringList; const Name, Value: String);
var i: Integer; Found: Boolean; Line: String;
begin
  Found := False;
  for i := 0 to Lines.Count - 1 do
  begin
    Line := Trim(Lines[i]);
    if (Length(Line) > 0) and (Line[1] <> '#') then
      if Pos(Name + '=', Lines[i]) = 1 then
      begin
        Lines[i] := Name + '=' + Value;
        Found := True;
        Break;
      end;
  end;
  if not Found then Lines.Add(Name + '=' + Value);
end;

// -------------------- 로그 폴더 찾아보기 --------------------
procedure LogBrowseBtnClick(Sender: TObject);
var Dir: String;
begin
  Dir := LogDirEdit.Text;
  if BrowseForFolder('Select log folder', Dir, False) then
    LogDirEdit.Text := Dir;
end;

// -------------------- 설치 페이지 초기화 --------------------
procedure InitializeWizard();
begin
  // 로그 폴더 선택 페이지
  LogPage := CreateCustomPage(wpSelectDir, 'Select Log Folder', 'Choose folder for agent logs');

  L := TLabel.Create(WizardForm);
  L.Parent := LogPage.Surface;
  L.Left := 10; L.Top := 13; L.Caption := 'Log folder:';

  LogDirEdit := TEdit.Create(WizardForm);
  LogDirEdit.Parent := LogPage.Surface;
  LogDirEdit.Left := 90; LogDirEdit.Top := 10; LogDirEdit.Width := 280;
  LogDirEdit.Text := ExpandConstant('{commonappdata}\NexisAgent\logs');

  LogBrowseBtn := TButton.Create(WizardForm);
  LogBrowseBtn.Parent := LogPage.Surface;
  LogBrowseBtn.Left := 378; LogBrowseBtn.Top := 8; LogBrowseBtn.Width := 75;
  LogBrowseBtn.Caption := 'Browse...';
  LogBrowseBtn.OnClick := @LogBrowseBtnClick;

  // 서버 IP/Port 설정 페이지
  ServerPage := CreateCustomPage(LogPage.ID, 'Server Connection', 'Enter Nexis Server IP and port');

  L := TLabel.Create(WizardForm);
  L.Parent := ServerPage.Surface; L.Left := 10; L.Top := 13; L.Caption := 'Server IP:';

  ServerHostEdit := TEdit.Create(WizardForm);
  ServerHostEdit.Parent := ServerPage.Surface;
  ServerHostEdit.Left := 120; ServerHostEdit.Top := 10; ServerHostEdit.Width := 200;
  ServerHostEdit.Text := '127.0.0.1';

  L := TLabel.Create(WizardForm);
  L.Parent := ServerPage.Surface; L.Left := 10; L.Top := 43; L.Caption := 'Server Port:';

  ServerPortEdit := TEdit.Create(WizardForm);
  ServerPortEdit.Parent := ServerPage.Surface;
  ServerPortEdit.Left := 120; ServerPortEdit.Top := 40; ServerPortEdit.Width := 80;
  ServerPortEdit.Text := '9000';
end;

// -------------------- 다음 버튼: 입력값 검증 + 전역변수 저장 --------------------
function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;

  // 로그 폴더 검증
  if CurPageID = LogPage.ID then
  begin
    if LogDirEdit.Text = '' then
      LogDirEdit.Text := ExpandConstant('{commonappdata}\NexisAgent\logs');

    G_LogDir := LogDirEdit.Text;

    if not DirExists(G_LogDir) then
      if not ForceDirectories(G_LogDir) then
      begin
        MsgBox('Cannot create log folder: ' + G_LogDir + #13#10 +
               'Please choose a folder outside of Program Files.',
               mbError, MB_OK);
        Result := False;
        Exit;
      end;

    if not SaveStringToFile(G_LogDir + '\.write_test', '', False) then
    begin
      MsgBox('No write permission for: ' + G_LogDir + #13#10 +
             'Please choose a different folder.',
             mbError, MB_OK);
      Result := False;
      Exit;
    end;
    DeleteFile(G_LogDir + '\.write_test');
  end;

  // 서버 접속 정보 검증
  if CurPageID = ServerPage.ID then
  begin
    if ServerHostEdit.Text = '' then
    begin
      MsgBox('Please enter Server IP.', mbError, MB_OK);
      Result := False;
      Exit;
    end;

    if ServerPortEdit.Text = '' then
    begin
      MsgBox('Please enter Server Port.', mbError, MB_OK);
      Result := False;
      Exit;
    end;

    G_ServerHost := ServerHostEdit.Text;
    G_ServerPort := ServerPortEdit.Text;
  end;
end;

// -------------------- 설치 완료 후 agent.properties 덮어쓰기 --------------------
procedure CurStepChanged(CurStep: TSetupStep);
var
  Lines: TStringList;
  PropFile, AppDir: String;
begin
  if CurStep = ssPostInstall then
  begin
    AppDir   := ExpandConstant('{app}');
    PropFile := AppDir + '\agent.properties';

    Lines := TStringList.Create;
    try
      if FileExists(PropFile) then Lines.LoadFromFile(PropFile);

      // 서버 접속 정보 저장
      SetPropertyValue(Lines, 'server.host', G_ServerHost);
      SetPropertyValue(Lines, 'server.port', G_ServerPort);
      // 로그 경로 - 슬래시로 변환하여 저장 (Java Properties 백슬래시 이스케이프 문제 방지)
      SetPropertyValue(Lines, 'log.dir', ToSlash(G_LogDir));

      Lines.SaveToFile(PropFile);
    finally
      Lines.Free;
    end;
  end;
end;