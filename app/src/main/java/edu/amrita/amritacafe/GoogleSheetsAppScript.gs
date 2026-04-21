function doGet(e) {
  var ss = SpreadsheetApp.openById("1uUwh_9mLVUmG621v40kdGSMGblr_JyKZfpEE-xIL0vo");

  // Get the value of the "menu" parameter
  var menu = e.parameter.menu;
  var sheet = ss.getSheetByName(menu);

  var text = convertRangeToCsvFile_("he",sheet)

  return ContentService.createTextOutput(text);
}

function convertRangeToCsvFile_(csvFileName, sheet) {
  // get available data range in the spreadsheet
  var activeRange = sheet.getDataRange();
  try {
    var data = activeRange.getValues();
    var csvFile = undefined;

    // loop through the data in the range and build a string with the csv data
    if (data.length > 1) {
      var csv = "";
      for (var row = 0; row < data.length; row++) {
        for (var col = 0; col < data[row].length; col++) {
          if (data[row][col].toString().indexOf(",") != -1) {
            data[row][col] = "\"" + data[row][col] + "\"";
          }
        }

        // join each row's columns
        // add a carriage return to end of each row, except for the last one
        if (row < data.length-1) {
            var toAdd = data[row].join(",") ;
            toAdd = toAdd.replace(/[\s,]*$/, "") + "\r\n";
            csv += toAdd;
        } else {
            csv += data[row].join(",").replace(/[\s,]*$/, "");
        }
      }
      csvFile = csv;
    }
    return csvFile;
  }
  catch(err) {
    Logger.log(err);
    Browser.msgBox(err);
  }
}

function testTime() {
  var date = new Date()
  var timeFormat = date.getFullYear() + '-' + (date.getMonth()+1) + '-' + date.getDate() + ' '+ date.getHours() + ':'+ date.getMinutes() + ':'+ date.getSeconds() + "." + date.getMilliseconds()
   Logger.log(timeFormat)
}

function doPost(e) {
  var ss = SpreadsheetApp.openById("1uUwh_9mLVUmG621v40kdGSMGblr_JyKZfpEE-xIL0vo");
  var sheet = ss.getSheetByName('Sheet1');

  Logger.log("doPostEntry: " + e)
  // Parse the request data
  var data = JSON.parse(e.postData.getDataAsString());
  // var data = e;
  var time = data.time;
  var tablet = data.tablet;
  var order = data.order;
  var items = data.items;
  var isGpay = data.isGpay || false;

  var date = new Date(time);
  var timeFormat = date.getFullYear() + '-' + (date.getMonth()+1) + '-' + date.getDate() + ' '+ date.getHours() + ':'+ date.getMinutes() + ':'+ date.getSeconds() + "." + date.getMilliseconds()

  for (var i = 0; i < items.length; i++) {
    var gpayAmount = isGpay ? items[i].total : 0;
    sheet.appendRow([timeFormat, tablet, order, items[i].quantity, items[i].name, items[i].renounciate, items[i].cost, items[i].total, gpayAmount]);
  }
  // sheet.appendRow([time,tablet,order,items[0].quantity,items[0].name,items[0].cost,items[0].total]);

  // Return a success message (optional)
  return ContentService.createTextOutput("Name inserted successfully!");
}

function getLastRow(sheet) {
  var lastRow = sheet.getLastRow();
  var emptyRow = 1;
  for (var i = 1; i <= lastRow; i++) {
    if (sheet.getRange(i, 1).isBlank()) {
      emptyRow = i;
      break;
    }
  }
  return emptyRow;
}

function insertRowAtTop_v1(data, sheetName, targetRow) {

  // const ss = SpreadsheetApp.getActiveSpreadsheet();
  var ss = SpreadsheetApp.openById("1uUwh_9mLVUmG621v40kdGSMGblr_JyKZfpEE-xIL0vo");
  const sheet = ss.getSheetByName(sheetName);

  // Insert a row
  // NOTE show what happens if we use insertRowAfter and how it carries over formatting from the top.
  sheet.insertRowBefore(targetRow);
  sheet
    .getRange(targetRow, 1, 1, data[0].length)
    .setValues(data);

  SpreadsheetApp.flush();
}

function runsies_example1(){
  const targetRow = 2;
  const sheetName = "Sheet1"

  // Dummy Data
  const myDate = new Date();
  const myTime = myDate.getTime() // The id
  const data = [
    [
      myTime,
      myDate,
      `${myTime}@example.com`
    ]
  ]

  insertRowAtTop_v1(data, sheetName, targetRow)
}

function insertRow(data) {
  // Get the sheet using its name or ID (replace with your sheet details)
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheet("1uUwh_9mLVUmG621v40kdGSMGblr_JyKZfpEE-xIL0vo");

  // Prepare the data row (adjust based on your specific data structure)
  var row = [data.name, data.value];

  // Insert the row at the end of the sheet
  sheet.appendRow(row);
  Logger.log("My function is working!2");
}

function myFunction() {
  // ... your code here ...
  var sheet = SpreadsheetApp.getActiveSheet();
  sheet.getRange("A1").setValue("Function worked!");
}