
import java.util.Arrays;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class Watchdog {

	@FXML
	private TextField pathToWatch;

	@FXML
	private Label labelToOperate;

	@FXML
	private TextField pathToOperate;

	@FXML
	private ComboBox<FileOperations> operation;

	private MainApp mainApp;

	@FXML
	private void initialize() {
		
	}

	public void setMainApp(MainApp mainApp) {
		this.mainApp = mainApp;
		
		operation.setItems(FXCollections.observableArrayList(Arrays.asList(FileOperations.values())));
		updateToOperateVisibility();
	}

	@FXML
	private void updateToOperateVisibility() {
		boolean vis = operation.getSelectionModel().getSelectedItem() != FileOperations.DELETE;
		labelToOperate.setVisible(vis);
		pathToOperate.setVisible(vis);
	}

}
