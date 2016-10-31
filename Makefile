.PHONY: all

all:
	@mkdir -p bin
	javac src/*.java -cp lib/lwjgl/\* -d bin
