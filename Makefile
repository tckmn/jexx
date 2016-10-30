.PHONY: all

all:
	@mkdir -p bin
	javac src/Jexx.java -cp lib/lwjgl/\* -d bin
