docker:
	docker build . -t mimalyzer:latest

watch-smithy4s:
	cd shared && \
		cat ../build/watch-smithy4s.sc | scala-cli run _.sc -- watch

smithy4s:
	cd shared && \
		cat ../build/watch-smithy4s.sc  | scala-cli run _.sc -- generate && \
		scala-cli --power compile . -O -rewrite -O -source -O 3.4-migration

setup-ide:
	rm -rf .scala-build .bsp .metals 
	cd build && scala-cli --power setup-ide .
	cd shared && scala-cli --power setup-ide .
	cd frontend && scala-cli --power setup-ide .
	cd backend && scala-cli --power setup-ide .

code-check:
	cd backend && scala-cli --power fmt . --check
	cd frontend && scala-cli --power fmt . --check

pre-ci:
	cd backend && scala-cli --power fmt .
	cd frontend && scala-cli --power fmt .

run-backend:
	cd backend && scala-cli --power run -w . --restart -- 9999

run-frontend:
	cd frontend && npm install && npm run dev


