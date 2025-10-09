<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="UploadFileServlet" enctype="multipart/form-data">
    <input type="file" name="file"/>
    <input type="submit" value="Upload">
</form>