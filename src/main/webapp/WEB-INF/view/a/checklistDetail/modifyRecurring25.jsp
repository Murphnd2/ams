<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:choose>
  <c:when test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList()==null}">
    <div class="row mt-2">
      <div class="col">
        <button type="button" class="btn btn-outline-warning w-100" data-bs-target="#makeRecurringSequence" data-bs-toggle="modal">Make Recurring</button>
      </div>
    </div>
  </c:when>
  <c:otherwise>
    <div class="accordion mt-2" id="recurItem">
      <div class="accordion-item">
        <h2 class="accordion-header">
          <button class="accordion-button accordion-button-1 collapsed p-2" type="button" data-bs-toggle="collapse" data-bs-target="#collapseOne" aria-expanded="true" aria-controls="collapseOne">
            View Repeat Settings
          </button>
        </h2>
        <div id="collapseOne" class="accordion-collapse collapse" data-bs-parent="#recurItem">
          <div class="accordion-body m-0 p-0">
            <form method="post" action="ModifyRecurringTask25" >
              <div class="d-none" id="code_xx">
                <c:set var="cType" value="checked"> </c:set>
                <c:set var="cM" value=""> </c:set>
                <c:set var="cT" value=""> </c:set>
                <c:set var="cW" value=""> </c:set>
                <c:set var="cR" value=""> </c:set>
                <c:set var="cF" value=""> </c:set>
                <c:forEach var="dae" items="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getDoWList()}">
                  <c:if test="${dae.getWeekdayId()==1}">
                    <c:set var="cM" value="${cType}"> </c:set>
                  </c:if>
                  <c:if test="${dae.getWeekdayId()==2}">
                    <c:set var="cT" value="${cType}"> </c:set>
                  </c:if>
                  <c:if test="${dae.getWeekdayId()==3}">
                    <c:set var="cW" value="${cType}"> </c:set>
                  </c:if>
                  <c:if test="${dae.getWeekdayId()==4}">
                    <c:set var="cR" value="${cType}"> </c:set>
                  </c:if>
                  <c:if test="${dae.getWeekdayId()==5}">
                    <c:set var="cF" value="${cType}"> </c:set>
                  </c:if>
                </c:forEach>
                <c:set var="word1" value="Update"> </c:set>
                <c:set var="ability" value=""> </c:set>
                <c:if test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().isInActive()==true}">
                  <c:set var="word1" value="Restart"> </c:set>
                  <c:set var="ability" value="disabled"> </c:set>
                </c:if>
              </div>
              <div class="row mt-2">
                <div class="col-12">
                  <div class="row m-0 p-0">
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 p-0 ">
                      <button type="button" class="btn btn-sm btn-warning pe-none text-truncate w-100 rounded-end-0">
                        List # ${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getId()}
                      </button>
                    </div>
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 p-0 text-center">
                      <input type="text" class="form-control form-control-sm w-100 text-center" name="sequenceName" id="sequenceName" required
                             placeholder="Enter description here" value="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getDescription()}">
                    </div>
                  </div>
                </div>
                <div class="col-12 mt-2">
                  <div class="row m-0 p-0">
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 p-0">
                      <button type="button" class="btn btn-sm btn-warning pe-none text-truncate w-100 rounded-end-0">
                        Starts
                      </button>
                    </div>
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 p-0 text-center">
                      <input class="form-control form-control-sm w-100 text-center" type="date" style="width:100px" name="startDate" id="startDate" required value="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getDateStart()}">
                    </div>
                  </div>
                </div>
                <div class="col-12 mt-2">
                  <div class="row m-0 p-0">
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 p-0">
                      <button type="button" class="btn btn-sm btn-warning pe-none text-truncate w-100 rounded-end-0">
                        Days Ahead
                      </button>
                    </div>
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 p-0">
                      <select class="form-select form-select-sm w-100 text-center" name="daysInAdvance" id="daysInAdvance">
                        <c:forEach var="num" begin="0" end="15" step="1">
                          <c:choose>
                            <c:when test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getDaysInAdvance()==num}">
                              <option selected value="${num}">${num}</option>
                            </c:when>
                            <c:otherwise>
                              <option value="${num}">${num}</option>
                            </c:otherwise>
                          </c:choose>
                        </c:forEach>
                      </select>
                    </div>
                  </div>
                </div>
                <div class="col-12 mt-2">
                  <div class="row m-0 p-0">
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 p-0">
                      <button type="button" class="btn btn-sm btn-warning pe-none text-truncate w-100 rounded-end-0">
                        Assigned To
                      </button>
                    </div>
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 p-0">
                      <select class="form-select form-select-sm w-100 text-center" aria-label="recurring freq type drop down" name="userList" id="userList">
                        <c:forEach var="user" items="${applicationScope.global.getUsers()}">
                          <c:choose>
                            <c:when test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getAssignee().getId()==user.getId()}">
                              <option selected value="${user.getId()}">${user.getFullNameFirstLast()}</option>
                            </c:when>
                            <c:otherwise>
                              <option value="${user.getId()}">${user.getFullNameFirstLast()}</option>
                            </c:otherwise>
                          </c:choose>
                        </c:forEach>
                      </select>
                    </div>
                  </div>
                </div>
                <div class="col-12 mt-2">
                  <div class="row m-0 p-0">
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 p-0">
                      <button type="button" class="btn btn-sm btn-warning pe-none text-truncate w-100 rounded-end-0">
                        Frequency
                      </button>
                    </div>
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 p-0">
                      <select class="form-select form-select-sm text-center" aria-label="recurring freq type drop down" name="frequencyList" id="frequencyList">
                        <c:forEach var="taskFrequency" items="${applicationScope.global.getTaskFrequencies()}">
                          <c:choose>
                            <c:when test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getTaskFrequency().getId()==taskFrequency.getId()}">
                              <option selected value="${taskFrequency.getId()}">${taskFrequency.getDescription()}</option>
                            </c:when>
                            <c:otherwise>
                              <option value="${taskFrequency.getId()}">${taskFrequency.getDescription()}</option>
                            </c:otherwise>
                          </c:choose>
                        </c:forEach>
                      </select>
                    </div>
                  </div>
                </div>
                <div class="col-12 mt-1">
                  <div class="row m-0 p-0">
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 mt-1 p-0">
                      <button type="button" class="btn btn-sm btn-warning pe-none text-truncate w-100 rounded-end-0">
                        Repeats
                      </button>
                    </div>
                    <div class="col-12 col-sm-6 col-md-12 col-lg-6 col-xl-12 m-0 mt-1 p-0">
                      <div class="row m-0 mt-1 p-0">

                        <div class="col m-0 ms-1 p-0">
                          <input type="checkbox" ${cM} class="btn-check"   id="cbMonday1" name="cbMonday" value="1" autocomplete="off">
                          <label class="btn btn-sm btn-outline-dark w-100" for="cbMonday1">M</label>
                        </div>
                        <div class="col m-0 ms-1 p-0">
                          <input class="btn-check" ${cT} type="checkbox" id="cbTuesday1" name="cbTuesday"  value="2" autocomplete="off">
                          <label class="btn btn-sm btn-outline-dark w-100" for="cbTuesday1">T</label>
                        </div>
                        <div class="col m-0 ms-1 p-0">
                          <input class="btn-check" ${cW} type="checkbox" id="cbWednesday1" name="cbWednesday" value="3" autocomplete="off">
                          <label class="btn btn-sm btn-outline-dark w-100" for="cbWednesday1">W</label>
                        </div>
                        <div class="col m-0 ms-1 p-0">
                          <input class="btn-check" ${cR} type="checkbox" id="cbThursday1" name="cbThursday" value="4" autocomplete="off">
                          <label class="btn btn-sm btn-outline-dark w-100" for="cbThursday1">R</label>
                        </div>
                        <div class="col m-0 ms-1 p-0">
                          <input class="btn-check" ${cF} type="checkbox" id="cbFriday1" name="cbFriday" value="5" autocomplete="off">
                          <label class="btn btn-sm btn-outline-dark w-100" for="cbFriday1">F</label>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="row mt-2">
                <div class="col m-0">
                  <button class="btn btn-success w-100" id="btnStandardRec"  name="btnRecurring"  value="1" type="submit">
                    <i class="bi bi-play-btn"></i>
                      ${word1}
                  </button>
                </div>
                <div class="col m-0 ms-1">
                  <button class="btn btn-danger w-100" ${ability} id="btnCloseRec" value = "0" name="btnRecurring" type="submit">
                    <i class="bi bi-stop-btn"></i>
                    Stop Repeat
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </c:otherwise>
</c:choose>
