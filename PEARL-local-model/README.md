# PEARL

## Model

### Command Line API

- `--train`/`--test`/`--evaluate`: Select a mode to run.
- `--cuglm`/`--parc`: Select a dataset to run. `--parc` dataset includes `eclipse` and `netbeans`.
- `--dynamic`/`--static`: Select dynamic/static model.
- `--tokenized`: Either choose `fulltoken` or `subtoken`.
- `--predict-type`: Either choose `sequence` or `firsttoken`.
- `--maintenance`: Select to run maintenance test.
- `--beam-search`: Use beam-search to generate candidates instead of program analysis.
- `--project`: If selected `--parc`, you **must** choose either `netbeans` or `eclipse`.
- `--fold`: If selected `--parc`, you **must** select a number in range [0,9]. This will be the test fold.
- `--solution-name`: If selected `--evaluate` mode, please choose `flute`.
- `--eval-parc`: If selected `--parc`, you may turn this on to evaluate libraries in the PARC paper.
- `--cuglm-id`: If selected `cuglm` and `dynamic` and `beam-search` options, you may choose a number in range [1,400]. The number is one of the 400 projects in cuglm dataset to test.
- `--last-parc-fold`: Used in sensibility experiment. If selected `--parc`, you may select a number in range [0,8]. Let's call the number as x. Folds [0,x] will be test data. Folds [x+1,9] will be test data.
- `--ngram-global`: If selected, use a simple ngram model instead of nested cache.

### Config files
- `src/main/java/flute/config/`: Config files location. You can change paths, model options and test options.

### Build jar file
- Run maven to build project jar file: `mvn clean compile assembly:single`. The entrypoint is `src/main/java/flute/Main.java`

### Run examples
Assuming the built jar file is named PEARL.jar

- Train netbeans nested cache, fulltoken model with fold 1 to 9 (fold 0 is the test fold): `java -jar PEARL.jar --train --project netbeans --fold 0 --parc --dynamic --tokenized fulltoken`
- Test netbeans fold 0, using the above trained model: `java -jar PEARL.jar --test --project netbeans --fold 0 --predict-type sequence --parc --dynamic --tokenized fulltoken`
- Evaluate netbeans fold 0, using the above trained model: `java -jar PEARL.jar --test --project netbeans --fold 0 --predict-type sequence --parc --dynamic --tokenized fulltoken`

- Train cuglm nested cache, fulltoken model: `java -jar PEARL.jar --train --cuglm --dynamic --tokenized fulltoken`
- Test the cuglm model: `java -jar PEARL.jar --test --cuglm --predict-type sequence --dynamic --tokenized fulltoken`
- Evaluate the cuglm model: `java -jar SLP-Modified.jar --evaluate --cuglm --predict-type sequence --dynamic --tokenized fulltoken --solution-name flute`