@echo off

java -Djava.library.path="G5_server_lib" --module-path "G5_server_lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "G5_server.jar;G5_server_lib/*" gui.Server_GUI
