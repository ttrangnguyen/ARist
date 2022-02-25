# FluteSC
## How to use parser
Auto load config from JSON (load source and *.jar path)
> Config.loadConfig(Config.STORAGE_DIR + "/json/project.json");

Create project parser for a project

> ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH, Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

Create file parser with position of method call
> FileParser fileParser =  new  FileParser(projectParser, file, line, height);

Generate excode and lexical candidates
> fileParser.parse(); 
> MultiMap result = fileParser.genParamsAt(position);
