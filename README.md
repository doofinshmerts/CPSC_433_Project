# BUILDING AND RUNING
## Making the program:
The simplest way to build and run the program is with the shell script: "./runExample" \
Or you can use the make file: "make clean" and "make all"
## Running the program manualy:
type command: "java -jar ./build/Build.jar"

## build commands for building manualy
"javac -d ./build ./schedulesearch/*.java"\
jar cfe ./build/Build.jar schedulesearch.Main -C ./build schedulesearch
java -jar ./build/Build.jar

