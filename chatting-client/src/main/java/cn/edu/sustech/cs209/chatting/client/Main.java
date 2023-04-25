package cn.edu.sustech.cs209.chatting.client;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Main extends Application {

  public static void main(String[] args) {
    launch();
  }

  @Override
  public void start(Stage stage) {
    try {
      FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
      stage.setScene(new Scene(fxmlLoader.load()));
      stage.setOnHidden(event -> {
        Controller c = fxmlLoader.getController();
        c.shutdown();
        Platform.exit();
      });
      stage.setTitle("Chatting Client");
      stage.show();

    } catch (IOException e) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setContentText("Cancle");
      alert.show();
    }

  }
}
