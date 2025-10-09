<%@ page import="java.util.*, net.superiorstate.ams.model.upload.UploadPreviewResult" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Upload Preview</title>
  <style>
    table { border-collapse: collapse; width: 100%; margin-top: 20px; }
    th, td { border: 1px solid #aaa; padding: 8px; text-align: left; }
    th { background-color: #eee; }
    .warn { color: orange; font-weight: bold; }
    .error { color: red; font-weight: bold; }
    .ok { color: green; font-weight: bold; }
  </style>
</head>
<body>

<h2>📁 Upload Preview</h2>

<form method="post" action="<%= request.getContextPath() %>/RunSelectedImports">
  <table>
    <tr>
      <th>Select</th>
      <th>File</th>
      <th>Matched Mappings</th>
      <th>Headers Detected</th>
      <th>Status</th>
      <th>Override Mapping</th>
    </tr>
    <%
      List<UploadPreviewResult> results = (List<UploadPreviewResult>) request.getAttribute("uploadPreviewResults");
      if (results != null) {
        for (UploadPreviewResult r : results) {
    %>
    <tr>
      <td><input type="checkbox" name="selectedFile" value="<%= r.fileName %>"/></td>
      <td><%= r.fileName %></td>
      <td>
        <% if (!r.matchedMappings.isEmpty()) {
          for (String m : r.matchedMappings) { %>
        ✅ <%= m %><br/>
        <% }} else { %>
        ❌ <span class="error">No mapping</span>
        <% } %>
      </td>
      <td>
        <% if (r.headers.isEmpty()) { %>
        ⚠️ <span class="warn">No headers</span>
        <% } else {
          for (String h : r.headers) {
            out.print(h + "<br/>");
          }
        } %>
      </td>
      <td class="<%= r.headerMissing ? "warn" : "ok" %>">
        <%= r.headerMissing ? "Possible data file or malformed" : "OK" %>
      </td>
      <td>
        <select name="override_<%= r.fileName %>">
          <option value="">-- Auto Detect --</option>
          <%
            for (String prefix : net.superiorstate.ams.data.Importer.TABLE_MAPPINGS.stream().map(m -> m.filePrefix()).toList()) {
          %>
          <option value="<%= prefix %>"><%= prefix %></option>
          <%
            }
          %>
        </select>
      </td>
    </tr>
    <%
        }
      }
    %>
  </table>

  <br/>
  <button type="submit">✅ Start Import for Selected Files</button>
</form>

</body>
</html>


