# JavaFX — nawigacja / FXML (FXMLLoader)

> Pełny zrzut z context7. ID: `/websites/openjfx_io_javadoc_21` (JavaFX 21 = runtime projektu). Data: 2026-06-15.
> Zapytanie: "FXMLLoader load FXML, switch scene root for view navigation, setControllerFactory".
> Destylacja i wnioski: `context/changes/view-navigation-shell/research.md` (pkt 1, 3, 4).

---

### Loading FXML with FXMLLoader and Resource Bundle

Source: https://openjfx.io/javadoc/21/javafx.fxml/javafx/fxml/doc-files/introduction_to_fxml.html

Loads an FXML file from a classpath location and localizes it with a resource bundle. Assumes the root element is a Pane and defines a controller of type MyController. Ensure the FXML file and resource bundle exist at the specified locations.

```java
URL location = getClass().getResource("example.fxml");
ResourceBundle resources = ResourceBundle.getBundle("com.foo.example");
FXMLLoader fxmlLoader = new FXMLLoader(location, resources);

Pane root = (Pane)fxmlLoader.load();
MyController controller = (MyController)fxmlLoader.getController();

```

--------------------------------

### FXMLLoader Load Methods with BuilderFactory

Source: https://openjfx.io/javadoc/21/javafx.base/javafx/util/class-use/BuilderFactory.html

Demonstrates how to load FXML content using FXMLLoader with a custom BuilderFactory.

```APIDOC
## POST FXMLLoader.load(URL location, ResourceBundle resources, BuilderFactory builderFactory)

### Description
Loads an object hierarchy from a FXML document using a specified builder factory.

### Method
POST

### Endpoint
/FXMLLoader/load

### Parameters
#### Request Body
- **location** (URL) - Required - The location of the FXML document.
- **resources** (ResourceBundle) - Optional - The resource bundle for localization.
- **builderFactory** (BuilderFactory) - Required - The builder factory to use for loading.

### Response
#### Success Response (200)
- **T** (Object) - The root object of the loaded hierarchy.

## POST FXMLLoader.load(URL location, ResourceBundle resources, BuilderFactory builderFactory, Callback<Class<?>,Object> controllerFactory)

### Description
Loads an object hierarchy from a FXML document with a custom controller factory and builder factory.

### Method
POST

### Endpoint
/FXMLLoader/load

### Parameters
#### Request Body
- **location** (URL) - Required - The location of the FXML document.
- **resources** (ResourceBundle) - Optional - The resource bundle for localization.
- **builderFactory** (BuilderFactory) - Required - The builder factory to use for loading.
- **controllerFactory** (Callback<Class<?>,Object>) - Optional - The controller factory to use.

### Response
#### Success Response (200)
- **T** (Object) - The root object of the loaded hierarchy.

## POST FXMLLoader.load(URL location, ResourceBundle resources, BuilderFactory builderFactory, Callback<Class<?>,Object> controllerFactory, Charset charset)

### Description
Loads an object hierarchy from a FXML document with specified character set, controller factory, and builder factory.

### Method
POST

### Endpoint
/FXMLLoader/load

### Parameters
#### Request Body
- **location** (URL) - Required - The location of the FXML document.
- **resources** (ResourceBundle) - Optional - The resource bundle for localization.
- **builderFactory** (BuilderFactory) - Required - The builder factory to use for loading.
- **controllerFactory** (Callback<Class<?>,Object>) - Optional - The controller factory to use.
- **charset** (Charset) - Optional - The character set to use for reading the FXML document.

### Response
#### Success Response (200)
- **T** (Object) - The root object of the loaded hierarchy.
```

--------------------------------

### Create FXMLLoader with Controller Factory

Source: https://openjfx.io/javadoc/21/javafx.base/javafx/util/class-use/Callback.html

Constructs an FXMLLoader instance with a custom controller factory. This allows for custom controller instantiation during FXML loading.

```Java
FXMLLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory, Callback<Class<?>,Object> controllerFactory)
```

```Java
FXMLLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory, Callback<Class<?>,Object> controllerFactory, Charset charset)
```

```Java
FXMLLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory, Callback<Class<?>,Object> controllerFactory, Charset charset, LinkedList<FXMLLoader> loaders)
```

--------------------------------

### Load FXML with Custom Controller Factory

Source: https://openjfx.io/javadoc/21/javafx.base/javafx/util/class-use/Callback.html

Loads an object hierarchy from FXML using a specified controller factory. Requires URL, ResourceBundle, BuilderFactory, and a Callback for controller creation.

```Java
static <T> T load(URL location, ResourceBundle resources, BuilderFactory builderFactory, Callback<Class<?>,Object> controllerFactory)
```

```Java
static <T> T load(URL location, ResourceBundle resources, BuilderFactory builderFactory, Callback<Class<?>,Object> controllerFactory, Charset charset)
```

--------------------------------

### FXMLLoader Controller

Source: https://openjfx.io/javadoc/21/index-all.html

Methods for setting the controller for an FXMLLoader.

```APIDOC
## setController(Object)

### Description
Sets the controller associated with the root object.

### Method
`public void setController(Object controller)`

### Endpoint
N/A (Method within a class)

### Class
`javafx.fxml.FXMLLoader`
```

```APIDOC
## setControllerFactory(Callback<Class<?>, Object>)

### Description
Sets the controller factory used by this loader.

### Method
`public void setControllerFactory(Callback<Class<?>, Object> controllerFactory)`

### Endpoint
N/A (Method within a class)

### Class
`javafx.fxml.FXMLLoader`
```
