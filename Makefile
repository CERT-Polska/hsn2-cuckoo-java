
COMPONENT=hsn2-cuckoo-java

all:	${COMPONENT}-package

clean:	${COMPONENT}-package-clean

${COMPONENT}-package:
	mvn clean install -U -Pbundle
	mkdir -p build/${COMPONENT}
	tar xzf target/${COMPONENT}-1.0.0-SNAPSHOT.tar.gz -C build/${COMPONENT}

${COMPONENT}-package-clean:
	rm -rf build

build-local:
	mvn clean install -U -Pbundle