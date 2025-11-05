<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="CreatePspUser">
    <div class="row">
        <div class="col-12">
            <div class="input-group">
                <span class="input-group-text" style="min-width: 25%">Name</span>
                <input class="form-control" required type="text" name="firstName" placeholder="First Name">
                <input class="form-control" required type="text" name="lastName" placeholder="Last Name">
            </div>
        </div>
        <div class="col-12 mt-2">
            <div class="input-group">
                <span class="input-group-text" style="min-width:25%">User's Email</span>
                <input class="form-control" required type="email" name="userEmail" placeholder="Enter user's email here">
            </div>
        </div>
        <div class="col-12 mt-2">
            <div class="input-group">
                <span class="input-group-text" style="min-width: 25%;">Temp Password</span>
                <input class="form-control" type="text" name="tempPassword" placeholder="If left blank, one will be generated.">
            </div>
        </div>
        <div class="col-12 mt-2">
            <div class="form-check">
                <input type="checkbox" id="makeAdmin" name="makeAdmin" value="1" class="form-check-input">
                <label for="makeAdmin" class="form-check-label">Make Administrator?</label>
            </div>
        </div>
        <div class="col-12 mt-2">
            <button type="submit" class="btn btn-secondary">
                <i class="bi bi-plus-circle"></i>&nbsp;Create&nbsp;User
            </button>
        </div>
    </div>
</form>
