<!doctype html>
<html>
<body>
<form action="/ams/UploadCsvServlet"
      method="post"
      enctype="multipart/form-data">
  <input type="file" name="file1"><br>
  <input type="text" name="matchedPrefix" value="I3||sample.csv"><br>
  <button type="submit">Send</button>
</form>
</body>
</html>

