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

function formatAppTime(millis) {
  var date = new Date(millis);
  // Matches app format: YYYY-M-D H:m:s.SSS
  return date.getFullYear() + '-' + (date.getMonth()+1) + '-' + date.getDate() + ' '+ date.getHours() + ':'+ date.getMinutes() + ':'+ date.getSeconds() + "." + date.getMilliseconds();
}

function doPost(e) {
  var ss = SpreadsheetApp.openById("1uUwh_9mLVUmG621v40kdGSMGblr_JyKZfpEE-xIL0vo");
  var sheet = ss.getSheetByName('Sheet1');

  // Parse the request data
  var data = JSON.parse(e.postData.getDataAsString());
  var timeMillis = data.time;
  var tablet = data.tablet;
  var isGpay = data.isGpay || false;
  var appVersion = data.appVersion || "";

  // Format the time EXACTLY like the app does for consistency
  var timeFormat = formatAppTime(timeMillis);

  // Handle Retrospective GPay Update
  if (data.action === "updateGPay") {
    var lastRow = sheet.getLastRow();
    if (lastRow < 2) return ContentService.createTextOutput("Sheet is empty.");

    // Dynamic headers
    var headers = sheet.getRange(1, 1, 1, Math.max(1, sheet.getLastColumn())).getValues()[0];
    
    // Find column indices (1-based for getRange)
    var tabletColIndex = headers.indexOf("TABLET") + 1 || 2; 
    var orderColIndex = headers.indexOf("ORDER") + 1 || 3;
    var totalColIndex = headers.indexOf("TOTAL") + 1 || 7;
    var gpayColIndex = headers.indexOf("GPAY AMOUNT") + 1 || 8;

    // Efficiency: Only check the last 2000 rows for updates (speed up search)
    var searchDepth = 2000;
    var startRow = Math.max(2, lastRow - searchDepth + 1);
    var numRows = lastRow - startRow + 1;

    // Get all columns up to the maximum column index we need
    var maxCol = Math.max(tabletColIndex, orderColIndex, totalColIndex, gpayColIndex);
    var values = sheet.getRange(startRow, 1, numRows, maxCol).getValues(); 
    var found = false;
    var targetOrder = data.order.toString();

    // Search bottom-up
    for (var i = values.length - 1; i >= 0; i--) {
      var cellTablet = values[i][tabletColIndex - 1].toString();
      var cellOrder = values[i][orderColIndex - 1].toString();

      if (cellOrder === targetOrder && cellTablet === tablet) {
        var total = values[i][totalColIndex - 1]; 
        var gpayAmount = isGpay ? total : 0;

        // Update GPay Amount column
        sheet.getRange(startRow + i, gpayColIndex).setValue(gpayAmount);
        found = true;
      } else if (found) {
        // If we already found the order block and now hit a different order, stop searching
        break;
      }
    }
    return ContentService.createTextOutput(found ? "Update successful!" : "Order not found in search range.");
  }

  // Normal Order Entry
  var order = data.order;
  var items = data.items;

  var headers = sheet.getRange(1, 1, 1, Math.max(1, sheet.getLastColumn())).getValues()[0];
  var appVersionColIndex = headers.indexOf("APP VERSION") + 1;

  for (var i = 0; i < items.length; i++) {
    var gpayAmount = isGpay ? items[i].total : 0;
    var rowValues = [timeFormat, tablet, order, items[i].quantity, items[i].name, items[i].cost, items[i].total, gpayAmount];
    
    if (appVersionColIndex > 0) {
      var maxCols = Math.max(rowValues.length, headers.length);
      var fullRow = [];
      for (var j = 0; j < maxCols; j++) {
        if (j === appVersionColIndex - 1) {
          fullRow.push(appVersion);
        } else if (j < rowValues.length) {
          fullRow.push(rowValues[j]);
        } else {
          fullRow.push("");
        }
      }
      sheet.appendRow(fullRow);
    } else {
      sheet.appendRow(rowValues);
    }
  }
  
  return ContentService.createTextOutput("Order inserted successfully!");
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
  var ss = SpreadsheetApp.openById("1uUwh_9mLVUmG621v40kdGSMGblr_JyKZfpEE-xIL0vo");
  const sheet = ss.getSheetByName(sheetName);
  sheet.insertRowBefore(targetRow);
  sheet.getRange(targetRow, 1, 1, data[0].length).setValues(data);
  SpreadsheetApp.flush();
}