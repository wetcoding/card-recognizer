@echo off
set /P Directory="Enter directory with images: "


java -jar CardRecognizer-1.0-SNAPSHOT.jar %Directory%