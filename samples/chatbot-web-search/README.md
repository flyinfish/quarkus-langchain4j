# Web search example

This example demonstrates how to create a chatbot that can use the Tavily search
engine to look up information on the web.

## Running the example

A prerequisite to running this example is to provide your OpenAI and Tavily API keys.

add [.env](.env)
```
QUARKUS_LANGCHAIN4J_AZURE_OPENAI_RESOURCE_NAME=<your-azure-openapi-api-host>
QUARKUS_LANGCHAIN4J_AZURE_OPENAI_API_KEY=<your-azure-openapi-api-key>
QUARKUS_LANGCHAIN4J_TAVILY_API_KEY=<your-tavily-api-key>
```

Then, simply run the project in Dev mode:

```
mvn quarkus:dev
```

## Using the example

Open your browser and navigate to http://localhost:8080. Click the red robot
in the bottom right corner to open the chat window.

Read the description on the web page to learn about the implementation details.


