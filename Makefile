docker:
	docker build . -t my-fullstack-scala:latest

smithy4s:
	cd shared && \
		rm -rf fullstack_scala/protocol && \
		cs launch smithy4s --contrib -- generate protocol.smithy --skip resource --skip openapi && \
		scala-cli --power compile . -O -rewrite -O -source -O 3.4-migration

setup-ide:
	rm -rf .scala-build .bsp .metals 
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
