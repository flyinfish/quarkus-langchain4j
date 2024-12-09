# Chatbot example with Easy RAG

This example demonstrates how to create a simple chatbot with RAG using
`quarkus-langchain4j` and specifically the Easy RAG extension.
For more information about Easy RAG, refer to the file
`docs/modules/ROOT/pages/easy-rag.adoc` or https://docs.quarkiverse.io/quarkus-langchain4j/dev/easy-rag.htm.

## Running the example

A prerequisite to running this example is to provide your OpenAI API key.

add [.env](.env)
```
QUARKUS_LANGCHAIN4J_AZURE_OPENAI_RESOURCE_NAME=<your-azure-openapi-api-host>
QUARKUS_LANGCHAIN4J_AZURE_OPENAI_API_KEY=<your-azure-openapi-api-key>
```

Then, simply run the project in Dev mode:

```
mvn quarkus:dev
```

## Using the example

Open your browser and navigate to http://localhost:8080. Click the red robot
in the bottom right corner to open the chat window.

The chatbot is a conversational agent that uses information from the files
in `src/main/resources/catalog` to answer your questions about banking
products. More information about how it works is shown on the webpage.

## Using other model providers

### Compatible OpenAI serving infrastructure

Add `quarkus.langchain4j.openai.base-url=http://yourerver` to `application.properties`.

In this case, `quarkus.langchain4j.openai.api-key` is generally not needed.

### Ollama

Replace:

```xml

<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-openai</artifactId>
    <version>${quarkus-langchain4j.version}</version>
</dependency>
```

with

```xml

<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-ollama</artifactId>
    <version>${quarkus-langchain4j.version}</version>
</dependency>
```
