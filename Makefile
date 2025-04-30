# Variables for main classes
CRAWLER_MAIN_CLASS=Crawler.Main
INDEXER_MAIN_CLASS=Indexer.Main
PROCESSOR_MAIN_CLASS=processor.tony

# Target to build the Maven project
build:
	mvn clean install

# Target to run the crawler
run-crawler: build
	mvn exec:java -Dexec.mainClass="$(CRAWLER_MAIN_CLASS)"

# Target to run the indexer
run-indexer: build
	mvn exec:java -Dexec.mainClass="$(INDEXER_MAIN_CLASS)"

run-processor: build
	mvn exec:java -Dexec.mainClass="$(PROCESSOR_MAIN_CLASS)"

# Target to run tests
test: build
	mvn test

# Target to clean up Maven build files
clean:
	mvn clean

# Target to run everything (build, crawler, and indexer)
all: build run-crawler run-indexer
