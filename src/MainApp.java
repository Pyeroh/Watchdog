import java.io.IOException;
import java.net.MalformedURLException;

import com.esotericsoftware.minlog.Log;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

	private Stage primaryStage;

	private BorderPane root;

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("Watchdog - Surveillance de fichiers");
		this.primaryStage.setResizable(false);
		
		Log.INFO();

		try {
			initMainApp();
			showWatchdog();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initMainApp() throws MalformedURLException, IOException {
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(MainApp.class.getResource("MainApp.fxml"));
		root = loader.load();

		Scene scene = new Scene(root);
		this.primaryStage.setScene(scene);
		this.primaryStage.show();
	}

	private void showWatchdog() throws IOException {
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(MainApp.class.getResource("Watchdog.fxml"));
		AnchorPane watchdog = loader.load();
		root.setCenter(watchdog);

		Watchdog controller = loader.getController();
		controller.setMainApp(this);
	}

	public static void main(String[] args) {
		launch(args);
	}

}
