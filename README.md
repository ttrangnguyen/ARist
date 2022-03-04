# FluteSC
## How to use the parser to generate candidate expressions
### Configuration
All configuration storage here: `flute.config.Config`
| Constant | Description  |
|--|--|
| MVN_HOME | Maven home folder |
| JAVAFX_DIR | JavaFX folder include all JavaFX *.jar files |
| TARGET_PARAM_POSITION | Use current line and column as param position, reference to `FileParser.genCurParams()` |
| FEATURE_PARAM_* | Enable that expression type to predict |

### Predict phase

Auto load config from JSON (load source and `*.jar` path)
```java
Config.loadConfig(Config.STORAGE_DIR + "/json/project.json");
```
or
```java
Config.autoConfigure(projectName, projectDir);
```

Create `project parser` for a project
```java
ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH, Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
```

Create `file parser` with position of method call
```java
FileParser fileParser =  new  FileParser(projectParser, file, line, column);
```

Generate excode and lexical candidates
```java
fileParser.parse(); 
MultiMap result = fileParser.genParamsAt(position);
```
