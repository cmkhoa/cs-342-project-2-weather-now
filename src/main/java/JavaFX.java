import ui.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application entry point.
 * Creates a Navigator (which owns all scene transitions) and hands it the Stage.
 */
public class JavaFX extends Application {

	public static void main(String[] args) { launch(args); }

	@Override
	public void start(Stage stage) {
		stage.setTitle("CS 342 Project 2: Weather App");
		stage.setResizable(false);
		new Navigator(stage).init();   // fetches Chicago data + shows Scene 1
	}
}