java_import(
	name = "lwjgl",
	jars = glob(["lib/lwjgl/*.jar"]),
)

java_binary(
	name = "jexx",
	srcs = glob(["src/*.java"]),
	main_class = "Jexx",
	deps = [":lwjgl"],
)
