<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- AI Knowledge Assistant chatbox — include on authenticated pages after navbar25.jsp --%>
<c:if test="${sessionScope.local.isAuthenticated() == true}">

    <%-- Floating trigger button --%>
    <button id="chatAssistantBtn" class="btn btn-primary shadow-lg"
            onclick="toggleChatbox()"
            style="position:fixed; bottom:24px; right:24px; z-index:1050;
               width:56px; height:56px; border-radius:50%;
               display:flex; align-items:center; justify-content:center;
               font-size:1.4rem;">
        <i class="bi bi-chat-dots-fill"></i>
    </button>

    <%-- Slide-out chat panel --%>
    <div id="chatAssistantPanel" style="display:none; position:fixed; bottom:90px; right:24px;
     z-index:1049; width:400px; max-height:520px;
     border-radius:12px; overflow:hidden;
     box-shadow:0 8px 32px rgba(0,0,0,0.18);
     background:#fff; border:1px solid #dee2e6;
     flex-direction:column;">

            <%-- Header --%>
        <div style="background:linear-gradient(135deg,#2B5F8A,#3a7bc8); color:#fff;
              padding:14px 18px; display:flex; align-items:center; justify-content:space-between;">
            <div>
                <i class="bi bi-robot"></i>&nbsp;
                <strong>Knowledge Assistant</strong>
            </div>
            <button class="btn btn-sm btn-link text-white p-0" onclick="toggleChatbox()" style="font-size:1.2rem; text-decoration:none;">
                <i class="bi bi-x-lg"></i>
            </button>
        </div>

            <%-- Messages area --%>
        <div id="chatMessages" style="flex:1; overflow-y:auto; padding:14px; min-height:300px; max-height:360px; background:#f8f9fa;">
            <div class="text-muted small text-center" style="margin-top:60px;">
                <i class="bi bi-lightbulb"></i>&nbsp;
                Ask me about Summit, benefits, billing, or company procedures.
            </div>
        </div>

            <%-- Input area --%>
        <div style="border-top:1px solid #dee2e6; padding:10px 14px; background:#fff;">
            <%-- File upload indicator (hidden by default) --%>
            <div id="chatFileIndicator" style="display:none; margin-bottom:6px;">
                <span class="badge bg-light text-dark border" style="font-size:0.78rem;">
                    <i class="bi bi-file-earmark me-1"></i>
                    <span id="chatFileName"></span>
                    <button type="button" class="btn-close ms-2" style="font-size:0.5rem;"
                            onclick="clearChatFile()" aria-label="Remove"></button>
                </span>
            </div>
            <div class="input-group">
                <c:if test="${sessionScope.local.isPspAdmin()}">
                <button class="btn btn-outline-secondary btn-sm" type="button"
                        onclick="document.getElementById('chatFileInput').click()"
                        title="Upload a file for analysis">
                    <i class="bi bi-paperclip"></i>
                </button>
                </c:if>
                <input type="text" id="chatInput" class="form-control form-control-sm"
                       placeholder="Type your question..."
                       onkeydown="if(event.key==='Enter') sendQuestion()"
                       autocomplete="off" />
                <button class="btn btn-primary btn-sm" onclick="sendQuestion()" id="chatSendBtn">
                    <i class="bi bi-send-fill"></i>
                </button>
            </div>
            <input type="file" id="chatFileInput" accept=".pdf,.xlsx,.csv,.txt" style="display:none;"
                   onchange="handleChatFileSelect(this)">
        </div>
    </div>

    <script>
        function toggleChatbox() {
            const panel = document.getElementById('chatAssistantPanel');
            if (panel.style.display === 'none' || panel.style.display === '') {
                panel.style.display = 'flex';
                document.getElementById('chatInput').focus();
            } else {
                panel.style.display = 'none';
            }
        }

        let pendingChatFile = null;

        function handleChatFileSelect(input) {
            if (input.files && input.files[0]) {
                const file = input.files[0];
                if (file.size > 10 * 1024 * 1024) {
                    appendMessage('assistant', 'File is too large. Maximum size is 10MB.');
                    input.value = '';
                    return;
                }
                pendingChatFile = file;
                document.getElementById('chatFileName').textContent = file.name;
                document.getElementById('chatFileIndicator').style.display = 'block';
            }
        }

        function clearChatFile() {
            pendingChatFile = null;
            document.getElementById('chatFileInput').value = '';
            document.getElementById('chatFileIndicator').style.display = 'none';
        }

        function sendQuestion() {
            const input = document.getElementById('chatInput');
            const question = input.value.trim();

            // If there's a file pending, send via file upload endpoint
            if (pendingChatFile) {
                const fileName = pendingChatFile.name;
                const displayMsg = question || ('Analyze: ' + fileName);
                appendMessage('user', displayMsg);
                input.value = '';

                const loadingId = 'loading-' + Date.now();
                appendLoading(loadingId);
                input.disabled = true;
                document.getElementById('chatSendBtn').disabled = true;

                const formData = new FormData();
                formData.append('chatFile', pendingChatFile);
                formData.append('question', question || 'Please analyze this document.');

                clearChatFile();

                fetch('ChatAssistant', {
                    method: 'POST',
                    body: formData
                })
                .then(response => response.json())
                .then(data => {
                    removeLoading(loadingId);
                    if (data.error) {
                        appendMessage('assistant', data.error);
                    } else {
                        appendMessage('assistant', data.answer);
                    }
                })
                .catch(err => {
                    removeLoading(loadingId);
                    appendMessage('assistant', 'Sorry, something went wrong processing the file.');
                    console.error('File upload error:', err);
                })
                .finally(() => {
                    input.disabled = false;
                    document.getElementById('chatSendBtn').disabled = false;
                    input.focus();
                });

                return;
            }

            // Normal text question flow
            if (!question) return;

            // Show user message
            appendMessage('user', question);
            input.value = '';

            // Show loading indicator
            const loadingId = 'loading-' + Date.now();
            appendLoading(loadingId);

            // Disable input while waiting
            input.disabled = true;
            document.getElementById('chatSendBtn').disabled = true;

            fetch('ChatAssistant', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ question: question })
            })
                .then(response => response.json())
                .then(data => {
                    removeLoading(loadingId);
                    if (data.error) {
                        appendMessage('assistant', data.error);
                    } else {
                        appendMessage('assistant', data.answer);
                    }
                })
                .catch(err => {
                    removeLoading(loadingId);
                    appendMessage('assistant', 'Sorry, something went wrong. Please try again.');
                    console.error('Chat error:', err);
                })
                .finally(() => {
                    input.disabled = false;
                    document.getElementById('chatSendBtn').disabled = false;
                    input.focus();
                });
        }

        function appendMessage(role, text) {
            const container = document.getElementById('chatMessages');
            // Clear the initial placeholder on first message
            if (container.querySelector('.text-muted.text-center')) {
                container.querySelector('.text-muted.text-center').remove();
            }

            const wrapper = document.createElement('div');
            wrapper.style.marginBottom = '12px';
            wrapper.style.display = 'flex';
            wrapper.style.justifyContent = role === 'user' ? 'flex-end' : 'flex-start';

            const bubble = document.createElement('div');
            bubble.style.maxWidth = '85%';
            bubble.style.padding = '10px 14px';
            bubble.style.borderRadius = '12px';
            bubble.style.fontSize = '0.875rem';
            bubble.style.lineHeight = '1.5';
            bubble.style.wordWrap = 'break-word';

            if (role === 'user') {
                bubble.style.background = '#2B5F8A';
                bubble.style.color = '#fff';
                bubble.textContent = text;
            } else {
                bubble.style.background = '#fff';
                bubble.style.border = '1px solid #dee2e6';
                bubble.style.color = '#212529';
                bubble.innerHTML = formatResponse(text);
            }

            wrapper.appendChild(bubble);
            container.appendChild(wrapper);
            container.scrollTop = container.scrollHeight;
        }

        function appendLoading(id) {
            const container = document.getElementById('chatMessages');
            const wrapper = document.createElement('div');
            wrapper.id = id;
            wrapper.style.marginBottom = '12px';
            wrapper.style.display = 'flex';
            wrapper.style.justifyContent = 'flex-start';
            wrapper.innerHTML =
                '<div style="background:#fff; border:1px solid #dee2e6; padding:10px 14px; border-radius:12px; color:#6c757d; font-size:0.875rem;">' +
                '<i class="bi bi-three-dots"></i> Thinking...' +
                '</div>';
            container.appendChild(wrapper);
            container.scrollTop = container.scrollHeight;
        }

        function removeLoading(id) {
            const el = document.getElementById(id);
            if (el) el.remove();
        }

        function formatResponse(text) {
            // Convert markdown-style links to HTML
            let html = text.replace(/\[([^\]]+)\]\((https?:\/\/[^\)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');
            // Convert plain URLs to links
            html = html.replace(/(^|[^"'])(https?:\/\/[^\s<]+)/g, '$1<a href="$2" target="_blank" rel="noopener">$2</a>');
            // Convert newlines to <br>
            html = html.replace(/\n/g, '<br>');
            // Bold text
            html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
            return html;
        }
    </script>

</c:if>
