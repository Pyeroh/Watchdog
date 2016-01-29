
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.wildcard.Paths;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;

public class Watchdog {

  @FXML
  private TextField pathToWatch;

  @FXML
  private TextField pattern;

  @FXML
  private Label labelToOperate;

  @FXML
  private TextField pathToOperate;

  @FXML
  private ComboBox<FileOperations> operation;

  @FXML
  private Button surveillance;

  @SuppressWarnings("unused")
  private MainApp mainApp;

  private static final String DEMARRER = "Démarrer la surveillance";
  private static final String ARRETER = "Arrêter la surveillance";

  private boolean watchdogStarted = false;

  private WatchThread thread;

  @FXML
  private void initialize() {

  }

  public void setMainApp(MainApp mainApp) {
    this.mainApp = mainApp;

    operation.setItems(FXCollections.observableArrayList(Arrays.asList(FileOperations.values())));
    operation.setCellFactory(listView -> new FileOperationsCell());
    updateToOperateVisibility();
  }

  @FXML
  private void updateToOperateVisibility() {
    boolean vis = operation.getSelectionModel().getSelectedItem() != FileOperations.DELETE;
    labelToOperate.setVisible(vis);
    pathToOperate.setVisible(vis);
  }

  @FXML
  private void manageWatchdog() {
    watchdogStarted = !watchdogStarted;

    if (watchdogStarted) {
      if (pathToWatch.getText().isEmpty() || pattern.getText().isEmpty()
          || (operation.getSelectionModel().isEmpty() || (operation.getSelectionModel().getSelectedItem() != FileOperations.DELETE && pathToOperate.getText().isEmpty()))) {
        watchdogStarted = !watchdogStarted;
        return;
      }

      surveillance.setText(ARRETER);
      pathToWatch.setDisable(true);
      pattern.setDisable(true);
      pathToOperate.setDisable(true);
      operation.setDisable(true);

      Log.info("Démarrage de la surveillance");
      thread = new WatchThread(pathToWatch.getText(), pattern.getText(), operation.getSelectionModel().getSelectedItem(), java.nio.file.Paths.get(pathToOperate.getText()));
      thread.start();
    } else {
      surveillance.setText(DEMARRER);
      pathToWatch.setDisable(false);
      pattern.setDisable(false);
      pathToOperate.setDisable(false);
      operation.setDisable(false);

      Log.info("Arrêt de la surveillance");
      thread.interrupt();
    }
  }

  private class FileOperationsCell extends ListCell<FileOperations> {
    @Override
    protected void updateItem(FileOperations item, boolean empty) {
      super.updateItem(item, empty);

      if (empty) {
        setText(null);
      } else {
        setText(item == null ? "null" : item.getLabel());
      }
      setGraphic(null);
    }
  }

  private class WatchThread extends Thread {

    private Paths paths;

    private FileOperations operation;

    private Path destination;

    public WatchThread(String path, String pattern, FileOperations operation, Path destination) {
      this.operation = operation;
      paths = new Paths();
      paths.glob(path, pattern);
      this.destination = destination;
    }

    @Override
    public void run() {
      Set<Path> pathsToWatch = updatePathsToWatch();

      Map<Path, WatchService> registeredWatchServices = new LinkedHashMap<>();
      ExecutorService executorService = Executors.newCachedThreadPool();
      try {
        while (!Thread.interrupted()) {
          updateRegisteredWatchers(pathsToWatch, registeredWatchServices);
          for (WatchService watchService : registeredWatchServices.values()) {
            executorService.execute(() -> {
              WatchKey watchKey = watchService.poll();

              if (watchKey != null) {
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                  WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                  Path filePath = paths.getFiles().stream().filter(f -> f.getName().equals(pathEvent.context().toString())).findFirst().get().toPath();

                  if (pathEvent.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                    try {
                      Path newPath = null;
                      if (operation == FileOperations.COPY) {
                        newPath = Files.copy(filePath, destination.resolve(filePath.toFile().getName()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                      } else if (operation == FileOperations.DELETE) {
                        Files.delete(filePath);
                      } else if (operation == FileOperations.MOVE) {
                        newPath = Files.move(filePath, destination.resolve(filePath.toFile().getName()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                      }
                      if (newPath == null && operation == FileOperations.DELETE) {
                        Log.info(FileOperations.DELETE.getLabel() + " : " + filePath);
                      } else if (newPath != null) {
                        Log.info(operation.getLabel() + " : " + filePath);
                      }
                    } catch (IOException e) {
                      Log.error("Oups", e);
                    }
                  }
                }

                watchKey.reset();
              }
            });
          }
          pathsToWatch.addAll(updatePathsToWatch());
        }
        executorService.shutdown();
      } catch (IOException e) {
        try {
          for (WatchService watchService : registeredWatchServices.values()) {
            watchService.close();
          }
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }

    private void updateRegisteredWatchers(Set<Path> pathsToWatch, Map<Path, WatchService> registeredWatchServices) throws IOException {
      for (Path path : pathsToWatch) {
        if (!registeredWatchServices.containsKey(path)) {
          WatchService watchService = path.getFileSystem().newWatchService();
          path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
          registeredWatchServices.put(path, watchService);
        }
      }
    }

    private TreeSet<Path> updatePathsToWatch() {
      return paths.getFiles().stream().map(File::getParentFile).map(File::toPath).collect(Collectors.toCollection(TreeSet::new));
    }
  }

}
