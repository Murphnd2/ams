#!/bin/bash
# Domain Guard Hook for Claude in Chrome
# Restricts navigation to allowed AMS demo domains only.
# Used as a PreToolUse hook in .claude/settings.local.json
#
# Exit codes:
#   0 = allow the tool call
#   2 = block the tool call

ALLOWED_DOMAINS="demo\.superiorstate\.biz|bpo\.superiorstate\.biz"

# Read the hook JSON from stdin
INPUT=$(cat)

# Extract tool name
TOOL_NAME=$(echo "$INPUT" | python -c "import sys,json; print(json.load(sys.stdin).get('tool_name',''))" 2>/dev/null)

# Extract tool_input as a JSON string for further parsing
TOOL_INPUT=$(echo "$INPUT" | python -c "import sys,json; import json as j; print(j.dumps(json.load(sys.stdin).get('tool_input',{})))" 2>/dev/null)

case "$TOOL_NAME" in

  mcp__Claude_in_Chrome__navigate)
    # Check the url parameter
    URL=$(echo "$TOOL_INPUT" | python -c "import sys,json; print(json.load(sys.stdin).get('url',''))" 2>/dev/null)

    # Allow browser history navigation
    if [[ "$URL" == "back" || "$URL" == "forward" ]]; then
      exit 0
    fi

    # Check if URL matches allowed domains
    if echo "$URL" | grep -qEi "^https?://($ALLOWED_DOMAINS)"; then
      exit 0
    fi

    # Block everything else
    echo "BLOCKED: Navigation to '$URL' is not allowed. Only demo.superiorstate.biz and bpo.superiorstate.biz are permitted." >&2
    exit 2
    ;;

  mcp__Claude_in_Chrome__javascript_tool)
    # Check for location-changing JavaScript
    JS_CODE=$(echo "$TOOL_INPUT" | python -c "import sys,json; print(json.load(sys.stdin).get('text',''))" 2>/dev/null)

    if echo "$JS_CODE" | grep -qEi "window\.location|document\.location|location\.href|location\.assign|location\.replace|window\.open"; then
      echo "BLOCKED: JavaScript containing location/navigation changes is not allowed. Use the navigate tool instead." >&2
      exit 2
    fi

    exit 0
    ;;

  *)
    # All other Chrome tools operate on the current tab — allow them
    exit 0
    ;;
esac
