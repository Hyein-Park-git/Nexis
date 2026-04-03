; ================================
; Nexis Server Installer
; ================================

[Setup]
AppName=Nexis Server
AppVersion=1.0
DefaultDirName={commonpf}\NexisServer
DefaultGroupName=Nexis Server
OutputBaseFilename=NexisServer_Setup_v1
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64compatible
SetupIconFile=icon.ico
AllowRootDirectory=yes
AllowNoIcons=yes
DisableDirPage=no

[Files]
Source: "Nexis_Server.exe";   DestDir: "{app}"; Flags: ignoreversion
Source: "server.properties";  DestDir: "{app}"; Flags: ignoreversion
Source: "icon.ico";           DestDir: "{app}"; Flags: ignoreversion
Source: "icon.png";           DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Nexis Server";       Filename: "{app}\Nexis_Server.exe"; IconFilename: "{app}\icon.ico"
Name: "{userdesktop}\Nexis Server"; Filename: "{app}\Nexis_Server.exe"; IconFilename: "{app}\icon.ico"

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:"; Flags: unchecked

[Code]
var
  LogPage, ServerPage, DbPage: TWizardPage;
  LogDirEdit, ServerHostEdit, ServerPortEdit: TEdit;
  LogBrowseBtn: TButton;
  DbTypeCombo: TComboBox;
  DbHostEdit, DbPortEdit, DbNameEdit, DbUserEdit, DbPassEdit: TEdit;
  L: TLabel;

  G_LogDir: String;
  G_ServerHost: String;
  G_ServerPort: String;
  G_DbType: String;
  G_DbHost: String;
  G_DbPort: String;
  G_DbName: String;
  G_DbUser: String;
  G_DbPass: String;

function ToSlash(const S: String): String;
var i: Integer; R: String;
begin
  R := '';
  for i := 1 to Length(S) do
    if S[i] = '\' then R := R + '/' else R := R + S[i];
  Result := R;
end;

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

procedure DbTypeComboChange(Sender: TObject);
begin
  case DbTypeCombo.ItemIndex of
    0: begin
         DbHostEdit.Text := '127.0.0.1';
         DbPortEdit.Text := '3306';
         DbHostEdit.Enabled := True;
         DbPortEdit.Enabled := True;
         DbNameEdit.Enabled := True;
       end;
    1: begin
         DbHostEdit.Text := '127.0.0.1';
         DbPortEdit.Text := '3306';
         DbHostEdit.Enabled := True;
         DbPortEdit.Enabled := True;
         DbNameEdit.Enabled := True;
       end;
    2: begin
         DbHostEdit.Text := '127.0.0.1';
         DbPortEdit.Text := '5432';
         DbHostEdit.Enabled := True;
         DbPortEdit.Enabled := True;
         DbNameEdit.Enabled := True;
       end;
  end;
end;

procedure LogBrowseBtnClick(Sender: TObject);
var Dir: String;
begin
  Dir := LogDirEdit.Text;
  if BrowseForFolder('Select log folder', Dir, False) then
    LogDirEdit.Text := Dir;
end;

procedure InitializeWizard();
begin
  // ── 로그 페이지 ──
  LogPage := CreateCustomPage(wpSelectDir, 'Select Log Folder', 'Choose folder for server logs');

  L := TLabel.Create(WizardForm);
  L.Parent := LogPage.Surface;
  L.Left := 10; L.Top := 13; L.Caption := 'Log folder:';

  LogDirEdit := TEdit.Create(WizardForm);
  LogDirEdit.Parent := LogPage.Surface;
  LogDirEdit.Left := 90; LogDirEdit.Top := 10; LogDirEdit.Width := 280;
  LogDirEdit.Text := ExpandConstant('{commonappdata}\NexisServer\logs');

  LogBrowseBtn := TButton.Create(WizardForm);
  LogBrowseBtn.Parent := LogPage.Surface;
  LogBrowseBtn.Left := 378; LogBrowseBtn.Top := 8; LogBrowseBtn.Width := 75;
  LogBrowseBtn.Caption := 'Browse...';
  LogBrowseBtn.OnClick := @LogBrowseBtnClick;

  // ── 서버 페이지 ──
  ServerPage := CreateCustomPage(LogPage.ID, 'Server Configuration', 'Set the server host and port');

  L := TLabel.Create(WizardForm);
  L.Parent := ServerPage.Surface; L.Left := 10; L.Top := 13; L.Caption := 'Server Host:';

  ServerHostEdit := TEdit.Create(WizardForm);
  ServerHostEdit.Parent := ServerPage.Surface;
  ServerHostEdit.Left := 120; ServerHostEdit.Top := 10; ServerHostEdit.Width := 200;
  ServerHostEdit.Text := '0.0.0.0';

  L := TLabel.Create(WizardForm);
  L.Parent := ServerPage.Surface; L.Left := 10; L.Top := 43; L.Caption := 'Server Port:';

  ServerPortEdit := TEdit.Create(WizardForm);
  ServerPortEdit.Parent := ServerPage.Surface;
  ServerPortEdit.Left := 120; ServerPortEdit.Top := 40; ServerPortEdit.Width := 80;
  ServerPortEdit.Text := '9000';

  // ── DB 페이지 ──
  DbPage := CreateCustomPage(ServerPage.ID, 'Database Configuration', 'Configure database connection');

  L := TLabel.Create(WizardForm);
  L.Parent := DbPage.Surface; L.Left := 10; L.Top := 13; L.Caption := 'DB Type:';

  DbTypeCombo := TComboBox.Create(WizardForm);
  DbTypeCombo.Parent := DbPage.Surface;
  DbTypeCombo.Left := 120; DbTypeCombo.Top := 10; DbTypeCombo.Width := 150;
  DbTypeCombo.Style := csDropDownList;
  DbTypeCombo.Items.Add('MySQL');
  DbTypeCombo.Items.Add('MariaDB');
  DbTypeCombo.Items.Add('PostgreSQL');
  DbTypeCombo.ItemIndex := 0;
  DbTypeCombo.OnChange := @DbTypeComboChange;

  L := TLabel.Create(WizardForm);
  L.Parent := DbPage.Surface; L.Left := 10; L.Top := 43; L.Caption := 'DB Host:';

  DbHostEdit := TEdit.Create(WizardForm);
  DbHostEdit.Parent := DbPage.Surface;
  DbHostEdit.Left := 120; DbHostEdit.Top := 40; DbHostEdit.Width := 200;
  DbHostEdit.Text := '127.0.0.1';
  DbHostEdit.Enabled := True;

  L := TLabel.Create(WizardForm);
  L.Parent := DbPage.Surface; L.Left := 10; L.Top := 73; L.Caption := 'DB Port:';

  DbPortEdit := TEdit.Create(WizardForm);
  DbPortEdit.Parent := DbPage.Surface;
  DbPortEdit.Left := 120; DbPortEdit.Top := 70; DbPortEdit.Width := 80;
  DbPortEdit.Text := '3306';
  DbPortEdit.Enabled := True;

  L := TLabel.Create(WizardForm);
  L.Parent := DbPage.Surface; L.Left := 10; L.Top := 103; L.Caption := 'DB Name:';

  DbNameEdit := TEdit.Create(WizardForm);
  DbNameEdit.Parent := DbPage.Surface;
  DbNameEdit.Left := 120; DbNameEdit.Top := 100; DbNameEdit.Width := 200;
  DbNameEdit.Text := 'nexis';
  DbNameEdit.Enabled := True;

  L := TLabel.Create(WizardForm);
  L.Parent := DbPage.Surface; L.Left := 10; L.Top := 133; L.Caption := 'Username:';

  DbUserEdit := TEdit.Create(WizardForm);
  DbUserEdit.Parent := DbPage.Surface;
  DbUserEdit.Left := 120; DbUserEdit.Top := 130; DbUserEdit.Width := 200;
  DbUserEdit.Text := '';

  L := TLabel.Create(WizardForm);
  L.Parent := DbPage.Surface; L.Left := 10; L.Top := 163; L.Caption := 'Password:';

  DbPassEdit := TEdit.Create(WizardForm);
  DbPassEdit.Parent := DbPage.Surface;
  DbPassEdit.Left := 120; DbPassEdit.Top := 160; DbPassEdit.Width := 200;
  DbPassEdit.PasswordChar := '*';
  DbPassEdit.Text := '';
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;

  if CurPageID = LogPage.ID then
  begin
    if LogDirEdit.Text = '' then LogDirEdit.Text := ExpandConstant('{commonappdata}\NexisServer\logs');
    G_LogDir := LogDirEdit.Text;
    if not DirExists(G_LogDir) then ForceDirectories(G_LogDir);
  end;

  if CurPageID = ServerPage.ID then
  begin
    if ServerHostEdit.Text = '' then
    begin
      MsgBox('Please enter Server Host.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if ServerPortEdit.Text = '' then
    begin
      MsgBox('Please enter Server Port.', mbError, MB_OK);
      Result := False; Exit;
    end;
    G_ServerHost := ServerHostEdit.Text;
    G_ServerPort := ServerPortEdit.Text;
  end;

  if CurPageID = DbPage.ID then
  begin
    if DbHostEdit.Text = '' then
    begin
      MsgBox('Please enter DB Host.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if DbPortEdit.Text = '' then
    begin
      MsgBox('Please enter DB Port.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if DbNameEdit.Text = '' then
    begin
      MsgBox('Please enter DB Name.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if DbUserEdit.Text = '' then
    begin
      MsgBox('Please enter DB Username.', mbError, MB_OK);
      Result := False; Exit;
    end;

    G_DbType := DbTypeCombo.Text;
    G_DbHost := DbHostEdit.Text;
    G_DbPort := DbPortEdit.Text;
    G_DbName := DbNameEdit.Text;
    G_DbUser := DbUserEdit.Text;
    G_DbPass := DbPassEdit.Text;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  Lines: TStringList;
  PropFile, AppDir: String;
begin
  if CurStep = ssPostInstall then
  begin
    AppDir   := ExpandConstant('{app}');
    PropFile := AppDir + '\server.properties';

    Lines := TStringList.Create;
    try
      if FileExists(PropFile) then Lines.LoadFromFile(PropFile);

      SetPropertyValue(Lines, 'log.dir',      ToSlash(G_LogDir));
      SetPropertyValue(Lines, 'server.host',  G_ServerHost);
      SetPropertyValue(Lines, 'server.port',  G_ServerPort);
      SetPropertyValue(Lines, 'db.type',      G_DbType);
      SetPropertyValue(Lines, 'db.host',      G_DbHost);
      SetPropertyValue(Lines, 'db.port',      G_DbPort);
      SetPropertyValue(Lines, 'db.name',      G_DbName);
      SetPropertyValue(Lines, 'db.username',  G_DbUser);
      SetPropertyValue(Lines, 'db.password',  G_DbPass);

      Lines.SaveToFile(PropFile);
    finally
      Lines.Free;
    end;
  end;
end;