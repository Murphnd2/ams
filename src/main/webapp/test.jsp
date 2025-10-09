<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Create Rate</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body>
<div class="container mt-4">
    <h2>Create Rate</h2>
    <form id="rateForm">
        <div id="rateRows"></div>

        <div class="mt-4 text-end">
            <button type="button" class="btn btn-primary" onclick="previewRate()">Preview Rate</button>
        </div>
    </form>

    <!-- Preview Modal -->
    <div class="modal fade" id="previewModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Rate Preview</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body" id="previewBody"></div>
            </div>
        </div>
    </div>
</div>

<script>
    // In-memory caches
    let serviceOptions = [];
    let addOnOptions = [];
    let feeTypeOptions = [];

    let rowId = 0;

    function createRateRow(afterId = null) {
        rowId++;
        const rowDiv = document.createElement("div");
        rowDiv.className = "row mb-2 align-items-center rate-entry";
        rowDiv.id = "rateRow" + rowId;

        rowDiv.innerHTML = `
        <div class="col-md-2">
            <select class="form-select type-select" onchange="populateServiceSelect(this, ${rowId})">
                <option value="service" selected>Service</option>
                <option value="add-on">Add-on</option>
            </select>
        </div>
        <div class="col-md-3">
            <select class="form-select service-select"></select>
            <button type="button" class="btn btn-link p-0" onclick="addNewService(this, ${rowId})">+ New</button>
        </div>
        <div class="col-md-3">
            <select class="form-select fee-type-select"></select>
            <button type="button" class="btn btn-link p-0" onclick="addNewFeeType(this, ${rowId})">+ New</button>
        </div>
        <div class="col-md-2">
            <input type="text" class="form-control price-input" placeholder="$0.00">
        </div>
        <div class="col-md-2">
            <button type="button" class="btn btn-outline-success" onclick="createRateRow(${rowId})">+ Add Row Below</button>
        </div>
    `;

        if (afterId) {
            const afterElement = document.getElementById("rateRow" + afterId);
            afterElement.insertAdjacentElement("afterend", rowDiv);
        } else {
            document.getElementById("rateRows").appendChild(rowDiv);
        }

        populateServiceSelect(rowDiv.querySelector(".type-select"), rowId);
        populateFeeTypeSelect(rowId);
    }

    function populateServiceSelect(typeSelectEl, rowId) {
        const type = typeSelectEl?.value || "service";
        const row = document.getElementById("rateRow" + rowId);
        const serviceSelect = row.querySelector(".service-select");
        if (!serviceSelect) return;

        // Select correct cache
        const options = type === "add-on" ? addOnOptions : serviceOptions;

        // Clear and rebuild dropdown
        serviceSelect.innerHTML = "";
        for (const opt of options) {
            if (!opt || !opt.trim()) continue;
            const option = document.createElement("option");
            option.value = opt;
            option.textContent = opt;
            serviceSelect.appendChild(option);
        }

        if (serviceSelect.options.length > 0) {
            serviceSelect.selectedIndex = 0;
        }
    }



    function populateFeeTypeSelect(rowId) {
        const row = document.getElementById("rateRow" + rowId);
        const feeSelect = row.querySelector(".fee-type-select");

        // Clear existing options
        feeSelect.innerHTML = "";

        for (const opt of feeTypeOptions) {
            if (!opt || !opt.trim()) continue;
            const option = document.createElement("option");
            option.value = opt;
            option.textContent = opt;
            feeSelect.appendChild(option);
        }

        if (feeSelect.options.length > 0) {
            feeSelect.selectedIndex = 0;
        }
    }


    function addNewService(button, rowId) {
        const typeSelect = document.querySelector(`#rateRow${rowId} .type-select`);
        const type = typeSelect ? typeSelect.value : "service";
        const select = button.previousElementSibling;

        if (!select || select.tagName !== 'SELECT') return;

        const input = document.createElement("input");
        input.type = "text";
        input.className = "form-control service-input";
        input.placeholder = `Enter new ${type}`;

        input.onblur = function () {
            const value = input.value.trim();
            if (!value) {
                input.replaceWith(select);
                return;
            }

            const cache = (type === "service") ? serviceOptions : addOnOptions;
            if (!cache.includes(value)) {
                cache.push(value);
                console.log(`[${type}] options:`, cache);
            }

            const newSelect = document.createElement("select");
            newSelect.className = "form-select service-select";

            for (const opt of cache) {
                const option = document.createElement("option");
                option.value = opt;
                option.textContent = opt;
                if (opt === value) option.selected = true;
                newSelect.appendChild(option);
            }

            input.replaceWith(newSelect);
        };

        select.replaceWith(input);
        input.focus();
    }


    function addNewService(button, rowId) {
        const typeSelect = document.querySelector(`#rateRow${rowId} .type-select`);
        const type = typeSelect?.value || "service";

        const select = button.previousElementSibling;
        if (!select || select.tagName !== 'SELECT') return;

        const input = document.createElement("input");
        input.type = "text";
        input.className = "form-control service-input";
        input.placeholder = `Enter new ${type}`;

        input.onblur = function () {
            const value = input.value.trim();
            if (!value) {
                input.replaceWith(select);
                return;
            }

            // Add to correct cache
            if (type === "service" && !serviceOptions.includes(value)) {
                serviceOptions.push(value);
            } else if (type === "add-on" && !addOnOptions.includes(value)) {
                addOnOptions.push(value);
            }

            // Build dropdown with updated list
            const newSelect = document.createElement("select");
            newSelect.className = "form-select service-select";
            const list = type === "service" ? serviceOptions : addOnOptions;

            for (const opt of list) {
                if (!opt || !opt.trim()) continue;
                const option = document.createElement("option");
                option.value = opt;
                option.textContent = opt;
                if (opt === value) option.selected = true;
                newSelect.appendChild(option);
            }

            input.replaceWith(newSelect);
        };

        select.replaceWith(input);
        input.focus();
    }



    function previewRate() {
        const rows = document.querySelectorAll(".rate-entry");
        const previewBody = document.getElementById("previewBody");

        const currencyFormatter = new Intl.NumberFormat("en-US", {
            style: "currency",
            currency: "USD",
            minimumFractionDigits: 2
        });

        const data = [];

        rows.forEach(row => {
            const type = row.querySelector(".type-select")?.value || "";
            const service = row.querySelector(".service-select")?.selectedOptions[0]?.textContent.trim() || "";
            const fee = row.querySelector(".fee-type-select")?.selectedOptions[0]?.textContent.trim() || "";
            const priceRaw = row.querySelector(".price-input")?.value.trim() || "0";
            const priceValue = parseFloat(priceRaw.replace(/[^0-9.]/g, "")) || 0;
            const priceFormatted = currencyFormatter.format(priceValue);

            data.push({ type, service, fee, price: priceValue, priceFormatted });
        });

        // Sort: service before add-on, name asc, price desc
        data.sort((a, b) => {
            if (a.type !== b.type) return a.type === "service" ? -1 : 1;
            const nameCompare = a.service.localeCompare(b.service);
            if (nameCompare !== 0) return nameCompare;
            return b.price - a.price;
        });

        previewBody.innerHTML = "";

        const table = document.createElement("table");
        table.className = "table table-bordered";

        const thead = document.createElement("thead");
        const headRow = document.createElement("tr");
        ["Type", "Service/Add-on", "Fee Type", "Price"].forEach(headerText => {
            const th = document.createElement("th");
            th.textContent = headerText;
            headRow.appendChild(th);
        });
        thead.appendChild(headRow);
        table.appendChild(thead);

        const tbody = document.createElement("tbody");

        data.forEach(({ type, service, fee, priceFormatted }) => {
            const tr = document.createElement("tr");
            [type, service, fee, priceFormatted].forEach(cellText => {
                const td = document.createElement("td");
                td.textContent = cellText;
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });

        table.appendChild(tbody);
        previewBody.appendChild(table);

        const modal = new bootstrap.Modal(document.getElementById("previewModal"));
        modal.show();
    }


    // Initialize
    window.onload = () => createRateRow();
</script>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>


