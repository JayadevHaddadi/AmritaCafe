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

function isTimeMatching(cellVal, targetMillis, targetTimeFormat) {
  if (cellVal instanceof Date) {
    return Math.abs(cellVal.getTime() - targetMillis) < 2000;
  }
  var str = cellVal ? cellVal.toString().trim() : "";
  if (str === targetTimeFormat) return true;
  var parsed = Date.parse(str);
  if (!isNaN(parsed) && Math.abs(parsed - targetMillis) < 2000) return true;
  return false;
}

function doPost(e) {
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(20000); // Wait up to 20s for concurrent writes to finish
  } catch (err) {
    return ContentService.createTextOutput("Server busy, please retry.");
  }

  try {
    var ss = SpreadsheetApp.openById("1uUwh_9mLVUmG621v40kdGSMGblr_JyKZfpEE-xIL0vo");
    var sheet = ss.getSheetByName('Sheet1');

    // Parse the request data
    var data = JSON.parse(e.postData.getDataAsString());

    // Handle Crash Log Reporting
    if (data.action === "reportCrash") {
      var crashSheet = ss.getSheetByName("Crash Logs");
      if (!crashSheet) {
        crashSheet = ss.insertSheet("Crash Logs");
        crashSheet.appendRow(["TIMESTAMP", "TABLET", "APP_VERSION", "DEVICE_INFO", "STACK_TRACE"]);
        crashSheet.getRange(1, 1, 1, 5).setFontWeight("bold");
      }
      var tabletName = (data.tablet || "Unknown").toString();
      var appVer = (data.appVersion || "Unknown").toString();
      var devInfo = (data.deviceInfo || "").toString();
      var stack = (data.stackTrace || "").toString();
      crashSheet.appendRow([new Date(), tabletName, appVer, devInfo, stack]);
      return ContentService.createTextOutput(JSON.stringify({ status: "success", message: "Crash logged" }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    var timeMillis = data.time;
    var tablet = (data.tablet || "").toString().trim();
    var isGpay = data.isGpay || false;
    var appVersion = data.appVersion || "";

    // Format the time EXACTLY like the app does for consistency
    var timeFormat = formatAppTime(timeMillis);

    // Dynamic headers
    var headers = sheet.getRange(1, 1, 1, Math.max(1, sheet.getLastColumn())).getValues()[0];

    // Handle Retrospective GPay Update
    if (data.action === "updateGPay") {
      var lastRow = sheet.getLastRow();
      if (lastRow < 2) return ContentService.createTextOutput("Sheet is empty.");

      // Find column indices (1-based for getRange)
      var tabletColIndex = headers.indexOf("TABLET") + 1 || 2; 
      var orderColIndex = headers.indexOf("ORDER") + 1 || 3;
      var totalColIndex = headers.indexOf("TOTAL") + 1 || 7;
      var gpayColIndex = headers.indexOf("GPAY AMOUNT") + 1 || 8;

      // Efficiency: Check last 1000 rows for updates
      var searchDepth = 1000;
      var startRow = Math.max(2, lastRow - searchDepth + 1);
      var numRows = lastRow - startRow + 1;

      var maxCol = Math.max(tabletColIndex, orderColIndex, totalColIndex, gpayColIndex);
      var values = sheet.getRange(startRow, 1, numRows, maxCol).getValues(); 
      var found = false;
      var targetOrder = data.order.toString().trim();

      // Search bottom-up
      for (var i = values.length - 1; i >= 0; i--) {
        var cellTablet = values[i][tabletColIndex - 1].toString().trim();
        var cellOrder = values[i][orderColIndex - 1].toString().trim();

        if (cellOrder === targetOrder && cellTablet === tablet) {
          var total = values[i][totalColIndex - 1]; 
          var gpayAmount = isGpay ? total : 0;

          sheet.getRange(startRow + i, gpayColIndex).setValue(gpayAmount);
          found = true;
        } else if (found) {
          break;
        }
      }
      return ContentService.createTextOutput(found ? "Update successful!" : "Order not found in search range.");
    }

    // Order Entry (Single or Batch)
    var isBatch = data.action === "batchOrders" || Array.isArray(data.orders);
    var orderList = isBatch ? data.orders : [data];
    var appVersionColIndex = headers.indexOf("APP VERSION") + 1;

    // Deduplication Check: Prevent duplicate rows if exact order was already recorded
    var lastRow = sheet.getLastRow();
    var checkValues = [];
    if (lastRow >= 2) {
      // Searching the last 1000 rows takes < 100ms even on a 100k-row sheet
      var searchDepth = 1000;
      var startRow = Math.max(2, lastRow - searchDepth + 1);
      var numRows = lastRow - startRow + 1;
      checkValues = sheet.getRange(startRow, 1, numRows, 3).getValues(); // col 1: TIME, col 2: TABLET, col 3: ORDER
    }

    var rowsToAppend = [];
    var insertedCount = 0;
    var duplicateCount = 0;

    for (var o = 0; o < orderList.length; o++) {
      var itemData = orderList[o];
      var oTimeMillis = itemData.time;
      var oTablet = (itemData.tablet || "").toString().trim();
      var oOrder = (itemData.order || "").toString().trim();
      var oIsGpay = itemData.isGpay || false;
      var oAppVersion = itemData.appVersion || "";
      var oItems = itemData.items || [];
      var oTimeFormat = formatAppTime(oTimeMillis);

      var isDuplicate = false;
      for (var k = checkValues.length - 1; k >= 0; k--) {
        var rowTablet = checkValues[k][1] ? checkValues[k][1].toString().trim() : "";
        var rowOrder = checkValues[k][2] ? checkValues[k][2].toString().trim() : "";
        if (rowTablet === oTablet && rowOrder === oOrder) {
          var rowTimeVal = checkValues[k][0];
          if (isTimeMatching(rowTimeVal, oTimeMillis, oTimeFormat)) {
            isDuplicate = true;
            break;
          }
        }
      }

      if (isDuplicate) {
        duplicateCount++;
        continue;
      }

      // Add to checkValues so later orders in this same batch don't duplicate
      checkValues.push([oTimeFormat, oTablet, oOrder]);
      insertedCount++;

      for (var i = 0; i < oItems.length; i++) {
        var gpayAmount = oIsGpay ? oItems[i].total : 0;
        var rowValues = [oTimeFormat, oTablet, oOrder, oItems[i].quantity, oItems[i].name, oItems[i].cost, oItems[i].total, gpayAmount];
        
        if (appVersionColIndex > 0) {
          var maxCols = Math.max(rowValues.length, headers.length);
          var fullRow = [];
          for (var j = 0; j < maxCols; j++) {
            if (j === appVersionColIndex - 1) {
              fullRow.push(oAppVersion);
            } else if (j < rowValues.length) {
              fullRow.push(rowValues[j]);
            } else {
              fullRow.push("");
            }
          }
          rowsToAppend.push(fullRow);
        } else {
          rowsToAppend.push(rowValues);
        }
      }
    }

    if (rowsToAppend.length > 0) {
      sheet.getRange(lastRow + 1, 1, rowsToAppend.length, rowsToAppend[0].length).setValues(rowsToAppend);
    }
    
    if (isBatch) {
      return ContentService.createTextOutput("Batch processed: " + insertedCount + " inserted, " + duplicateCount + " duplicate(s) skipped.");
    } else {
      if (duplicateCount > 0 && insertedCount === 0) {
        return ContentService.createTextOutput("Order already exists. Skipped duplicate insertion.");
      }
      return ContentService.createTextOutput("Order inserted successfully!");
    }
  } finally {
    lock.releaseLock();
  }
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