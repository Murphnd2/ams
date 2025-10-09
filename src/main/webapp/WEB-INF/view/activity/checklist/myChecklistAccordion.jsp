<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
  <c:when test="${sessionScope.myChecklists.size()>0}">
    <div class="accordion" id="accChecklists" name="accChecklists">
      <c:forEach var="list" items="${sessionScope.myChecklists}">
        <div class="accordion-item">
          <h2 class="accordion-header" id="${list.getId()}">
            <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#c${list.getId()}" aria-expanded="true" aria-controls="c${list.getId()}">
              ${list.getFullName()}
            </button>
          </h2>
          <div id="c${list.getId()}" class="accordion-collapse collapse show" aria-labelledby="headingOne" data-bs-parent="#accChecklists">
            <div class="accordion-body">
              <form method="post" action="ManageToDoList">
                <div class="row mb-1">
                  <div class="col">
                    <c:forEach var="toDo" items="${list.getToDoList()}">
                      <c:choose>
                        <c:when test="${toDo.isComplete()}">
                          <div class="form-check">
                            <input class="form-check-input" type="checkbox" value="td${list.getId()}-${toDo.getId()}" name="toDoCheck" id="td${list.getId()}-${toDo.getId()}" checked disabled>
                            <label class="form-check-label" for="td${list.getId()}-${toDo.getId()}">
                                ${toDo.getTask().getDescription}
                            </label>
                          </div>
                        </c:when>
                        <c:otherwise>
                          <div class="form-check">
                            <input class="form-check-input" type="checkbox" value="td${list.getId()}-${toDo.getId()}" name="toDoCheck" id="td${list.getId()}-${toDo.getId()}">
                            <label class="form-check-label" for="td${list.getId()}-${toDo.getId()}">
                              ${toDo.getTask().getDescription}
                            </label>
                          </div>
                        </c:otherwise>
                      </c:choose>
                    </c:forEach>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </div>
      </c:forEach>
    </div>
  </c:when>
  <c:otherwise>

  </c:otherwise>
</c:choose>