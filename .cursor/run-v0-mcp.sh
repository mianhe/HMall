#!/bin/bash
export PATH="/opt/homebrew/opt/node@22/bin:$PATH"
exec npx mcp-remote https://mcp.v0.dev --header "Authorization: Bearer $V0_API_KEY"
