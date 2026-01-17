@echo off

java -Djava.library.path="G5_client_lib" --module-path "G5_client_lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "G5_client.jar;G5_client_lib/*" gui.Client_GUI
