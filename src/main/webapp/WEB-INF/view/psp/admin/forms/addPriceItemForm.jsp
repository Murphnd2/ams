<form action="AddPriceItem" method="post">
  <div class="row mb-3">
    <div class="col">
      <div class="input-group input-group-sm">
        <label for="priceItemName" class="input-group-text">Item Name</label>
        <input type="text" class="form-control" id="priceItemName" name = "priceItemName" required/>
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <div class="input-group input-group-sm">
        <label for="piSortOrder" class="input-group-text">Sort #</label>
        <input type="text" class="form-control" id="piSortOrder" name = "piSortOrder" min="0" required/>
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <button class="btn btn-secondary form-control" type="submit">Add</button>
    </div>
  </div>
</form>
