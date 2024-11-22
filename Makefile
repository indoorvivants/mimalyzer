docker:
	docker build . -t mimalyzer:latest

watch-smithy4s:
	cd shared && \
		cat ../build/watch-smithy4s.sc | scala-cli run _.sc -- watch

watch-scala213bridge:
	cd scala-213-bridge && \
		scala-cli package -w . --library -f -o ../.dev/scala213bridge.jar

watch-scala212bridge:
	cd scala-212-bridge && \
		scala-cli package -w . --library -f -o ../.dev/scala212bridge.jar

watch-scala3bridge:
	cd scala-3-bridge && \
		scala-cli package -w . --library -f -o ../.dev/scala3bridge.jar

smithy4s:
	cd shared && \
		cat ../build/watch-smithy4s.sc  | scala-cli run _.sc -- generate && \
		scala-cli --power compile . -O -rewrite -O -source -O 3.4-migration

.dev:
	mkdir -p .dev

.dev/scala3bridge.jar: .dev
	cd scala-3-bridge && scala-cli package . --library -f -o ../.dev/scala3bridge.jar

.dev/scala212bridge.jar: .dev
	cd scala-212-bridge && scala-cli package . --library -f -o ../.dev/scala212bridge.jar

.dev/scala213bridge.jar: .dev
	cd scala-213-bridge && scala-cli package . --library -f -o ../.dev/scala213bridge.jar

.dev/scala213.classpath: .dev
	cs fetch -p org.scala-lang:scala-library:2.13.15 > .dev/scala213.classpath

.dev/scala213.compiler.classpath: .dev
	cs fetch -p org.scala-lang:scala-compiler:2.13.15 > .dev/scala213.compiler.classpath

.dev/scala212.classpath: .dev
	cs fetch -p org.scala-lang:scala-library:2.12.20 > .dev/scala212.classpath

.dev/scala212.compiler.classpath: .dev
	cs fetch -p org.scala-lang:scala-compiler:2.12.20 > .dev/scala212.compiler.classpath


.dev/scala3.compiler.classpath: .dev
	cs fetch -p org.scala-lang:scala3-compiler_3:3.3.4 > .dev/scala3.compiler.classpath

.dev/scala3.classpath: .dev
	cs fetch -p org.scala-lang:scala3-library_3:3.3.4 > .dev/scala3.classpath

library_classpaths = ./.dev/scala3.classpath ./.dev/scala213.classpath ./.dev/scala212.classpath
compiler_classpaths = ./.dev/scala3.compiler.classpath ./.dev/scala213.compiler.classpath ./.dev/scala212.compiler.classpath
bridges = ./.dev/scala3bridge.jar ./.dev/scala213bridge.jar ./.dev/scala212bridge.jar

prepare-classpaths: $(library_classpaths) $(compiler_classpaths)

setup-ide:
	rm -rf .scala-build .bsp .metals 
	cd build && scala-cli --power setup-ide .
	cd shared && scala-cli --power setup-ide .
	cd frontend && scala-cli --power setup-ide .
	cd backend && scala-cli --power setup-ide .
	cd compiler-interface && scala-cli --power setup-ide .
	cd scala-213-bridge && scala-cli --power setup-ide .
	cd scala-212-bridge && scala-cli --power setup-ide .
	cd scala-3-bridge && scala-cli --power setup-ide .

code-check:
	cd backend && scala-cli --power fmt . --check
	cd frontend && scala-cli --power fmt . --check

pre-ci:
	cd backend && scala-cli --power fmt .
	cd frontend && scala-cli --power fmt .

run-backend: $(bridges) $(library_classpaths) $(compiler_classpaths)
	cd backend && \
		export SCALA_213_COMPILER_CLASSPATH_FILE="../.dev/scala213.compiler.classpath" && \
		export SCALA_213_CLASSPATH_FILE="../.dev/scala213.classpath" && \
		export SCALA_213_BRIDGE="../.dev/scala213bridge.jar" && \
		export SCALA_3_COMPILER_CLASSPATH_FILE="../.dev/scala3.compiler.classpath" && \
		export SCALA_3_CLASSPATH_FILE="../.dev/scala3.classpath" && \
		export SCALA_3_BRIDGE="../.dev/scala3bridge.jar" && \
		export SCALA_212_COMPILER_CLASSPATH_FILE="../.dev/scala212.compiler.classpath" && \
		export SCALA_212_CLASSPATH_FILE="../.dev/scala212.classpath" && \
		export SCALA_212_BRIDGE="../.dev/scala212bridge.jar" && \
		scala-cli --power run -w . --restart -- 9999

run-frontend:
	cd frontend && npm install && npm run dev


