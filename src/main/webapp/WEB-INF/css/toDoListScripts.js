
function toggleWhen1(toDoId){
    let cbA = "cb1a" + toDoId;
    let cbB = "cb1b" + toDoId;
    let cb1 = document.getElementById(cbA);
    let cb2 = document.getElementById(cbB);
    cb2.checked=false;
    cb1.disabled=true;
    cb2.disabled=false;
}
function toggleWhen2(toDoId){
    let cbA = "cb1a" + toDoId;
    let cbB = "cb1b" + toDoId;
    let cb1 = document.getElementById(cbA);
    let cb2 = document.getElementById(cbB);
    cb1.checked=false;
    cb2.disabled=true;
    cb1.disabled=false;
}