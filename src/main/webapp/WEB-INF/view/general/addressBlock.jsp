<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<address class="container">
    <div class="row mb-3">
        <div class="col">
            <div class="input-group input-group-sm">
                <label for="address1" class="col-3 input-group-text">Address 1</label>
                <input type="text" class="form-control" id="address1" name="address1" required/>
            </div>
        </div>
    </div>
    <div class="row mb-3">

        <div class="col">
            <div class="input-group input-group-sm">
                <label for="address2" class="col-3 input-group-text">Address 2</label>
                <input type="text" class="form-control" id="address2" name="address2"/>
            </div>
        </div>
    </div>
    <div class="row mb-3">
        <div class="col">
            <div class="input-group input-group-sm">
                <label for="city" class="col-3 input-group-text">City</label>
                <input type="text" class="form-control" id="city" name="city" required/>
            </div>
        </div>
    </div>
    <div class="row mb-3">
        <div class="col">
            <div class="input-group input-group-sm">
                <%--@declare id="statelist"--%><label for="stateList" class="col-3 input-group-text">State</label>
                <c:import url="/WEB-INF/view/general/dropDown/ddStates.jsp"></c:import>
            </div>
        </div>
    </div>
    <div class="row mb-3">
        <div class="col">
            <div class = "input-group input-group-sm">
                <label for="zipCode" class="col-3 input-group-text">Zip Code</label>
                <input type="text" class="form-control" id="zipCode" name="zipCode" required pattern="\d{5}-?(\d{4})?" title="Must be either ##### or #####-####"/>
            </div>
        </div>
    </div>
</address>

