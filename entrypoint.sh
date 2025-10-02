#!/bin/bash

# Start Ollama in the background.
/bin/ollama serve &
# Record Process ID.
pid=$!

# Pause for Ollama to start.
sleep 5

echo "🔴 Retrieve LLAMA3 model..."
ollama pull phi3:3.8b
echo "🟢 Done!"

# Wait for Ollama process to finish.
wait $pid