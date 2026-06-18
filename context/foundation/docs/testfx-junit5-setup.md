# TestFX — setup JUnit 5 (ApplicationExtension, FxRobot, headless)

> Pelny zrzut z context7. ID: `/testfx/testfx`. Data: 2026-06-15.
> Zapytanie: "JUnit 5 ApplicationExtension Start annotation FxRobot lookup by id WaitForAsyncUtils headless Monocle setup".
> Destylacja i wnioski: `context/changes/view-navigation-shell/research.md` (pkt 7).

---

### JUnit 5 Extension with Dependency Injection

Source: https://context7.com/testfx/testfx/llms.txt

Demonstrates using JUnit 5 extensions and method parameter injection with TestFX. It sets up a JavaFX application, injects an FxRobot, and performs assertions on UI elements like buttons. Requires TestFX core and JUnit 5 extensions.

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class JUnit5ExtensionTest {

    private Button button;

    @Start
    private void start(Stage stage) {
        button = new Button("click me!");
        button.setId("myButton");
        button.setOnAction(actionEvent -> button.setText("clicked!"));
        stage.setScene(new Scene(new StackPane(button), 100, 100));
        stage.show();
    }

    @Test
    void should_contain_button_with_text(FxRobot robot) {
        // Verify using direct node reference
        FxAssert.verifyThat(button, LabeledMatchers.hasText("click me!"));

        // Verify using CSS id selector
        FxAssert.verifyThat("#myButton", LabeledMatchers.hasText("click me!"));

        // Verify using CSS class selector
        FxAssert.verifyThat(".button", LabeledMatchers.hasText("click me!"));
    }

    @Test
    void when_button_is_clicked_text_changes(FxRobot robot) {
        // Perform interaction using injected robot
        robot.clickOn(".button");

        // Verify state change
        FxAssert.verifyThat(button, LabeledMatchers.hasText("clicked!"));
        FxAssert.verifyThat("#myButton", LabeledMatchers.hasText("clicked!"));
        FxAssert.verifyThat(".button", LabeledMatchers.hasText("clicked!"));
    }
}

```

--------------------------------

### Headless Testing Configuration

Source: https://context7.com/testfx/testfx/llms.txt

Provides configuration for running TestFX tests in headless environments, suitable for CI/CD pipelines. Includes Gradle build script dependencies and GitHub Actions workflow setup. Requires TestFX core, JUnit 5, and OpenJFX Monocle.

```gradle
// build.gradle
dependencies {
    testImplementation "org.testfx:testfx-core:4.0.18"
    testImplementation "org.testfx:testfx-junit5:4.0.18"
    testImplementation "org.testfx:openjfx-monocle:jdk-11+26"
    testImplementation "org.hamcrest:hamcrest:2.1"
}

test {
    systemProperty "java.awt.headless", "true"
    systemProperty "testfx.robot", "glass"
    systemProperty "testfx.headless", "true"
    systemProperty "prism.order", "sw"
    systemProperty "prism.text", "t2k"
}

```

```yaml
# .github/workflows/test.yml
name: TestFX Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
          distribution: 'adopt'
      - name: Run tests
        run: ./gradlew test
        env:
          _JAVA_OPTIONS: "-Djava.awt.headless=true -Dtestfx.robot=glass -Dtestfx.headless=true -Dprism.order=sw"

```

--------------------------------

### Simulate User Input with FxRobot in JavaFX Tests

Source: https://context7.com/testfx/testfx/llms.txt

Demonstrates how to use FxRobot to simulate user interactions like clicking, typing, and mouse events within a JavaFX application. This snippet requires JUnit 5 and the ApplicationExtension. It takes a Stage and Scene as input and outputs simulated user actions.

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class UserInteractionTest {

    @Start
    private void start(Stage stage) {
        TextField textField = new TextField();
        textField.setId("inputField");
        Button submitButton = new Button("Submit");
        submitButton.setId("submitBtn");
        stage.setScene(new Scene(new VBox(textField, submitButton), 300, 200));
        stage.show();
    }

    @Test
    void testUserInput(FxRobot robot) {
        // Click on text field using CSS selector
        robot.clickOn("#inputField");

        // Type text with keyboard simulation
        robot.write("Hello TestFX");

        // Click button and verify interaction
        robot.clickOn("#submitBtn");

        // Move mouse to specific position
        robot.moveTo("#submitBtn").press(MouseButton.PRIMARY).release(MouseButton.PRIMARY);
    }
}
```

--------------------------------

### Maven Project Configuration for TestFX

Source: https://context7.com/testfx/testfx/llms.txt

This snippet shows the complete pom.xml configuration for a Maven project using TestFX. It includes dependencies for JavaFX controls, TestFX core, JUnit 5, Hamcrest, AssertJ, and Monocle for headless testing, targeting Java 11+.

```xml
<!-- pom.xml -->
<dependencies>
    <!-- JavaFX dependencies for Java 11+ -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>17.0.2</version>
    </dependency>

    <!-- TestFX core library -->
    <dependency>
        <groupId>org.testfx</groupId>
        <artifactId>testfx-core</artifactId>
        <version>4.0.18</version>
        <scope>test</scope>
    </dependency>

    <!-- JUnit 5 integration -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-api</artifactId>
        <version>5.8.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testfx</groupId>
        <artifactId>testfx-junit5</artifactId>
        <version>4.0.18</version>
        <scope>test</scope>
    </dependency>

    <!-- Hamcrest matchers -->
    <dependency>
        <groupId>org.hamcrest</groupId>
        <artifactId>hamcrest</artifactId>
        <version>2.2</version>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ assertions -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.22.0</version>
        <scope>test</scope>
    </dependency>

    <!-- Monocle for headless testing -->
    <dependency>
        <groupId>org.testfx</groupId>
        <artifactId>openjfx-monocle</artifactId>
        <version>jdk-11+26</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

--------------------------------

### Gradle Project Configuration for TestFX

Source: https://context7.com/testfx/testfx/llms.txt

This snippet outlines the build.gradle configuration for a Gradle project utilizing TestFX. It specifies dependencies for TestFX core, JUnit 5, Hamcrest, AssertJ, and Monocle, along with JavaFX plugin configuration for Java 11+.

```gradle
// build.gradle
plugins {
    id 'java'
    id 'org.openjfx.javafxplugin' version '0.0.12'
}

javafx {
    version = '17.0.2'
    modules = ['javafx.controls', 'javafx.fxml']
}

dependencies {
    // TestFX core
    testImplementation 'org.testfx:testfx-core:4.0.18'

    // JUnit 5 integration
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.8.2'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.2'
    testImplementation 'org.testfx:testfx-junit5:4.0.18'

    // Matchers and assertions
    testImplementation 'org.hamcrest:hamcrest:2.2'
    testImplementation 'org.assertj:assertj-core:3.22.0'

    // Headless testing support
    testImplementation 'org.testfx:openjfx-monocle:jdk-11+26'
}

test {
    useJUnitPlatform()
}
```
