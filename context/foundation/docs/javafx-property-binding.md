# JavaFX — Property / binding (baza ViewModelu)

> Pełny zrzut z context7. ID: `/websites/openjfx_io_javadoc_21` (JavaFX 21 = runtime projektu). Data: 2026-06-15.
> Zapytanie: "Property SimpleStringProperty bind bindBidirectional ObservableValue for ViewModel".
> Destylacja i wnioski: `context/changes/view-navigation-shell/research.md` (pkt 2).

---

### bindBidirectional (Property<String>, Property<?>, Format)

Source: https://openjfx.io/javadoc/21/javafx.base/javafx/beans/binding/Bindings.html

Creates a bidirectional binding between a StringProperty and another Property, using a Format for conversion.

```APIDOC
## bindBidirectional

### Description
Generates a bidirectional binding (or "bind with inverse") between a `String`-`Property` and another `Property` using the specified `Format` for conversion.
A bidirectional binding is a binding that works in both directions. If two properties `a` and `b` are linked with a bidirectional binding and the value of `a` changes, `b` is set to the same value automatically. And vice versa, if `b` changes, `a` is set to the same value.
A bidirectional binding can be removed with `unbindBidirectional(Object, Object)`.
Note: this implementation of a bidirectional binding behaves differently from all other bindings here in two important aspects. A property that is linked to another property with a bidirectional binding can still be set (usually bindings would throw an exception). Secondly bidirectional bindings are calculated eagerly, i.e. a bound property is updated immediately.

### Method
`public static void bindBidirectional(Property<String> stringProperty, Property<?> otherProperty, Format format)`

### Parameters
#### Path Parameters
- **stringProperty** (Property<String>) - Required - 
- **otherProperty** (Property<?>) - Required - 
- **format** (Format) - Required - 
```

--------------------------------

### Create Bidirectional Binding with String Conversion

Source: https://openjfx.io/javadoc/21/javafx.base/javafx/beans/binding/Bindings.html

Creates a bidirectional binding between a StringProperty and another Property, using a specified Format for value conversion. Similar to the generic bindBidirectional, but includes format conversion.

```java
public static void bindBidirectional(Property<String> stringProperty, Property<?> otherProperty, Format format)
```

--------------------------------

### Create Bidirectional Binding with Property

Source: https://openjfx.io/javadoc/21/javafx.base/javafx/beans/property/StringProperty.html

Establishes a bidirectional binding between this StringProperty and another Property. Weak listeners are used, meaning properties can be garbage collected. Multiple bidirectional bindings are supported but discouraged.

```java
public void bindBidirectional(Property<String> other)
Create a bidirectional binding between this `Property` and another one. Bidirectional bindings exists independently of unidirectional bindings. So it is possible to add unidirectional binding to a property with bidirectional binding and vice-versa. However, this practice is discouraged. 
It is possible to have multiple bidirectional bindings of one Property. 
JavaFX bidirectional binding implementation use weak listeners. This means bidirectional binding does not prevent properties from being garbage collected. 
```

--------------------------------

### Create Bidirectional Binding between Properties

Source: https://openjfx.io/javadoc/21/javafx.base/javafx/beans/binding/Bindings.html

Establishes a two-way binding between two properties. Changes in one property are automatically reflected in the other. This binding is eager and allows properties to be set directly.

```java
public static <T> void bindBidirectional(Property<T> property1, Property<T> property2)
```

--------------------------------

### IntegerProperty bindBidirectional Method

Source: https://openjfx.io/javadoc/21/javafx.base/javafx/beans/property/IntegerProperty.html

Explains how to establish a bidirectional binding between two properties.

```APIDOC
### bindBidirectional
```java
public void bindBidirectional(Property<Number> other)
```
Create a bidirectional binding between this `Property` and another one. Bidirectional bindings exists independently of unidirectional bindings. So it is possible to add unidirectional binding to a property with bidirectional binding and vice-versa. It is possible to have multiple bidirectional bindings of one Property. JavaFX bidirectional binding implementation use weak listeners. This means bidirectional binding does not prevent properties from being garbage collected.

Parameters:
- `other` (Property<Number>) - the other `Property`
```
