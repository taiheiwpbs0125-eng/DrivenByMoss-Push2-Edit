# 開発引き継ぎ
対象はWindows上のPush 2。操作変更や新しいフィルター機能は後回し。現在のAdd Deviceはユーザーが実機で評価済みで、デザインの基準とする。背景は暖色系の暗色、選択はオレンジ、日本語表示、上下のボタン・ノブ位置とラベルを一致させる。音色の表示を検証した日本語版はユーザーが引き続き利用する。自動インストールは行わない。

## 主な変更箇所
- OrangeBrowser はAdd Device専用の8列×6行、17pxの上下帯と選択名を描く。入力は既存のDeviceBrowserMode。
- 日本語はJapaneseLabels、Device ModeはDeviceTileとDeviceTileSurface。Bitwigの画像確保は初期化中のみ可能。
- AddTrackMode、NoteViewSelectMode、SessionViewSelectMode、ScaleLayoutModeはOptionsComponentとLabelComponentを利用する。ScalesModeはListComponentも使う。
- NoteModeのイベント編集はParameterComponent、SessionModeのクリップ・シーンはClipListComponentとSceneListGridElement。再生、録音、選択を区別する表示を残す。
- usesCompactOrangeDisplay はPush 2だけで有効。他機種の既定値はfalse。入出力、USB、モード遷移はこのGUI差分の対象外。

## 実機確認
1. Add Trackで上下の対応、トラック種類の色、長い候補名を確認。
2. Scaleでスケール一覧・ルート・In Keyを切り替え、選択強調を確認。
3. Noteのビュー選択、ノート未選択、ステップ選択後のパラメーター表示を確認。
4. Sessionのビュー選択と、Clips・Scenes表示の選択、再生、録音・停止記号を確認。
5. Add Deviceの概要・フィルター・候補を再確認。Deviceの「音色」、バンク切替、Mixとの往復を確認。
日本語版のDevice操作は実機確認済み。今回追加したGUIは実機確認待ち。表示の指摘は使用モード、操作、期待値、実際の画面をissueに記録する。
