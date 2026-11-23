# DrivenByMoss Push 2 Edit
Windows版Bitwig Studio 6.0.11、Push 2用の日本語表示・GUI改修。上流は git-moss/DrivenByMoss 26.4.0 (a192d5a)。ソースのライセンスと帰属は LICENSE および各ファイルを参照。

## ビルド
JDK 21 と Maven 3.9.x を用意し、ルートで次を実行する。
```powershell
mvn -B package
Copy-Item target/DrivenByMoss-26.4.0.jar target/DrivenByMoss-Push2-Edit.bwextension
```
GitHub ActionsのWindows buildも同じ成果物を生成する。通常利用中の拡張と同じコントローラーIDを使う。テストの場合はBitwig終了後に通常版をExtensionsの外へ退避し、試験版を1本だけ入れる。復帰時は逆の順序で戻す。

## 開発と引き継ぎ
[開発メモと実機確認項目](CLAUDE.md) を参照。画面ごとの変更はブランチとPRにまとめ、実機確認結果を記録する。
