var SPREADSHEET_ID = "1uUwh_9mLVUmG621v40kdGSMGblr_JyKZfpEE-xIL0vo";

function doGet(e) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var menu = e.parameter.menu;
  var sheet = ss.getSheetByName(menu);
  if (!sheet) return ContentService.createTextOutput("Error: Sheet not found");

  var text = convertRangeToCsvFile_(sheet);
  return ContentService.createTextOutput(text);
}

function doPost(e) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName('Sheet1');

  var data = JSON.parse(e.postData.getDataAsString());
  var time = data.time;
  var tablet = data.tablet;
  var order = data.order;
  var items = data.items;
  var isGpay = data.isGpay || false;

  var date = new Date(time);
  var timeFormat = date.getFullYear() + '-' + (date.getMonth()+1) + '-' + date.getDate() + ' '+ date.getHours() + ':'+ date.getMinutes() + ':'+ date.getSeconds() + "." + date.getMilliseconds();

  for (var i = 0; i < items.length; i++) {
    var gpayAmount = isGpay ? items[i].total : 0;
    sheet.appendRow([timeFormat, tablet, order, items[i].quantity, items[i].name, items[i].renounciate, items[i].cost, items[i].total, gpayAmount]);
  }
  return ContentService.createTextOutput("Success");
}

function convertRangeToCsvFile_(sheet) {
  var activeRange = sheet.getDataRange();
  try {
    var data = activeRange.getValues();
    if (data.length > 0) {
      var csv = "";
      for (var row = 0; row < data.length; row++) {
        for (var col = 0; col < data[row].length; col++) {
          if (data[row][col].toString().indexOf(",") != -1) {
            data[row][col] = "\"" + data[row][col] + "\"";
          }
        }
        var rowText = data[row].join(",").replace(/[\s,]*$/, "");
        csv += rowText + (row < data.length - 1 ? "\r\n" : "");
      }
      return csv;
    }
    return "";
  } catch(err) {
    return "Error: " + err.toString();
  }
}