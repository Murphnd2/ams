<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="CreateInsertLink">
    <div class="row">
        <div class="col-12">
            <div class="input-group">
                <span class="input-group-text">Name</span>
                <input type="text" class="form-control" name="linkName" required>
            </div>
        </div>
        <div class="col-12 mt-3">
            <div class="input-group">
                <span class="input-group-text">Path</span>
                <input type="text" class="form-control" name="linkPath" required>
            </div>
        </div>
        <div class="col-12 mt-3">
            <button type="submit" class="btn btn-primary w-100">Submit</button>
        </div>
    </div>
</form>
